package play.cache.redis

import java.util.concurrent.TimeUnit

import scala.concurrent._
import scala.language.implicitConversions
import scala.util._

import play.api._
import play.api.libs.concurrent.Akka
import play.cache.api.CacheAPI

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.ByteString
import brando._

/**
 * <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p>
 */
class RedisCache( implicit app: Application ) extends CacheAPI {

  protected val log = Logger( "play.redis" )

  protected def config = com.typesafe.config.ConfigFactory.load( ).getConfig( "play.redis" )

  /** default invocation context of all cache commands */
  protected implicit val context: ExecutionContext = Akka.system.dispatchers.lookup( config.getString( "dispatcher" ) )

  /** timeout of cache requests */
  private implicit val timeout = akka.util.Timeout( config.getInt( "timeout" ), TimeUnit.MILLISECONDS )

  /** communication module to Redis cache */
  private var redis: RedisRef = null

  /** Retrieve a value from the cache. */
  def get( key: String ): Future[ Option[ String ] ] =
    redis ? Request( "GET", key ) map {
      case Success( Some( response: ByteString ) ) =>
        log.trace( s"Hit on key '$key'." )
        Some( response.utf8String )
      case Success( None ) =>
        log.debug( s"Miss on key '$key'." )
        None
      case Failure( ex ) =>
        log.error( s"GET command failed for key '$key'.", ex )
        None
      case _ =>
        log.error( s"Unrecognized answer from GET command for key '$key'." )
        None
    }

  /** Determines whether exists the value with given key */
  def exists( key: String ): Future[ Boolean ] =
    redis ? Request( "EXISTS", key ) map {
      case Success( Some( 1L ) ) =>
        log.trace( s"Key '$key' exists." )
        true
      case Success( Some( 0L ) ) =>
        log.trace( s"Key '$key' doesn't exist." )
        false
      case Failure( ex ) =>
        log.error( s"EXISTS command failed for key '$key'.", ex )
        false
      case _ =>
        log.error( s"Unrecognized answer from EXISTS command for key '$key'." )
        false
    }

  /** Set a value into the cache. */
  def set( key: String, value: String, expiration: Int ): Future[ Try[ String ] ] =
    redis ? Request( "SETEX", key, expiration.toString, value ) map {
      case Success( Some( Ok ) ) =>
        log.debug( s"Set on key '$key' on $expiration seconds." )
        Success( "OK" )
      case Success( None ) =>
        log.warn( s"Set on key '$key' failed." )
        Failure( new IllegalStateException( "SETEX command failed." ) )
      case Failure( ex ) =>
        log.error( s"SETEX command failed for key '$key'.", ex )
        Failure( new IllegalStateException( "SETEX command failed.", ex ) )
      case _ =>
        log.error( s"Unrecognized answer from SETEX command for key '$key'." )
        Failure( new IllegalStateException( "SETEX command failed." ) )
    }

  /** Remove all values from the cache */
  def remove( keys: String* ): Future[ Try[ String ] ] =
    redis ? Request( "DEL", keys: _* ) map {
      case Success( Some( 0 ) ) => // Nothing was removed
        log.debug( s"Remove on key '$keys' succeeded but nothing was removed." )
        Success( "OK" )
      case Success( Some( number ) ) => // Some entries were removed
        log.debug( s"Remove on key '$keys' removed $number values." )
        Success( "OK" )
      case Failure( ex ) =>
        log.error( s"DEL command failed for key '$keys'.", ex )
        Failure( new IllegalStateException( "DEL command failed.", ex ) )
      case _ =>
        log.error( s"Unrecognized answer from DEL command for key '$keys'." )
        Failure( new IllegalStateException( "DEL command failed." ) )
    }

  /** Remove all keys in cache */
  def invalidate( ): Future[ Try[ String ] ] =
    redis ? Request( "FLUSHDB" ) map {
      case Success( Some( Ok ) ) => // cache was invalidated
        log.info( "Invalidated." )
        Success( "OK" )
      case Success( None ) => // execution failed
        log.warn( "Invalidation failed." )
        Failure( new IllegalStateException( "Invalidation failed." ) )
      case Failure( ex ) =>
        log.error( s"Invalidation failed with an exception.", ex )
        Failure( new IllegalStateException( "Invalidation failed.", ex ) )
      case _ =>
        log.error( s"Unrecognized answer from invalidation command." )
        Failure( new IllegalStateException( "Invalidated failed." ) )
    }

  def start( ) = {
    val host = config.getString( "host" )
    val port = config.getInt( "port" )
    val database = config.getInt( "database" )
    // create new brando actor
    redis = Akka.system.actorOf( Brando( host, port, Some( database ) ) )
    log.info( s"Started Redis cache actor. Actor is connected to $host:$port?database=$database" )
  }

  def stop( ) = {
    // stop running brando actor
    if ( redis != null ) Akka.system.stop( redis.actor.actorRef )
    log.info( "Stopped Redis cache actor." )
  }

  def expire( key: String, expiration: Int ) = {
    redis ? Request( "EXPIRE", key, expiration.toString ) map {
      case Success( Some( 1 ) ) => // expiration was set
        log.debug( s"Expiration set on key '$key'." )
        Success( "OK" )
      case Success( Some( 0 ) ) => // Nothing was removed
        log.debug( s"Expiration set on key '$key' failed. Key does not exist." )
        Success( "OK" )
      case Failure( ex ) =>
        log.error( s"EXPIRE command failed for key '$key'.", ex )
        Failure( new IllegalStateException( "EXPIRE command failed.", ex ) )
      case _ =>
        log.error( s"Unrecognized answer from EXPIRE command for key '$key'." )
        Failure( new IllegalStateException( "EXPIRE command failed." ) )
    }
  }

  override def matching( pattern: String ): Future[ Set[ String ] ] = redis ? Request( "KEYS", pattern ) map {
    case Success( Some( response: List[ _ ] ) ) =>
      val keys = response.asInstanceOf[ List[ Option[ ByteString ] ] ].flatten.map( _.utf8String ).toSet
      log.debug( s"KEYS on '$pattern' responded '${ keys.mkString( ", " ) }'." )
      keys
    case Failure( ex ) => log.error( s"KEYS command failed for pattern '$pattern'.", ex ); Set.empty[ String ]
    case _ => log.error( s"Unrecognized answer from KEYS command for pattern '$pattern'." ); Set.empty[ String ]
  }

  override def removeMatching( pattern: String ): Future[ Unit ] =
    matching( pattern ) flatMap ( keys => if ( keys.isEmpty ) Future { } else remove( keys.toSeq: _* ).map { case _ => } )

  private implicit class RedisRef( brando: ActorRef ) {

    private[ RedisCache ] val actor = new AskableActorRef( brando )

    def ?( request: Request ): Future[ Any ] =
      actor ask request map Success.apply recover {
        case ex => Failure( ex ) // execution failed, recover
      }
  }

}