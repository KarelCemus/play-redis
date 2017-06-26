package play.api.cache.redis.impl

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p> */
private[ impl ] class RedisCache[ Result[ _ ] ]( redis: RedisConnector )( implicit protected val implicitBuilder: Builders.ResultBuilder[ Result ], protected val implicitPolicy: RecoveryPolicy ) extends AbstractCacheApi[ Result ] {

  // implicit ask timeout and execution context
  import redis.{context, timeout}
  import dsl._

  override def get[ T: ClassTag ]( key: String ) =
    redis.get[ T ]( key ).recoverWithDefault( None )

  override def getAll[ T: ClassTag ]( keys: String* ): Result[ Seq[ Option[ T ] ] ] =
    redis.mGet[ T ]( keys: _* ).recoverWithDefault( keys.toList.map( _ => None ) )

  override def set( key: String, value: Any, expiration: Duration ) =
    redis.set( key, value, expiration ).recoverWithDone

  override def setIfNotExists( key: String, value: Any, expiration: Duration ) =
    redis.setIfNotExists( key, value ).map { result =>
      if ( result && expiration.isFinite( ) ) redis.expire( key, expiration )
      result
    }.recoverWithDefault( true )

  override def setAll( keyValues: (String, Any)* ): Result[ Done ] =
    redis.mSet( keyValues: _* ).recoverWithDone

  override def setAllIfNotExist( keyValues: (String, Any)* ): Result[ Boolean ] =
    redis.mSetIfNotExist( keyValues: _* ).recoverWithDefault( true )

  override def append( key: String, value: String, expiration: Duration ): Result[ Done ] =
    redis.append( key, value ).flatMap { result =>
      // if the new string length is equal to the appended string, it means they should equal
      // when the finite duration is required, set it
      if ( result == value.length && expiration.isFinite() ) redis.expire( key, expiration ) else Future.successful[ Unit ]( Unit )
    }.recoverWithDone

  override def expire( key: String, expiration: Duration ) =
    redis.expire( key, expiration ).recoverWithDone

  override def matching( pattern: String ) =
    redis.matching( pattern ).recoverWithDefault( Seq.empty[ String ] )

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
    redis.remove( key ).recoverWithDone

  override def remove( key1: String, key2: String, keys: String* ) =
    redis.remove( key1 +: key2 +: keys: _* ).recoverWithDone

  override def removeAll( keys: String* ): Result[ Done ] =
    redis.remove( keys: _* ).recoverWithDone

  override def removeMatching( pattern: String ): Result[ Done ] =
    redis.matching( pattern ).flatMap( keys => redis.remove( keys: _* ) ).recoverWithDone

  override def invalidate( ) =
    redis.invalidate( ).recoverWithDone

  override def exists( key: String ) =
    redis.exists( key ).recoverWithDefault( false )

  override def increment( key: String, by: Long ) =
    redis.increment( key, by ).recoverWithDefault( by )

  override def decrement( key: String, by: Long ) =
    increment( key, -by )

  override def list[ T: ClassTag ]( key: String ): RedisList[ T, Result ] =
    new RedisListImpl( key, redis )

  override def set[ T: ClassTag ]( key: String ): RedisSet[ T, Result ] =
    new RedisSetImpl( key, redis )

  override def map[ T: ClassTag ]( key: String ): RedisMap[ T, Result ] =
    new RedisMapImpl( key, redis )
}
