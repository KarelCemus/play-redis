package play.api.cache.redis.impl

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p> */
private[ impl ] class RedisCache[ Result[ _ ] ]( redis: RedisConnector )( implicit builder: Builders.ResultBuilder[ Result ], policy: RecoveryPolicy ) extends AbstractCacheApi[ Result ] with Implicits {

  // implicit ask timeout and execution context
  import redis.{context, timeout}

  override def get[ T: ClassTag ]( key: String ) =
    redis.get[ T ]( key ).recoverWithDefault( None )

  override def set( key: String, value: Any, expiration: Duration ) =
    redis.set( key, value, expiration ).recoverWithUnit

  override def setIfNotExists( key: String, value: Any, expiration: Duration ) =
    redis.setIfNotExists( key, value ).map { result =>
      if ( result && expiration.isFinite( ) ) redis.expire( key, expiration )
      result
    }.recoverWithDefault( true )

  override def append( key: String, value: String, expiration: Duration ): Result[ Unit ] =
    redis.append( key, value ).map[ Unit ] { result =>
      // if the new string length is equal to the appended string, it means they should equal
      // when the finite duration is required, set it
      if ( result == value.length && expiration.isFinite( ) ) redis.expire( key, expiration )
    }.recoverWithUnit

  override def expire( key: String, expiration: Duration ) =
    redis.expire( key, expiration ).recoverWithUnit

  override def matching( pattern: String ) =
    redis.matching( pattern ).recoverWithDefault( Set.empty[ String ] )

  override def getOrElse[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => T ) =
    getOrFuture( key, expiration )( orElse.toFuture ).recoverWithDefault( orElse )

  override def getOrFuture[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => Future[ T ] ): Future[ T ] =
    redis.get[ T ]( key ).flatMap {
      // cache hit, return the unwrapped value
      case Some( value: T ) => value.toFuture
      // cache miss, compute the value, store it into cache and return the value
      case None => orElse flatMap ( value => redis.set( key, value, expiration ) map ( _ => value ) )
    }.recoverWithFuture( orElse )

  override def remove( key: String ) =
    redis.remove( key ).recoverWithUnit

  override def remove( key1: String, key2: String, keys: String* ) =
    redis.remove( key1 +: key2 +: keys: _* ).recoverWithUnit

  override def removeAll( keys: String* ): Result[ Unit ] =
    redis.remove( keys: _* ).recoverWithUnit

  override def removeMatching( pattern: String ): Result[ Unit ] =
    redis.matching( pattern ).flatMap( keys => redis.remove( keys.toSeq: _* ) ).recoverWithUnit

  override def invalidate( ) =
    redis.invalidate( ).recoverWithUnit

  override def exists( key: String ) =
    redis.exists( key ).recoverWithDefault( false )

  override def increment( key: String, by: Long ) =
    redis.increment( key, by ).recoverWithDefault( by )

  override def decrement( key: String, by: Long ) =
    increment( key, -by )
}
