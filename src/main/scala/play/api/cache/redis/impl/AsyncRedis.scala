package play.api.cache.redis.impl

import javax.inject.{Inject, Singleton}

import play.api.cache.redis._

/**
  * Implementation of **asynchronous** Redis API
  *
  * @author Karel Cemus
  */
@Singleton
private[ impl ] class AsyncRedis @Inject( )( redis: RedisConnector )
  extends RedisCache( redis )( Builders.AsynchronousBuilder ) with CacheAsyncApi
