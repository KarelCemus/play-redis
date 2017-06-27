package play.api.cache.redis.connector

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.cache.redis.Synchronization
import play.api.cache.redis.exception.ExecutionFailedException

/**
  * @author Karel Cemus
  */
object FailingConnector extends RedisConnector with Synchronization {

  implicit def context: ExecutionContext = ExecutionContext.Implicits.global

  private def theError = new IllegalStateException( "Redis connector failure reproduction" )

  private def failKeyed( key: String, command: String ) = Future.failed {
    ExecutionFailedException( Some( key ), command, theError )
  }

  private def failCommand( command: String ) = Future.failed {
    ExecutionFailedException( None, command, theError )
  }

  def set( key: String, value: Any, expiration: Duration ): Future[ Unit ] =
    failKeyed( key, "SET" )

  def setIfNotExists( key: String, value: Any ): Future[ Boolean ] =
    failKeyed( key, "SETNX" )

  def mSet( keyValues: (String, Any)* ): Future[ Unit ] =
    failKeyed( keyValues.map( _._1 ).mkString( " " ), "MSET" )

  def mSetIfNotExist( keyValues: (String, Any)* ): Future[ Boolean ] =
    failKeyed( keyValues.map( _._1 ).mkString( " " ), "MSETNX" )

  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] =
    failKeyed( key, "GET" )

  def mGet[ T: ClassTag ]( keys: String* ): Future[ List[ Option[ T ] ] ] =
    failKeyed( keys.mkString( " " ), "MGET" )

  def expire( key: String, expiration: Duration ): Future[ Unit ] =
    failKeyed( key, "EXPIRE" )

  def remove( keys: String* ): Future[ Unit ] =
    failKeyed( keys.mkString( " " ), "DEL" )

  def matching( pattern: String ): Future[ Seq[ String ] ] =
    failCommand( "KEYS" )

  def invalidate( ): Future[ Unit ] =
    failCommand( "FLUSHDB" )

  def ping( ): Future[ Unit ] =
    failCommand( "PING" )

  def exists( key: String ): Future[ Boolean ] =
    failKeyed( key, "EXISTS" )

  def increment( key: String, by: Long ): Future[ Long ] =
    failKeyed( key, "INCR" )

  def append( key: String, value: String ): Future[ Long ] =
    failKeyed( key, "APPEND" )

  def listPrepend( key: String, value: Any* ) =
    failKeyed( key, "LPUSH" )

  def listAppend( key: String, value: Any* ) =
    failKeyed( key, "RPUSH" )

  def listSize( key: String ) =
    failKeyed( key, "LLEN" )

  def listSetAt( key: String, position: Int, value: Any ) =
    failKeyed( key, "LSET" )

  def listHeadPop[ T: ClassTag ]( key: String ) =
    failKeyed( key, "LPOP" )

  def listSlice[ T: ClassTag ]( key: String, start: Int, end: Int ) =
    failKeyed( key, "LRANGE" )

  def listRemove( key: String, value: Any, count: Int ) =
    failKeyed( key, "LREM" )

  def listTrim( key: String, start: Int, end: Int ) =
    failKeyed( key, "LTRIM" )

  def listInsert( key: String, pivot: Any, value: Any ) =
    failKeyed( key, "LINSERT" )

  def setAdd( key: String, value: Any* ) =
    failKeyed( key, "SADD" )

  def setSize( key: String ) =
    failKeyed( key, "SCARD" )

  def setMembers[ T: ClassTag ]( key: String ) =
    failKeyed( key, "SMEMBERS" )

  def setIsMember( key: String, value: Any ) =
    failKeyed( key, "SISMEMBER" )

  def setRemove( key: String, value: Any* ) =
    failKeyed( key, "SREM" )

  def hashRemove( key: String, field: String* ) =
    failKeyed( key, "HREM" )

  def hashExists( key: String, field: String ) =
    failKeyed( key, "HEXISTS" )

  def hashGet[ T: ClassTag ]( key: String, field: String ) =
    failKeyed( key, "HGET" )

  def hashGetAll[ T: ClassTag ]( key: String ) =
    failKeyed( key, "HGETALL" )

  def hashSize( key: String ) =
    failKeyed( key, "HLEN" )

  def hashKeys( key: String ) =
    failKeyed( key, "HKEYS" )

  def hashSet( key: String, field: String, value: Any ) =
    failKeyed( key, "HSET" )

  def hashValues[ T: ClassTag ]( key: String ) =
    failKeyed( key, "HVALS" )
}
