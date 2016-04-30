package play.api.cache.redis

import javax.inject.Singleton

import play.api.cache.redis.connector.{AkkaSerializer, AkkaSerializerImpl, RedisConnector, RedisConnectorImpl}
import play.api.inject.{Binding, Module}
import play.api.{Environment, Logger}

/** Provides optional configurations of the redis module */
object ModuleConfiguration {

  /** Provides core bindings mapping APIs to their implementations and provides.
    *
    * Note: These might be possibly extracted from the core into optional setup in future releases.
    */
  trait CoreBinding extends Module {
    override def bindings( environment: Environment, configuration: play.api.Configuration ): Seq[ Binding[ _ ] ] = Seq(
      // binds akka serializer to its implementation
      bind[ AkkaSerializer ].to[ AkkaSerializerImpl ],
      // redis actor encapsulating brando
      bind[ RedisActor ].toProvider[ RedisActorProvider ],
      // redis connector implementing the protocol
      bind[ RedisConnector ].to[ RedisConnectorImpl ],
      // extracts the configuration
      bind[ ConnectionSettings ].toProvider[ ConnectionSettingsProvider ]
    )
  }

  /** provides default implementation replacing default Play CacheApi implementation */
  trait DefaultBinding extends Module {
    abstract override def bindings( environment: Environment, configuration: play.api.Configuration ): Seq[ Binding[ _ ] ] = {
      // default binding for Play's CacheApi to SyncCache to replace default EHCache
      super.bindings( environment, configuration ) ++ Seq( bind[ play.api.cache.CacheApi ].to[ SyncRedis ] )
    }
  }

  /** appends synchronous and asynchronous implementation of the Api to the bindings */
  trait SyncOrAsync extends Module {
    abstract override def bindings( environment: Environment, configuration: play.api.Configuration ) = {
      // enable sync module when required
      val sync = bind[ CacheApi ].to[ SyncRedis ]
      // enable async module when required
      val async = bind[ CacheAsyncApi ].to[ AsyncRedis ]
      // java api
      val java = bind[ play.cache.CacheApi ].to[ JavaRedis ]
      // add to other bindings
      super.bindings( environment, configuration ) :+ sync :+ async :+ java
    }
  }

  /** enables various sources of the redis connector configuration */
  trait ConfigurationProvider extends Module {

    private val log = Logger( "play.api.cache.redis" )

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

import play.api.cache.redis.ModuleConfiguration._

/** Play framework module implementing play.api.cache.CacheApi for redis-server key/value storage. For more details
  * see README.
  *
  * @author Karel Cemus
  */
@Singleton
class RedisCacheModule extends Module with CoreBinding with DefaultBinding with SyncOrAsync with ConfigurationProvider
