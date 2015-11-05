package play.api.cache.redis

import javax.inject._

import play.api._
import play.api.inject._

/** Synchronous and blocking implementation of the connection to the redis database */
trait CacheApi extends InternalCacheApi[ Builders.Identity ]

@Singleton
class SyncRedis @Inject( )( implicit application: Application, lifecycle: ApplicationLifecycle ) extends RedisCache( )( Builders.SynchronousBuilder, application, lifecycle ) with CacheApi with play.api.cache.CacheApi

/** Asynchronous non-blocking implementation of the connection to the redis database */
trait CacheAsyncApi extends InternalCacheApi[ Builders.Future ]

@Singleton
class AsyncRedis @Inject( )( implicit application: Application, lifecycle: ApplicationLifecycle ) extends RedisCache( )( Builders.AsynchronousBuilder, application, lifecycle ) with CacheAsyncApi

@Singleton
class RedisCacheModule extends Module with Config {

  def bindings( environment: Environment, configuration: Configuration ) = {
    // enable sync module when required
    val sync: Option[ Binding[ _ ] ] = if ( implementations.contains( "sync" ) ) Some( bind[ CacheApi ].to[ SyncRedis ] ) else None
    // enable async module when required
    val async: Option[ Binding[ _ ] ] = if ( implementations.contains( "async" ) ) Some( bind[ CacheAsyncApi ].to[ AsyncRedis ] ) else None
    // return bindings
    Seq( sync, async ).flatten
  }
}
