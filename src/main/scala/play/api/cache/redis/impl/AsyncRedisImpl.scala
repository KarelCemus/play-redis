package play.api.cache.redis.impl

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.cache.redis._

/**
  * Implementation of **asynchronous** Redis API
  */
private[impl] trait AsyncRedis extends play.api.cache.AsyncCacheApi with CacheAsyncApi

private[impl] class AsyncRedisImpl(redis: RedisConnector)(implicit runtime: RedisRuntime) extends RedisCache(redis, Builders.AsynchronousBuilder) with AsyncRedis {

  def getOrElseUpdate[T: ClassTag](key: String, expiration: Duration)(orElse: => Future[T]) =
    getOrFuture[T](key, expiration)(orElse)

  def removeAll(): Future[Done] = invalidate()
}
