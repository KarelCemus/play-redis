package play.api.cache.redis.configuration

import play.api.cache.redis.{Configuration => RedisConfiguration}
import play.api.inject.Module
import play.api.{Configuration, Environment, Logger}

/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
object ConfigurationModule extends Module {

  private def log = Logger( "play.api.cache.redis" ) // todo make logs exceptions

  override def bindings( environment: Environment, configuration: Configuration ) =
    provider( configuration ).toSeq

  /** returns configuration provider based on the application configuration */
  private def provider( configuration: play.api.Configuration ) = configuration.getString( "play.cache.redis.configuration" ) match {
    case Some( "static" ) => // required static implementation using application.conf
      Some( bind[ RedisConfiguration ].to[ StaticConfiguration ] )
    case Some( "env" ) if connectionStringVariable( configuration ).nonEmpty => // required environmental implementation
      Some( bind[ RedisConfiguration ].to( new EnvironmentConfigurationProvider( connectionStringVariable( configuration ).get ) ) )
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
