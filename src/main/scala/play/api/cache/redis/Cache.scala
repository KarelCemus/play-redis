package play.api.cache.redis

import javax.inject.Inject

import play.api.Application

/** Synchronous and blocking implementation of the connection to the redis database */
trait CacheApi extends InternalCacheApi[ Builders.Identity ]
class SyncRedis @Inject() ( implicit application: Application ) extends RedisCache()( Builders.SynchronousBuilder, application ) with CacheApi

/** Asynchronous non-blocking implementation of the connection to the redis database */
trait CacheAsyncApi extends InternalCacheApi[ Builders.Future ]
class AsyncRedis @Inject() ( implicit application: Application ) extends RedisCache()( Builders.AsynchronousBuilder, application ) with CacheAsyncApi
