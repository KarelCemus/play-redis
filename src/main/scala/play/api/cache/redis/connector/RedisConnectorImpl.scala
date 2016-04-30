package play.api.cache.redis.connector

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

import play.api.Logger
import play.api.cache.redis._
import play.api.cache.redis.exception._
import play.api.inject.ApplicationLifecycle

import akka.util.ByteString
import brando.{Ok, Pong, Request}


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

  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] = redis ?? ( "GET", key ) expects {
    case Success( Some( response: ByteString ) ) =>
      log.trace( s"Hit on key '$key'." )
      Some( decode[ T ]( key, response.utf8String ) )
    case Success( None ) =>
      log.debug( s"Miss on key '$key'." )
      None
  }

  /** decodes the object, reports an exception if fails */
  private def decode[ T: ClassTag ]( key: String, encoded: String ): T = serializer.decode[ T ]( encoded ).recover {
    case ex => serializationFailed( key, "Deserialization failed", ex )
  }.get

  def set( key: String, value: Any, expiration: Duration ): Future[ Unit ] =
    // no value to set
    if ( value == null ) remove( key )
    // set for finite duration
    else if ( expiration.isFinite( ) ) setTemporally( key, encode( key, value ), expiration )
    // set for infinite duration
    else setEternally( key, encode( key, value ) )

  /** encodes the object, reports an exception if fails */
  private def encode( key: String, value: Any ): String = serializer.encode( value ).recover {
    case ex => serializationFailed( key, "Serialization failed", ex )
  }.get

  /** temporally stores already encoded value into the storage */
  private def setTemporally( key: String, value: String, expiration: Duration ): Future[ Unit ] =
    redis ?? ( "SETEX", key, expiration.toSeconds.toString, value ) expects {
      case Success( Some( Ok ) ) => log.debug( s"Set on key '$key' on $expiration seconds." )
      case Success( None ) => log.warn( s"Set on key '$key' failed." )
    }

  /** eternally stores already encoded value into the storage */
  private def setEternally( key: String, value: String ): Future[ Unit ] =
    redis ?? ( "SET", key, value ) expects {
      case Success( Some( Ok ) ) => log.debug( s"Set on key '$key' for infinite seconds." )
      case Success( None ) => log.warn( s"Set on key '$key' failed." )
    }

  def expire( key: String, expiration: Duration ): Future[ Unit ] =
    redis ?? ( "EXPIRE", key, expiration.toSeconds.toString ) expects {
      case Success( Some( 1 ) ) => log.debug( s"Expiration set on key '$key'." ) // expiration was set
      case Success( Some( 0 ) ) => log.debug( s"Expiration set on key '$key' failed. Key does not exist." ) // Nothing was removed
    }

  def matching( pattern: String ): Future[ Set[ String ] ] = redis ?? ( "KEYS", pattern ) expects {
    case Success( Some( response: List[ _ ] ) ) =>
      val keys = response.asInstanceOf[ List[ Option[ ByteString ] ] ].flatten.map( _.utf8String ).toSet
      log.debug( s"KEYS on '$pattern' responded '${ keys.mkString( ", " ) }'." )
      keys
  }

  def invalidate( ): Future[ Unit ] = redis !! "FLUSHDB" expects {
    case Success( Some( Ok ) ) => log.info( "Invalidated." ) // cache was invalidated
    case Success( None ) => log.warn( "Invalidation failed." ) // execution failed
  }

  def exists( key: String ): Future[ Boolean ] = redis ?? ( "EXISTS", key ) expects {
    case Success( Some( 1L ) ) => log.debug( s"Key '$key' exists." ); true
    case Success( Some( 0L ) ) => log.debug( s"Key '$key' doesn't exist." ); false
  }

  def remove( keys: String* ): Future[ Unit ] =
    if ( keys.nonEmpty ) // if any key to remove do it
      redis !! ( "DEL", keys: _* ) expects {
        case Success( Some( 0 ) ) => // Nothing was removed
          log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } succeeded but nothing was removed." )
        case Success( Some( number ) ) => // Some entries were removed
          log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } removed $number values." )
      }
    else Future( Unit ) // otherwise return immediately

  def ping( ): Future[ Unit ] = redis !! "PING" expects {
    case Success( Pong ) => Unit
  }

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


  /** enriches the actor to return expected future. The extended future implements advanced response handling */
  private implicit class RichRedisActor( redis: RedisActor ) {

    def ??[ T ]( command: String, key: String, params: String* ): ExpectedFuture[ T ] =
      new ExpectedFuture[ T ]( redis ? Request( command, key +: params: _* ), s"$command ${ key +: params.headOption.toList mkString " " }" )

    def !!( command: String, params: String* ): ExpectedFuture[ Unit ] =
      new ExpectedFuture[ Unit ]( redis ? Request( command, params: _* ), s"${ command +: params.headOption.toList mkString " " }" )
  }

  /** The extended future implements advanced response handling. It unifies maintenance of unexpected responses */
  private class ExpectedFuture[ T ]( future: Future[ Any ], cmd: => String ) {

    /** received an unexpected response */
    private def onUnexpected: PartialFunction[ Any, T ] = {
      case Success( _ ) => unexpected( "???", cmd ) // TODO provide the key
      case Failure( ex ) => failed( "???", cmd, ex ) // TODO provide the key
      case _ => unexpected( "???", cmd ) // TODO provide the key
    }

    /** execution failed with an exception */
    private def onException: PartialFunction[ Throwable, T ] = {
      case ex => failed( "???", cmd, ex ) // TODO provide the key
    }

    /** handles both expected and unexpected responses and recovers from them */
    def expects( expected: PartialFunction[ Any, T ] ): Future[ T ] =
      future map ( expected orElse onUnexpected ) recover onException
  }

  // start up the connector
  start( )

  // bind shutdown
  lifecycle.addStopHook( stop _ )
}
