package play.api.cache.redis.configuration

import play.api.cache.redis.{Configuration => RedisConfiguration}
import play.api.inject.Module
import play.api.{Configuration, Environment, Logger}
import play.api.cache.redis.exception._
/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
object ConfigurationModule extends Module {

  override def bindings( environment: Environment, configuration: Configuration ) =
    provider( configuration ).toSeq

  /** returns configuration provider based on the application configuration */
  private def provider( configuration: play.api.Configuration ) = configuration.getString( "play.cache.redis.configuration" ) match {
    case Some( "static" ) => // required static implementation using application.conf
      Some( bind[ RedisConfiguration ].to[ ConfigurationFile ] )
    case Some( "env" ) if connectionStringVariable( configuration ).nonEmpty => // required environmental implementation
      Some( bind[ RedisConfiguration ].to( new ConnectionStringProvider( connectionStringVariable( configuration ).get ) ) )
    case Some( "env" ) => // required environmental implementation but the variable with the connection string is unknown
      invalidConfiguration( "Unknown name of the environmental variable with the connection string. Please define 'play.redis.cache.connection-string-variable' value in the application.conf." )
    case Some( "heroku" ) => // required heroku configuration
      Some( bind[ RedisConfiguration ].to( new ConnectionStringProvider( "REDIS_URL" ) ) )
    case Some( "heroku-cloud" ) => // required heroku configuration
      Some( bind[ RedisConfiguration ].to( new ConnectionStringProvider( "REDISCLOUD_URL" ) ) )
    case Some( "custom" ) => // supplied custom implementation
      None // ignore, supplied custom configuration provider
    case Some( other ) => // found but unrecognized
      invalidConfiguration( "Unrecognized configuration provider in key 'play.cache.redis.configuration'. Expected values are 'custom', 'static', 'heroku', 'heroku-cloud', and 'env'." )
    case None => // key is missing
      invalidConfiguration( "Configuration provider definition is missing. Please define the key 'play.cache.redis.configuration'. Expected values are 'custom', 'static', 'herouk', 'heroku-cloud', and 'env'." )
  }

  /** returns name of the variable with the connection string */
  private def connectionStringVariable( configuration: play.api.Configuration ) =
    configuration.getString( "play.cache.redis.connection-string-variable" )
}
