package play.api.cache.redis.connector

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.Logger
import play.api.cache.redis.exception._
import play.api.cache.redis.{Configuration, RedisConnector}

import akka.actor.ActorSystem
import akka.util.ByteString
import brando.{Ok, Pong}


/**
  * The connector directly connects with the REDIS instance, implements protocol commands
  * and is supposed to by used internally by another wrappers. The connector does not
  * directly implement [[play.api.cache.redis.CacheApi]] but provides fundamental functionality.
  *
  * @param redis         communication module to Redis cache
  * @param serializer    encodes/decodes objects into/from a string
  * @param configuration connection settings
  * @author Karel Cemus
  */
@Singleton
private [ connector ] class RedisConnectorImpl @Inject( )( redis: RedisActor, serializer: AkkaSerializer, configuration: Configuration )( implicit system: ActorSystem ) extends RedisConnector {

  // implicit ask timeout
  implicit val timeout = akka.util.Timeout( configuration.timeout )

  /** implicit execution context */
  implicit val context = system.dispatchers.lookup( configuration.invocationContext )

  /** logger instance */
  protected val log = Logger( "play.api.cache.redis" )

  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] = redis ? ( "GET", key ) expects {
    case Some( response: ByteString ) =>
      log.trace( s"Hit on key '$key'." )
      Some( decode[ T ]( key, response.utf8String ) )
    case None =>
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
    redis ? ( "SETEX", key, expiration.toSeconds.toString, value ) expects {
      case Some( Ok ) => log.debug( s"Set on key '$key' on $expiration seconds." )
      case None => log.warn( s"Set on key '$key' failed." )
    }

  /** eternally stores already encoded value into the storage */
  private def setEternally( key: String, value: String ): Future[ Unit ] =
    redis ? ( "SET", key, value ) expects {
      case Some( Ok ) => log.debug( s"Set on key '$key' for infinite seconds." )
      case None => log.warn( s"Set on key '$key' failed." )
    }

  def setIfNotExists( key: String, value: Any ): Future[ Boolean ] =
    redis ? ( "SETNX", key,  encode( key, value ) ) expects {
      case Some( 0 ) => log.debug( s"Set if not exists on key '$key' ignored. Value already exists." ); false
      case Some( 1 ) => log.debug( s"Set if not exists on key '$key' succeeded." ); true
      case None => log.warn( s"Set if not exists on key '$key' failed." ); false
    }

  def expire( key: String, expiration: Duration ): Future[ Unit ] =
    redis ? ( "EXPIRE", key, expiration.toSeconds.toString ) expects {
      case Some( 1 ) => log.debug( s"Expiration set on key '$key'." ) // expiration was set
      case Some( 0 ) => log.debug( s"Expiration set on key '$key' failed. Key does not exist." ) // Nothing was removed
    }

  def matching( pattern: String ): Future[ Set[ String ] ] = redis ? ( "KEYS", pattern ) expects {
    case Some( response: List[ _ ] ) =>
      val keys = response.asInstanceOf[ List[ Option[ ByteString ] ] ].flatten.map( _.utf8String ).toSet
      log.debug( s"KEYS on '$pattern' responded '${ keys.mkString( ", " ) }'." )
      keys
  }

  def invalidate( ): Future[ Unit ] = redis ! "FLUSHDB" expects {
    case Some( Ok ) => log.info( "Invalidated." ) // cache was invalidated
    case None => log.warn( "Invalidation failed." ) // execution failed
  }

  def exists( key: String ): Future[ Boolean ] = redis ? ( "EXISTS", key ) expects {
    case Some( 1L ) => log.debug( s"Key '$key' exists." ); true
    case Some( 0L ) => log.debug( s"Key '$key' doesn't exist." ); false
  }

  def remove( keys: String* ): Future[ Unit ] =
    if ( keys.nonEmpty ) // if any key to remove do it
      redis ! ( "DEL", keys: _* ) expects {
        // Nothing was removed
        case Some( 0 ) => log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } succeeded but nothing was removed." )
        // Some entries were removed
        case Some( number ) => log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } removed $number values." )
      }
    else Future( Unit ) // otherwise return immediately

  def ping( ): Future[ Unit ] = redis ! "PING" expects {
    case Pong => Unit
  }
}
