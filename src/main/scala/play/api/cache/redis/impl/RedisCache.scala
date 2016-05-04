package play.api.cache.redis.impl

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p> */
class RedisCache[ Result[ _ ] ]( redis: RedisConnector, settings: ConnectionSettings )( implicit builder: Builders.ResultBuilder[ Result ] ) extends InternalCacheApi[ Result ] with Implicits {

  // implicit execution context and ask timeout
  import settings.{invocationContext, timeout}

  override def get[ T: ClassTag ]( key: String ) =
    redis.get[ T ]( key )

  override def set( key: String, value: Any, expiration: Duration ) =
    redis.set( key, value, expiration )

  override def expire( key: String, expiration: Duration ) =
    redis.expire( key, expiration )

  override def matching( pattern: String ) =
    redis.matching( pattern )

  override def getOrElse[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => T ) =
    getOrFuture( key, expiration )( orElse.toFuture )

  override def getOrFuture[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => Future[ T ] ): Future[ T ] =
    redis.get[ T ]( key ) flatMap {
      // cache hit, return the unwrapped value
      case Some( value: T ) => value.toFuture
      // cache miss, compute the value, store it into cache and return the value
      case None => orElse flatMap ( value => redis.set( key, value, expiration ) map ( _ => value ) )
    }

  override def remove( key: String ) =
    redis.remove( key )

  override def remove( key1: String, key2: String, keys: String* ) =
    redis.remove( key1 +: key2 +: keys: _* )

  override def removeAll( keys: String* ): Result[ Unit ] =
    redis.remove( keys: _* )

  override def removeMatching( pattern: String ): Result[ Unit ] =
    redis.matching( pattern ) flatMap ( keys => redis.remove( keys.toSeq: _* ) )

  override def invalidate( ) =
    redis.invalidate( )

  override def exists( key: String ) =
    redis.exists( key )
}
