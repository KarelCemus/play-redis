package play.api.cache.redis.impl

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p> */
private[ impl ] class RedisCache[ Result[ _ ] ]( redis: RedisConnector, builder: Builders.ResultBuilder[ Result ] )( implicit runtime: RedisRuntime ) extends AbstractCacheApi[ Result ] {

  // implicit ask timeout and execution context
  import dsl._

  @inline implicit protected def implicitBuilder: Builders.ResultBuilder[ Result ] = builder

  def get[ T: ClassTag ]( key: String ) =
    redis.get[ T ]( key ).recoverWithDefault( None )

  def getAll[ T: ClassTag ]( keys: String* ): Result[ Seq[ Option[ T ] ] ] =
    redis.mGet[ T ]( keys: _* ).recoverWithDefault( keys.toList.map( _ => None ) )

  def set( key: String, value: Any, expiration: Duration ) =
    redis.set( key, value, expiration ).recoverWithDone

  def setIfNotExists( key: String, value: Any, expiration: Duration ) =
    redis.setIfNotExists( key, value ).map { result =>
      if ( result && expiration.isFinite( ) ) redis.expire( key, expiration )
      result
    }.recoverWithDefault( true )

  def setAll( keyValues: (String, Any)* ): Result[ Done ] =
    redis.mSet( keyValues: _* ).recoverWithDone

  def setAllIfNotExist( keyValues: (String, Any)* ): Result[ Boolean ] =
    redis.mSetIfNotExist( keyValues: _* ).recoverWithDefault( true )

  def append( key: String, value: String, expiration: Duration ): Result[ Done ] =
    redis.append( key, value ).flatMap { result =>
      // if the new string length is equal to the appended string, it means they should equal
      // when the finite duration is required, set it
      if ( result == value.length && expiration.isFinite() ) redis.expire( key, expiration ) else Future.successful[ Unit ]( Unit )
    }.recoverWithDone

  def expire( key: String, expiration: Duration ) =
    redis.expire( key, expiration ).recoverWithDone

  def matching( pattern: String ) =
    redis.matching( pattern ).recoverWithDefault( Seq.empty[ String ] )

  def getOrElse[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => T ) =
    getOrFuture( key, expiration )( orElse.toFuture ).recoverWithDefault( orElse )

  def getOrFuture[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => Future[ T ] ): Future[ T ] =
    redis.get[ T ]( key ).flatMap {
      // cache hit, return the unwrapped value
      case Some( value: T ) => value.toFuture
      // cache miss, compute the value, store it into cache and return the value
      case None => orElse flatMap ( value => redis.set( key, value, expiration ) map ( _ => value ) )
    }.recoverWithFuture( orElse )

  def remove( key: String ) =
    redis.remove( key ).recoverWithDone

  def remove( key1: String, key2: String, keys: String* ) =
    redis.remove( key1 +: key2 +: keys: _* ).recoverWithDone

  def removeAll( keys: String* ): Result[ Done ] =
    redis.remove( keys: _* ).recoverWithDone

  def removeMatching( pattern: String ): Result[ Done ] =
    redis.matching( pattern ).flatMap( keys => redis.remove( keys: _* ) ).recoverWithDone

  def invalidate( ) =
    redis.invalidate( ).recoverWithDone

  def exists( key: String ) =
    redis.exists( key ).recoverWithDefault( false )

  def increment( key: String, by: Long ) =
    redis.increment( key, by ).recoverWithDefault( by )

  def decrement( key: String, by: Long ) =
    increment( key, -by )

  def list[ T: ClassTag ]( key: String ): RedisList[ T, Result ] =
    new RedisListImpl( key, redis )

  def set[ T: ClassTag ]( key: String ): RedisSet[ T, Result ] =
    new RedisSetImpl( key, redis )

  def map[ T: ClassTag ]( key: String ): RedisMap[ T, Result ] =
    new RedisMapImpl( key, redis )

  override def toString = s"RedisCache(name=${ runtime.name })"
}
