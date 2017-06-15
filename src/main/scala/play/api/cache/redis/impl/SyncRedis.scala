package play.api.cache.redis.impl

import javax.inject.{Inject, Singleton}

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.cache.redis._

/**
  * Implementation of **synchronous** and **blocking** Redis API. It also implements standard Play Scala CacheApi
  *
  * @author Karel Cemus
  */
@Singleton
private[ impl ] class SyncRedis @Inject( )( redis: RedisConnector, policy: RecoveryPolicy )
  extends RedisCache( redis )( Builders.SynchronousBuilder, policy ) with CacheApi {

  // implicit ask timeout and execution context
  import redis.{context, timeout}
  import Implicits._

  override def getOrElse[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => T ) = {
    // compute or all and try to set it into the cache
    def computeAndSet = {
      // compute
      val value = orElse
      // set the value and finally return the computed value regardless the result of set
      redis.set( key, value, expiration ).map( _ => value ) recoverWithDefault value
    }
    // try to hit the cache, if hit return, if miss or failure then set and return orElse
    redis.get[ T ]( key ).recoverWithDefault( Some( computeAndSet ) ) getOrElse computeAndSet
  }
}
