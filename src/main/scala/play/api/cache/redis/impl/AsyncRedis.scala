package play.api.cache.redis.impl

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.cache.redis._

/**
  * Implementation of **asynchronous** Redis API
  *
  * @author Karel Cemus
  */
@Singleton
private[ impl ] class AsyncRedis @Inject()( redis: RedisConnector, policy: RecoveryPolicy ) extends RedisCache( redis )( Builders.AsynchronousBuilder, policy ) with CacheAsyncApi with play.api.cache.AsyncCacheApi {

  def getOrElseUpdate[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => Future[ T ] ) = getOrFuture[ T ]( key, expiration )( orElse )

  def removeAll( ): Future[ Done ] = invalidate()
}
