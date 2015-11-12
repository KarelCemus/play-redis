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
      case Some( "static" ) => Some( bind[ Configuration ].to[ StaticConfiguration ] )
      // required environmental implementation
      case Some( "env" ) if connectionStringVariable( configuration ).nonEmpty => Some( bind[ Configuration ].to( new EnvironmentConfigurationProvider( connectionStringVariable( configuration ).get ) ) )
      // required environmental implementation but the variable with the connection string is unknown
      case Some( "env" ) => log.error( "Unknown name of the environmental variable with the connection string. Please define 'play.redis.cache.connection-string-variable' value in the application.conf." ); None
      // supplied custom implementation
      case Some( "custom" ) => None // ignore, supplied custom configuration provider
      // found but unrecognized
      case Some( other ) => log.error( "Unrecognized configuration provider in key 'play.cache.redis.configuration'. Expected values are 'custom', 'static', and 'env'." ); None
      // key is missing
      case _ => log.error( "Configuration provider definition is missing. Please define the key 'play.cache.redis.configuration'. Expected values are 'custom', 'static', and 'env'." ); None
    }
    // return bindings
    Seq( sync, async, default ) ++ config
  }

  /** returns name of the variable with the connection string */
  private def connectionStringVariable( configuration: play.api.Configuration ) =
    configuration.getString( "play.cache.redis.connection-string-variable" )
}
