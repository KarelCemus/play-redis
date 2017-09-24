package play.api.cache.redis.impl

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.cache.redis._

/**
  * Implementation of **asynchronous** Redis API
  *
  * @author Karel Cemus
  */
private[ impl ] class AsyncRedis( redis: RedisConnector )( implicit runtime: RedisRuntime )
  extends RedisCache( redis, Builders.AsynchronousBuilder )
  with play.api.cache.AsyncCacheApi
  with CacheAsyncApi
{

  def getOrElseUpdate[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => Future[ T ] ) = getOrFuture[ T ]( key, expiration )( orElse )

  def removeAll( ): Future[ Done ] = invalidate()
}
