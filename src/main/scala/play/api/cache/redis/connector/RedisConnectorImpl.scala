package play.api.cache.redis.connector

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.Logger
import play.api.cache.redis.exception._
import play.api.cache.redis.{Configuration, RedisConnector}
import play.api.inject.ApplicationLifecycle

import akka.actor.ActorSystem
import scredis.Client


/**
  * The connector directly connects with the REDIS instance, implements protocol commands
  * and is supposed to by used internally by another wrappers. The connector does not
  * directly implement [[play.api.cache.redis.CacheApi]] but provides fundamental functionality.
  *
  * @param serializer    encodes/decodes objects into/from a string
  * @param configuration connection settings
  * @param lifecycle     application lifecycle
  * @author Karel Cemus
  */
@Singleton
private[ connector ] class RedisConnectorImpl @Inject( )( serializer: AkkaSerializer, configuration: Configuration, lifecycle: ApplicationLifecycle )( implicit system: ActorSystem ) extends RedisConnector {

  // implicit ask timeout
  implicit val timeout = akka.util.Timeout( configuration.timeout )

  /** implicit execution context */
  implicit val context = system.dispatchers.lookup( configuration.invocationContext )

  /** logger instance */
  protected val log = Logger( "play.api.cache.redis" )

  private val redis = Client(
    host = configuration.host,
    port = configuration.port,
    database = configuration.database,
    passwordOpt = configuration.password
  )

  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] =
    redis.get[ String ]( key ) executing "GET" withParameter key expects {
      case Some( response: String ) =>
        log.trace( s"Hit on key '$key'." )
        Some( decode[ T ]( key, response ) )
      case None =>
        log.debug( s"Miss on key '$key'." )
        None
    }

  /** decodes the object, reports an exception if fails */
  private def decode[ T: ClassTag ]( key: String, encoded: String ): T =
    serializer.decode[ T ]( encoded ).recover {
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
  private def encode( key: String, value: Any ): String =
    serializer.encode( value ).recover {
      case ex => serializationFailed( key, "Serialization failed", ex )
    }.get

  /** temporally stores already encoded value into the storage */
  private def setTemporally( key: String, value: String, expiration: Duration ): Future[ Unit ] =
    redis.setEX( key, value, expiration.toSeconds.toInt ) executing "SETEX" withParameters s"$key $value $expiration" expects {
      case _ => log.debug( s"Set on key '$key' on $expiration seconds." )
    }

  /** eternally stores already encoded value into the storage */
  private def setEternally( key: String, value: String ): Future[ Unit ] =
    redis.set( key, value ) executing "SET" withParameters s"$key $value" expects {
      case true => log.debug( s"Set on key '$key' for infinite seconds." )
      case false => log.warn( s"Set on key '$key' failed. Condition was not met." )
    }

  def setIfNotExists( key: String, value: Any ): Future[ Boolean ] =
    redis.setNX( key, encode( key, value ) ) executing "SETNX" withParameters s"$key ${ encode( key, value ) }" expects {
      case false => log.debug( s"Set if not exists on key '$key' ignored. Value already exists." ); false
      case true => log.debug( s"Set if not exists on key '$key' succeeded." ); true
    }

  def expire( key: String, expiration: Duration ): Future[ Unit ] =
    redis.expire( key, expiration.toSeconds.toInt ) executing "EXPIRE" withParameters s"$key, $expiration" expects {
      case true => log.debug( s"Expiration set on key '$key'." ) // expiration was set
      case false => log.debug( s"Expiration set on key '$key' failed. Key does not exist." ) // Nothing was removed
    }

  def matching( pattern: String ): Future[ Set[ String ] ] =
    redis.keys( pattern ) executing "KEYS" withParameter pattern expects {
      case keys =>
        log.debug( s"KEYS on '$pattern' responded '${ keys.mkString( ", " ) }'." )
        keys
    }

  def invalidate( ): Future[ Unit ] =
    redis.flushDB( ) executing "FLUSHDB" expects {
      case _ => log.info( "Invalidated." ) // cache was invalidated
    }

  def exists( key: String ): Future[ Boolean ] =
    redis.exists( key ) executing "EXISTS" withParameter key expects {
      case true => log.debug( s"Key '$key' exists." ); true
      case false => log.debug( s"Key '$key' doesn't exist." ); false
    }

  def remove( keys: String* ): Future[ Unit ] =
    if ( keys.nonEmpty ) // if any key to remove do it
      redis.del( keys: _* ) executing "DEL" withParameters keys.mkString( " " ) expects {
        // Nothing was removed
        case 0L => log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } succeeded but nothing was removed." )
        // Some entries were removed
        case removed => log.debug( s"Remove on keys ${ keys.mkString( "'", ",", "'" ) } removed $removed values." )
      }
    else Future( Unit ) // otherwise return immediately

  def ping( ): Future[ Unit ] =
    redis.ping( ) executing "PING" expects {
      case "PONG" => Unit
    }

  def increment( key: String, by: Long ): Future[ Long ] =
    redis.incrBy( key, by ) executing "INCRBY" withParameters s"$key, $by" expects {
      case value => log.debug( s"The value at key '$key' was incremented by $by to $value." ); value
    }

  def append( key: String, value: String ): Future[ Long ] =
    redis.append( key, value ) executing "APPEND" withParameters s"$key $value" expects {
      case length => log.debug( s"The value was appended to key '$key'." ); length
    }

  def start( ) = {
    import configuration.{host, port, database}
    log.info( s"Redis cache actor started. It is connected to $host:$port?database=$database" )
  }

  /** stops the actor */
  def stop( ): Future[ Unit ] = {
    log.info( "Stopping the redis cache actor ..." )
    redis.quit( ).map[ Unit ] { _ => log.info( "Redis cache stopped." ) }
  }

  // start the connector
  start( )
  // listen on system stop
  lifecycle.addStopHook( stop _ )
}
