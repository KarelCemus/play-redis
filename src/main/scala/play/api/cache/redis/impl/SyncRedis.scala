package play.api.cache.redis.impl

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.cache.redis._

/**
  * Implementation of **synchronous** and **blocking** Redis API. It also implements standard Play Scala CacheApi
  */
private[impl] class SyncRedis(redis: RedisConnector)(implicit runtime: RedisRuntime) extends RedisCache(redis, Builders.SynchronousBuilder) with CacheApi {
  // helpers for dsl
  import dsl._

  override def getOrElse[T: ClassTag](key: String, expiration: Duration)(orElse: => T) = key.prefixed { key =>
    // note: this method is overridden so the `orElse` won't be included in the timeout
    // compute orElse and try to set it into the cache
    def computeAndSet = {
      // compute
      val value = orElse
      // set the value and finally return the computed value regardless the result of set
      runtime.invocation.invoke(redis.set(key, value, expiration), thenReturn = value) recoverWithDefault value
    }
    // try to hit the cache, return on hit, set and return orElse on miss or failure
    redis.get[T](key).recoverWithDefault(Some(computeAndSet)) getOrElse computeAndSet
  }
}
