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
class RedisCacheModule extends Module with ModuleConfiguration.DefaultBinding with ModuleConfiguration.SyncOrAsync with ModuleConfiguration.ConfigurationProvider

/** Provides optional configurations of the redis module */
object ModuleConfiguration {

  trait DefaultBinding extends Module {
    override def bindings( environment: Environment, configuration: play.api.Configuration ): Seq[ Binding[ _ ] ] = {
      // default binding for Play's CacheApi to SyncCache to replace default EHCache
      Seq( bind[ play.api.cache.CacheApi ].to[ SyncRedis ] )
    }
  }

  trait SyncOrAsync extends Module {
    abstract override def bindings( environment: Environment, configuration: play.api.Configuration ) = {
      // enable sync module when required
      val sync = bind[ CacheApi ].to[ SyncRedis ]
      // enable async module when required
      val async = bind[ CacheAsyncApi ].to[ AsyncRedis ]
      // add to other bindings
      super.bindings( environment, configuration ) :+ sync :+ async
    }
  }

  trait ConfigurationProvider extends Module {

    protected val log = Logger( "play.api.cache.redis" )

    abstract override def bindings( environment: Environment, configuration: play.api.Configuration ) =
      super.bindings( environment, configuration ) ++ provider( configuration )

    /** returns configuration provider based on the application configuration */
    private def provider( configuration: play.api.Configuration ) = configuration.getString( "play.cache.redis.configuration" ) match {
      case Some( "static" ) => // required static implementation using application.conf
        Some( bind[ Configuration ].to[ StaticConfiguration ] )
      case Some( "env" ) if connectionStringVariable( configuration ).nonEmpty => // required environmental implementation
        Some( bind[ Configuration ].to( new EnvironmentConfigurationProvider( connectionStringVariable( configuration ).get ) ) )
      case Some( "env" ) => // required environmental implementation but the variable with the connection string is unknown
        log.error( "Unknown name of the environmental variable with the connection string. Please define 'play.redis.cache.connection-string-variable' value in the application.conf." )
        None
      case Some( "custom" ) => // supplied custom implementation
        None // ignore, supplied custom configuration provider
      case Some( other ) => // found but unrecognized
        log.error( "Unrecognized configuration provider in key 'play.cache.redis.configuration'. Expected values are 'custom', 'static', and 'env'." )
        None
      case _ => // key is missing
        log.error( "Configuration provider definition is missing. Please define the key 'play.cache.redis.configuration'. Expected values are 'custom', 'static', and 'env'." )
        None
    }

    /** returns name of the variable with the connection string */
    private def connectionStringVariable( configuration: play.api.Configuration ) =
      configuration.getString( "play.cache.redis.connection-string-variable" )
  }
}
