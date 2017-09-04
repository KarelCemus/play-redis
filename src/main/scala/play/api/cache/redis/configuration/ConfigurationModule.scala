package play.api.cache.redis.configuration

import play.api.cache.redis.exception._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

/**
  * Dispatches redis url configuration and declares methods
  * to initialize the configuration provider properly.
  * This is reusable implementation encapsulating all supported values
  * in configuration and to ensure consistency if the keys are
  * added or changed.
  *
  * @author Karel Cemus
  */
private[ configuration ] trait ConfigurationResolver[ T ] {

  /** returns configuration provider based on the application configuration */
  def resolve( configuration: Configuration ) = configuration.get[ String ]( "play.cache.redis.configuration" ) match {
    // required static implementation using application.conf
    case "static" => static
    // required environmental implementation
    case "env" => env( connectionStringVariable( configuration ) )
    // required heroku configuration
    case "heroku" => heroku
    // required heroku configuration
    case "heroku-cloud" => herokuCloud
    // supplied custom implementation
    case "custom" => custom
    // found but unrecognized
    case other => invalidConfiguration( s"Unrecognized configuration provider '$other' in key 'play.cache.redis.configuration'. Expected values are 'custom', 'static', 'heroku', 'heroku-cloud', and 'env'." )
  }

  /** returns name of the variable with the connection string */
  private def connectionStringVariable( configuration: Configuration ) =
    configuration.get[ String ]( "play.cache.redis.connection-string-variable" )

  /**
    * creates the configuration provider based on the resolver implementation
    *
    * Note: required static implementation using application.conf
    */
  protected def static: T

  /**
    * creates the configuration provider based on the resolver implementation
    *
    * Note: required environmental implementation
    */
  protected def env( connectionString: String ): T

  /**
    * creates the configuration provider based on the resolver implementation
    *
    * Note: required heroku configuration
    */
  protected def heroku: T

  /**
    * creates the configuration provider based on the resolver implementation
    *
    * Note: required heroku configuration
    */
  protected def herokuCloud: T

  /**
    * creates the configuration provider based on the resolver implementation
    *
    * Note: supplied custom implementation
    */
  protected def custom: T
}


/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
object ConfigurationModule extends Module {

  private class RedisConfigurationResolver( implicit configuration: Configuration ) extends ConfigurationResolver[ Option[ Binding[ RedisConfiguration ] ] ] {
    protected def static = Some( bind[ RedisConfiguration ].to[ ConfigurationFile ] )
    protected def env( connectionString: String ) = Some( bind[ RedisConfiguration ].to( new ConnectionStringProvider( connectionString ) ) )
    protected def heroku = Some( bind[ RedisConfiguration ].to( new ConnectionStringProvider( "REDIS_URL" ) ) )
    protected def herokuCloud = Some( bind[ RedisConfiguration ].to( new ConnectionStringProvider( "REDISCLOUD_URL" ) ) )
    protected def custom = None // ignore, supplied custom configuration provider
  }

  override def bindings( environment: Environment, configuration: Configuration ) =
    new RedisConfigurationResolver()( configuration ).resolve( configuration ).toSeq
}


/**
  * Components for compile-time dependency injection.
  * It binds components from configuration package
  *
  * @author Karel Cemus
  */
private[ redis ] trait ConfigurationComponents {

  def configuration: Configuration

  private object RedisConfigurationResolver extends ConfigurationResolver[ RedisConfiguration ] {
    implicit def config = configuration
    protected def static = new ConfigurationFile()
    protected def env( connectionString: String ) = new ConnectionStringProvider( connectionString ).get()
    protected def heroku = new ConnectionStringProvider( "REDIS_URL" ).get()
    protected def herokuCloud = new ConnectionStringProvider( "REDISCLOUD_URL" ).get()
    protected def custom = customRedisConfiguration
  }

  protected def customRedisConfiguration: RedisConfiguration =
    shouldBeOverwritten( "In order to use custom RedisConfiguration overwrite this method." )

  lazy val redisConfiguration = RedisConfigurationResolver.resolve( configuration )
}
