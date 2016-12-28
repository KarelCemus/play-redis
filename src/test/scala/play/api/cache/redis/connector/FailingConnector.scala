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

  def set( key: String, value: Any, expiration: Duration ): Future[ Unit ] = Future {
    failed( Some( key ), "SET", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def setIfNotExists( key: String, value: Any ): Future[ Boolean ] = Future {
    failed( Some( key ), "SETNX", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] = Future {
    failed( Some( key ), "GET", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def expire( key: String, expiration: Duration ): Future[ Unit ] = Future {
    failed( Some( key ), "EXPIRE", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def remove( keys: String* ): Future[ Unit ] = Future {
    failed( Some( keys.mkString( " " ) ), "DEL", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def matching( pattern: String ): Future[ Set[ String ] ] = Future {
    failed( None, "KEYS", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def invalidate( ): Future[ Unit ] = Future {
    failed( None, "FLUSHDB", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def ping( ): Future[ Unit ] = Future {
    failed( None, "PING", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def exists( key: String ): Future[ Boolean ] = Future {
    failed( Some( key ), "EXISTS", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def increment( key: String, by: Long ): Future[ Long ] = Future {
    failed( Some( key ), "INCR", new IllegalStateException( "Redis connector failure reproduction" ) )
  }

  def append( key: String, value: String ): Future[ Long ] = Future {
    failed( Some( key ), "APPEND", new IllegalStateException( "Redis connector failure reproduction" ) )
  }
}
