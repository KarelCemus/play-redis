package play.api.cache.redis

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag
import scala.util._

import play.api._
import play.api.inject.ApplicationLifecycle

import akka.actor.ActorSystem
import akka.util.ByteString
import brando._

/** <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p> */
class RedisCache[ Result[ _ ] ](

  implicit builder: Builders.ResultBuilder[ Result ],
  protected val application: Application,
  lifecycle: ApplicationLifecycle,
  protected val configuration: Configuration,
  protected val system: ActorSystem

) extends InternalCacheApi[ Result ] with Implicits with Config with AkkaSerializer {

  import builder._

  /** logger instance */
  protected val log = Logger( "play.api.cache.redis" )

  /** default invocation context of all cache commands */
  protected implicit val context: ExecutionContext = system.dispatchers.lookup( invocationContext )

  /** communication module to Redis cache */
  protected val redis: RedisRef = system actorOf StashingRedis {
    system actorOf Redis( host, port, database = database, auth = password )
  }

  override def get[ T: ClassTag ]( key: String ) = redis ? Request( "GET", key ) map {
    case Success( Some( response: ByteString ) ) => log.trace( s"Hit on key '$key'." ); decode[ T ]( key, response.utf8String ).toOption
    case Success( None ) => log.debug( s"Miss on key '$key'." ); None
    case Failure( ex ) => log.error( s"GET command failed for key '$key'.", ex ); None
    case _ => log.error( s"Unrecognized answer from GET command for key '$key'." ); None
  }

  override def set( key: String, value: Any, expiration: Duration ) = if ( value == null ) removeAll( key )
  else (expiration, encode( key, value )) match {
    case (Duration.Inf, Success( encoded: String )) => setEternally( key, encoded )
    case (temporal: Duration, Success( encoded: String )) => setTemporally( key, encoded, temporal )
    case (_, Failure( ex )) => log.error( s"SET command failed. Encoding of the value for the key '$key' failed.", ex ).toFuture
  }

  /** temporally stores already encoded value into the storage */
  private def setTemporally( key: String, value: String, expiration: Duration ): Future[ Unit ] =
    redis ? Request( "SETEX", key, expiration.toSeconds.toString, value ) map {
      case Success( Some( Ok ) ) => log.debug( s"Set on key '$key' on $expiration seconds." )
      case Success( None ) => log.warn( s"Set on key '$key' failed." )
      case Failure( ex ) => log.error( s"SETEX command failed for key '$key'.", ex )
      case _ => log.error( s"Unrecognized answer from SETEX command for key '$key'." )
    }

  /** eternally stores already encoded value into the storage */
  private def setEternally( key: String, value: String ): Future[ Unit ] =
    redis ? Request( "SET", key, value ) map {
      case Success( Some( Ok ) ) => log.debug( s"Set on key '$key' for infinite seconds." )
      case Success( None ) => log.warn( s"Set on key '$key' failed." )
      case Failure( ex ) => log.error( s"SET command failed for key '$key'.", ex )
      case _ => log.error( s"Unrecognized answer from SET command for key '$key'." )
    }

  override def expire( key: String, expiration: Duration ) =
    redis ? Request( "EXPIRE", key, expiration.toSeconds.toString ) map {
      case Success( Some( 1 ) ) => log.debug( s"Expiration set on key '$key'." ) // expiration was set
      case Success( Some( 0 ) ) => log.debug( s"Expiration set on key '$key' failed. Key does not exist." ) // Nothing was removed
      case Failure( ex ) => log.error( s"EXPIRE command failed for key '$key'.", ex )
      case _ => log.error( s"Unrecognized answer from EXPIRE command for key '$key'." )
    }

  override def matching( pattern: String ): Result[ Set[ String ] ] = redis ? Request( "KEYS", pattern ) map {
    case Success( Some( response: List[ _ ] ) ) =>
      val keys = response.asInstanceOf[ List[ Option[ ByteString ] ] ].flatten.map( _.utf8String ).toSet
      log.debug( s"KEYS on '$pattern' responded '${ keys.mkString( ", " ) }'." )
      keys
    case Failure( ex ) => log.error( s"KEYS command failed for pattern '$pattern'.", ex ); Set.empty[ String ]
    case _ => log.error( s"Unrecognized answer from KEYS command for pattern '$pattern'." ); Set.empty[ String ]
  }

  override def getOrElse[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => T ) =
    getOrFuture( key, expiration )( orElse.toFuture )

  override def getOrFuture[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => Future[ T ] ): Future[ T ] = get[ T ]( key ) andFuture {
    // cache hit, return the unwrapped value
    case Some( value: T ) => value.toFuture
    // cache miss, compute the value, store it into cache and return the value
    case None => orElse flatMap ( value => set( key, value, expiration ) andFuture ( _ => Future( value ) ) )
  }

  override def remove( key: String ) =
    removeAll( key )

  override def remove( key1: String, key2: String, keys: String* ) =
    removeAll( key1 +: key2 +: keys: _* )

  override def removeAll( keys: String* ): Result[ Unit ] =
    if ( keys.nonEmpty ) // if any key to remove do it
      redis ? Request( "DEL", keys: _* ) map {
        case Success( Some( 0 ) ) => // Nothing was removed
          log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } succeeded but nothing was removed." )
        case Success( Some( number ) ) => // Some entries were removed
          log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } removed $number values." )
        case Failure( ex ) =>
          log.error( s"DEL command failed for keys ${ keys.mkString( "'", ",", "'" ) }.", ex )
        case _ =>
          log.error( s"Unrecognized answer from DEL command for keys ${ keys.mkString( "'", ",", "'" ) }." )
      }
    else builder toResult Future( Unit ) // otherwise return immediately

  override def removeMatching( pattern: String ): Result[ Unit ] =
    matching( pattern ) andThen ( keys => removeAll( keys.toSeq: _* ) )

  override def invalidate( ) = redis ? Request( "FLUSHDB" ) map {
    case Success( Some( Ok ) ) => log.info( "Invalidated." ) // cache was invalidated
    case Success( None ) => log.warn( "Invalidation failed." ) // execution failed
    case Failure( ex ) => log.error( s"Invalidation failed with an exception.", ex )
    case _ => log.error( s"Unrecognized answer from invalidation command." )
  }

  override def exists( key: String ) = redis ? Request( "EXISTS", key ) map {
    case Success( Some( 1L ) ) => log.trace( s"Key '$key' exists." ); true
    case Success( Some( 0L ) ) => log.trace( s"Key '$key' doesn't exist." ); false
    case Failure( ex ) => log.error( s"EXISTS command failed for key '$key'.", ex ); false
    case _ => log.error( s"Unrecognized answer from EXISTS command for key '$key'." ); false
  }

  def start( ) = redis ? Request( "PING" ) map { _ =>
    log.info( s"Redis cache started. Actor is connected to $host:$port?database=$database" )
  }

  /** stops running brando actor */
  def stop( ) = Future {
    system.stop( redis.actor.actorRef )
    log.info( "Redis cache stopped." )
  }

  // start up the connector
  start( )

  // bind shutdown
  lifecycle.addStopHook( stop _ )
}
