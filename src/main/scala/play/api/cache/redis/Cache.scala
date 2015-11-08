package play.api.cache.redis

import javax.inject._

import play.api._
import play.api.inject._

/** Synchronous and blocking implementation of the connection to the redis database */
trait CacheApi extends InternalCacheApi[ Builders.Identity ]

@Singleton
class SyncRedis @Inject( )( implicit application: Application, lifecycle: ApplicationLifecycle, configuration: Configuration ) extends RedisCache( )( Builders.SynchronousBuilder, application, lifecycle, configuration ) with CacheApi with play.api.cache.CacheApi

/** Asynchronous non-blocking implementation of the connection to the redis database */
trait CacheAsyncApi extends InternalCacheApi[ Builders.Future ]

@Singleton
class AsyncRedis @Inject( )( implicit application: Application, lifecycle: ApplicationLifecycle, configuration: Configuration ) extends RedisCache( )( Builders.AsynchronousBuilder, application, lifecycle, configuration ) with CacheAsyncApi

@Singleton
class RedisCacheModule extends Module {

  val log = Logger( "play.api.cache.redis" )

  def bindings( environment: Environment, configuration: play.api.Configuration ) = {
    // default binding for Play's CacheApi to SyncCache to replace default EHCache
    val default = bind[ play.api.cache.CacheApi ].to[ SyncRedis ]
    // enable sync module when required
    val sync = bind[ CacheApi ].to[ SyncRedis ]
    // enable async module when required
    val async = bind[ CacheAsyncApi ].to[ AsyncRedis ]
    // configuration provider
    val config: Option[ Binding[ Configuration ] ] = configuration.getString( "play.cache.redis.configuration" ) match {
      // required local implementation
      case Some( "local" ) => Some( bind[ Configuration ].to[ LocalConfiguration ] )
      // required Heroku implementation
      case Some( "heroku" ) => Some( bind[ Configuration ].to( HerokuConfigurationProvider ) )
      // supplied custom implementation
      case Some( "none" ) => None // ignore, supplied custom configuration provider
      // found but unrecognized
      case Some( other ) => log.error( "Unrecognized configuration provider in key 'play.cache.redis.configuration'. Expected values are 'none', 'local', and 'heroku'." ); None
      // key is missing
      case _ => log.error( "Configuration provider definition is missing. Please define the key 'play.cache.redis.configuration'. Expected values are 'none', 'local', and 'heroku'." ); None
    }
    // return bindings
    Seq( sync, async, default ) ++ config
  }
}
