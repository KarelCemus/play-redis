package play.api.cache.redis

import javax.inject.Singleton

import play.api.cache.redis.configuration.{EnvironmentConfigurationProvider, StaticConfiguration}
import play.api.cache.redis.connector.RedisConnectorModule
import play.api.cache.redis.impl.{AsyncRedis, JavaRedis, SyncRedis}
import play.api.inject.Module
import play.api.{Environment, Logger}

/** Play framework module implementing play.api.cache.CacheApi for redis-server key/value storage. For more details
  * see README.
  *
  * @author Karel Cemus
  */
@Singleton
class RedisCacheModule extends Module {

  private val log = Logger( "play.api.cache.redis" )

  override def bindings( environment: Environment, configuration: play.api.Configuration ) = Seq(
    // extracts the configuration
    bind[ ConnectionSettings ].toProvider[ ConnectionSettingsProvider ],
    // default binding for Play's CacheApi to SyncCache to replace default EHCache
    bind[ play.api.cache.CacheApi ].to[ SyncRedis ],
    // enable sync module when required
    bind[ CacheApi ].to[ SyncRedis ],
    // enable async module when required
    bind[ CacheAsyncApi ].to[ AsyncRedis ],
    // java api
    bind[ play.cache.CacheApi ].to[ JavaRedis ]
  ) ++ provider( configuration ) ++ RedisConnectorModule.bindings( environment, configuration )

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
