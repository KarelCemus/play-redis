package play.api.cache.redis

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

import play.api.Logger
import play.api.inject.ApplicationLifecycle

import akka.util.ByteString
import brando.{Ok, Request}

/**
  * The connector directly connects with the REDIS instance, implements protocol commands
  * and is supposed to by used internally by another wrappers. The connector does not
  * directly implement [[CacheApi]] but provides fundamental functionality.
  *
  * @param redis      communication module to Redis cache
  * @param serializer encodes/decodes objects into/from a string
  * @param lifecycle  application lifecycle enables stop hook
  * @param settings   connection settings
  *
  * @author Karel Cemus
  */
@Singleton
class RedisConnectorImpl @Inject( )( redis: RedisActor, serializer: AkkaSerializer, lifecycle: ApplicationLifecycle, settings: ConnectionSettings ) extends RedisConnector {

  // implicit execution context and ask timeout
  import settings.{invocationContext, timeout}

  /** logger instance */
  protected val log = Logger( "play.api.cache.redis" )

  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] = redis ? Request( "GET", key ) map {
    case Success( Some( response: ByteString ) ) => log.trace( s"Hit on key '$key'." ); decode[ T ]( key, response.utf8String )
    case Success( None ) => log.debug( s"Miss on key '$key'." ); None
    case Failure( ex ) => log.error( s"GET command failed for key '$key'.", ex ); None
    case _ => log.error( s"Unrecognized answer from GET command for key '$key'." ); None
  }

  /** decodes the object, reports an exception if fails */
  private def decode[ T: ClassTag ]( key: String, encoded: String ): Option[ T ] = serializer.decode[ T ]( encoded ).recoverWith {
    case ex => log.error( s"Deserialization for key '$key' failed. Cause: '$ex'." ); Failure( ex )
  }.toOption

  def set( key: String, value: Any, expiration: Duration ): Future[ Unit ] = if ( value == null ) remove( key )
  else (expiration, encode( key, value )) match {
    case (Duration.Inf, Success( encoded: String )) => setEternally( key, encoded )
    case (temporal: Duration, Success( encoded: String )) => setTemporally( key, encoded, temporal )
    case (_, Failure( ex )) => log.error( s"SET command failed. Encoding of the value for the key '$key' failed.", ex ); Future( Unit )
  }

  /** encodes the object, reports an exception if fails */
  private def encode( key: String, value: Any ) = serializer.encode( value ) recoverWith {
    case ex => log.error( s"Serialization for key '$key' failed. Cause: '$ex'." ); Failure( ex )
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

  def expire( key: String, expiration: Duration ): Future[ Unit ] =
    redis ? Request( "EXPIRE", key, expiration.toSeconds.toString ) map {
      case Success( Some( 1 ) ) => log.debug( s"Expiration set on key '$key'." ) // expiration was set
      case Success( Some( 0 ) ) => log.debug( s"Expiration set on key '$key' failed. Key does not exist." ) // Nothing was removed
      case Failure( ex ) => log.error( s"EXPIRE command failed for key '$key'.", ex )
      case _ => log.error( s"Unrecognized answer from EXPIRE command for key '$key'." )
    }

  def matching( pattern: String ): Future[ Set[ String ] ] = redis ? Request( "KEYS", pattern ) map {
    case Success( Some( response: List[ _ ] ) ) =>
      val keys = response.asInstanceOf[ List[ Option[ ByteString ] ] ].flatten.map( _.utf8String ).toSet
      log.debug( s"KEYS on '$pattern' responded '${ keys.mkString( ", " ) }'." )
      keys
    case Failure( ex ) => log.error( s"KEYS command failed for pattern '$pattern'.", ex ); Set.empty[ String ]
    case _ => log.error( s"Unrecognized answer from KEYS command for pattern '$pattern'." ); Set.empty[ String ]
  }

  def invalidate( ): Future[ Unit ] = redis ? Request( "FLUSHDB" ) map {
    case Success( Some( Ok ) ) => log.info( "Invalidated." ) // cache was invalidated
    case Success( None ) => log.warn( "Invalidation failed." ) // execution failed
    case Failure( ex ) => log.error( s"Invalidation failed with an exception.", ex )
    case _ => log.error( s"Unrecognized answer from invalidation command." )
  }

  def exists( key: String ): Future[ Boolean ] = redis ? Request( "EXISTS", key ) map {
    case Success( Some( 1L ) ) => log.trace( s"Key '$key' exists." ); true
    case Success( Some( 0L ) ) => log.trace( s"Key '$key' doesn't exist." ); false
    case Failure( ex ) => log.error( s"EXISTS command failed for key '$key'.", ex ); false
    case _ => log.error( s"Unrecognized answer from EXISTS command for key '$key'." ); false
  }

  def remove( keys: String* ): Future[ Unit ] =
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
    else Future( Unit ) // otherwise return immediately

  def ping( ): Future[ Unit ] = redis ? Request( "PING" ) map ( _ => Unit )

  /** start up the connector and test it with ping */
  def start( ): Future[ Unit ] = ping( ) map {
    import settings._
    _ => log.info( s"Redis cache started. Actor is connected to $host:$port?database=$database" )
  }

  /** stops running brando actor */
  def stop( ): Future[ Unit ] = Future {
    redis.stop( )
    log.info( "Redis cache stopped." )
  }

  // start up the connector
  start( )

  // bind shutdown
  lifecycle.addStopHook( stop _ )
}
