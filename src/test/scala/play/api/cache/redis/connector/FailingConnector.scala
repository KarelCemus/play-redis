package play.api.cache.redis.connector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import play.api.cache.redis.exception.failed
import play.api.cache.redis.{RedisConnector, Synchronization}

/**
  * @author Karel Cemus
  */
object FailingConnector extends RedisConnector with Synchronization {

  implicit def context: ExecutionContext = global

  private def theError = new IllegalStateException( "Redis connector failure reproduction" )

  private def failKeyed( key: String, command: String ) = Future {
    failed( Some( key ), command, theError )
  }

  private def failCommand( command: String ) = Future {
    failed( None, command, theError )
  }

  def set( key: String, value: Any, expiration: Duration ): Future[ Unit ] =
    failKeyed( key, "SET" )

  def setIfNotExists( key: String, value: Any ): Future[ Boolean ] =
    failKeyed( key, "SETNX" )

  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] =
    failKeyed( key, "GET" )

  def expire( key: String, expiration: Duration ): Future[ Unit ] =
    failKeyed( key, "EXPIRE" )

  def remove( keys: String* ): Future[ Unit ] =
    failed( Some( keys.mkString( " " ) ), "DEL", theError )

  def matching( pattern: String ): Future[ Set[ String ] ] =
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
}
