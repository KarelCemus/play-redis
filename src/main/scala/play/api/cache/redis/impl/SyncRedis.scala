package play.api.cache.redis.impl

import javax.inject.{Inject, Singleton}

import play.api.cache.redis.{CacheApi, ConnectionSettings, RedisConnector}

/**
  * @author Karel Cemus
  */
@Singleton
class SyncRedis @Inject( )( redis: RedisConnector, settings: ConnectionSettings )
  extends RedisCache( redis, settings )( Builders.SynchronousBuilder ) with CacheApi with play.api.cache.CacheApi
