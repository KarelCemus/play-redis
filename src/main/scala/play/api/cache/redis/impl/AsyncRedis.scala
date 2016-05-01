package play.api.cache.redis.impl

import javax.inject.{Inject, Singleton}

import play.api.cache.redis.{CacheAsyncApi, ConnectionSettings, RedisConnector}

/**
  * @author Karel Cemus
  */
@Singleton
class AsyncRedis @Inject( )( redis: RedisConnector, settings: ConnectionSettings )
  extends RedisCache( redis, settings )( Builders.AsynchronousBuilder ) with CacheAsyncApi
