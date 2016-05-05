package play.api.cache.redis.impl

import javax.inject.{Inject, Singleton}

import play.api.cache.redis._

/**
  * Implementation of **synchronous** and **blocking** Redis API. It also implements standard Play Scala CacheApi
  *
  * @author Karel Cemus
  */
@Singleton
private[ impl ] class SyncRedis @Inject( )( redis: RedisConnector )
  extends RedisCache( redis )( Builders.SynchronousBuilder ) with CacheApi with play.api.cache.CacheApi
