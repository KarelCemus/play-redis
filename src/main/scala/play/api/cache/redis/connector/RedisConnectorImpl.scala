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
import scredis._


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
private[ connector ] class RedisConnectorImpl @Inject()( serializer: AkkaSerializer, configuration: Configuration, lifecycle: ApplicationLifecycle )( implicit system: ActorSystem ) extends RedisConnector {

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
    else if ( expiration.isFinite() ) setTemporally( key, encode( key, value ), expiration )
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
    redis.flushDB() executing "FLUSHDB" expects {
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
    redis.ping() executing "PING" expects {
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

  def listPrepend( key: String, values: Any* ): Future[ Long ] =
    redis.lPush( key, values.map( encode( key, _ ) ): _* ) executing "LPUSH" withParameters s"$key ${ values mkString " " }" expects {
      case length => log.debug( s"The $length values was prepended to key '$key'." ); length
    } recover {
      case ExecutionFailedException( _, _, ex ) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn( s"Value at '$key' is not a list." )
        throw new IllegalArgumentException( s"Value at '$key' is not a list." )
    }

  def listAppend( key: String, values: Any* ) =
    redis.rPush( key, values.map( encode( key, _ ) ): _* ) executing "RPUSH" withParameters s"$key ${ values mkString " " }" expects {
      case length => log.debug( s"The $length values was appended to key '$key'." ); length
    }

  def listSize( key: String ) =
    redis.lLen( key ) executing "LLEN" withParameter key expects {
      case length => log.debug( s"The collection at '$key' has $length items." ); length
    }

  def listSetAt( key: String, position: Int, value: Any ) =
    redis.lSet( key, position, encode( key, value ) ) executing "LSET" withParameters s"$key $value" expects {
      case _ => log.debug( s"Updated value at $position in '$key' to $value." )
    } recover {
      case ExecutionFailedException( _, _, exceptions.RedisErrorResponseException( "ERR index out of range" ) ) =>
        log.debug( s"Update of the value at $position in '$key' failed due to index out of range." )
        throw new IndexOutOfBoundsException( "Index out of range" )
    }

  def listHeadPop[ T: ClassTag ]( key: String ) =
    redis.lPop[ String ]( key ) executing "LPOP" withParameter key expects {
      case Some( encoded ) =>
        log.trace( s"Hit on head in key '$key'." )
        Some( decode[ T ]( key, encoded ) )
      case None =>
        log.trace( s"Miss on head in key '$key'." )
        None
    }

  def listSlice[ T: ClassTag ]( key: String, start: Int, end: Int ) =
    redis.lRange[ String ]( key, start, end ) executing "LRANGE" withParameters s"$start $end" expects {
      case values =>
        log.debug( s"The range on '$key' from $start to $end included returned ${ values.size } values." )
        values.map( decode[ T ]( key, _ ) )
    }

  def listRemove( key: String, value: Any, count: Int ) =
    redis.lRem( key, encode( key, value ), count ) executing "LREM" withParameter s"$key $value $count" expects {
      case removed => log.debug( s"Removed $removed occurrences of $value in '$key'." ); removed
    }

  def listTrim( key: String, start: Int, end: Int ) =
    redis.lTrim( key, start, end ) executing "LTRIM" withParameter s"$key $start $end" expects {
      case _ => log.debug( s"Trimmed collection at '$key' to $start:$end " )
    }

  def listInsert( key: String, pivot: Any, value: Any ) =
    redis.lInsert( key, Position.Before, encode( key, pivot ), encode( key, value ) ) executing "LINSERT" withParameter s"$key $pivot $value" expects {
      case None | Some( 0L ) => log.debug( s"Insert into the list at '$key' failed. Pivot not found." ); None
      case Some( length ) => log.debug( s"Inserted $value into the list at '$key'. New size is $length." ); Some( length )
    } recover {
      case ExecutionFailedException( _, _, ex ) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn( s"Value at '$key' is not a list." )
        throw new IllegalArgumentException( s"Value at '$key' is not a list." )
    }

  def setAdd( key: String, values: Any* ) = {
    // encodes the value
    def toEncoded( value: Any ) = encode( key, value )
    redis.sAdd( key, values map toEncoded: _* ) executing "SADD" withParameters s"$key ${ values mkString " " }" expects {
      case inserted => log.debug( s"Inserted $inserted elements into the set at '$key'." ); inserted
    } recover {
      case ExecutionFailedException( _, _, ex ) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn( s"Value at '$key' is not a set." )
        throw new IllegalArgumentException( s"Value at '$key' is not a set." )
    }
  }

  def setSize( key: String ) =
    redis.sCard( key ) executing "SCARD" withParameter key expects {
      case length => log.debug( s"The collection at '$key' has $length items." ); length
    }

  def setMembers[ T: ClassTag ]( key: String ) =
    redis.sMembers( key ) executing "SMEMBERS" withParameter key expects {
      case items =>
        log.debug( s"Returned ${ items.size } items from the collection at '$key'." )
        items.map( decode[ T ]( key, _ ) )
    }

  def setIsMember( key: String, value: Any ) =
    redis.sIsMember( key, encode( key, value ) ) executing "SISMEMBER" withParameters s"$key $value" expects {
      case true => log.debug( s"Item $value exists in the collection at '$key'." ); true
      case false => log.debug( s"Item $value does not exist in the collection at '$key'" ); false
    }

  def setRemove( key: String, values: Any* ) = {
    // encodes the value
    def toEncoded( value: Any ) = encode( key, value )

    redis.sRem( key, values map toEncoded: _* ) executing "SREM" withParameters s"$key ${ values mkString " " }" expects {
      case removed => log.debug( s"Removed $removed elements from the collection at '$key'." ); removed
    }
  }

  def hashRemove( key: String, fields: String* ) =
    redis.hDel( key, fields: _* ) executing "HDEL" withParameters s"$key ${ fields mkString " " }" expects {
      case removed => log.debug( s"Removed $removed elements from the collection at '$key'." ); removed
    }

  def hashExists( key: String, field: String ) =
    redis.hExists( key, field ) executing "HEXISTS" withParameters s"$key $field" expects {
      case true => log.debug( s"Item $field exists in the collection at '$key'." ); true
      case false => log.debug( s"Item $field does not exist in the collection at '$key'" ); false
    }

  def hashGet[ T: ClassTag ]( key: String, field: String ) =
    redis.hGet[ String ]( key, field ) executing "HGET" withParameters s"$key $field" expects {
      case Some( encoded ) => log.debug( s"Item $field exists in the collection at '$key'." ); Some( decode[ T ]( key, encoded ) )
      case None => log.debug( s"Item $field is not in the collection at '$key'." ); None
    }

  def hashGetAll[ T: ClassTag ]( key: String ) =
    redis.hGetAll[ String ]( key ) executing "HGETALL" withParameter key expects {
      case Some( encoded ) => log.debug( s"Collection at '$key' has ${ encoded.size } items." ); encoded.mapValues( decode[ T ]( key, _ ) )
      case None => log.debug( s"Collection at '$key' is empty." ); Map.empty
    }

  def hashSize( key: String ) =
    redis.hLen( key ) executing "HLEN" withParameter key expects {
      case length => log.debug( s"The collection at '$key' has $length items." ); length
    }

  def hashKeys( key: String ) =
    redis.hKeys( key ) executing "HKEYS" withParameter key expects {
      case keys => log.debug( s"The collection at '$key' defines: ${ keys mkString " " }." ); keys
    }

  def hashSet( key: String, field: String, value: Any ) =
    redis.hSet( key, field, encode( key, value ) ) executing "HSET" withParameters s"$key $field $value" expects {
      case true => log.debug( s"Item $field in the collection at '$key' was inserted." ); true
      case false => log.debug( s"Item $field in the collection at '$key' was updated." ); false
    } recover {
      case ExecutionFailedException( _, _, ex ) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn( s"Value at '$key' is not a map." )
        throw new IllegalArgumentException( s"Value at '$key' is not a map." )
    }

  def hashValues[ T: ClassTag ]( key: String ) =
    redis.hVals( key ) executing "HVALS" withParameter key expects {
      case values => log.debug( s"The collection at '$key' contains ${ values.size } values." ); values.map( decode[ T ]( key, _ ) ).toSet
    }

  def start( ) = {
    import configuration.{database, host, port}
    log.info( s"Redis cache actor started. It is connected to $host:$port?database=$database" )
  }

  /** stops the actor */
  def stop( ): Future[ Unit ] = {
    log.info( "Stopping the redis cache actor ..." )
    redis.quit().map[ Unit ] { _ => log.info( "Redis cache stopped." ) }
  }

  // start the connector
  start()
  // listen on system stop
  lifecycle.addStopHook( stop _ )
}
