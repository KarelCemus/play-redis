package play.api.cache.redis

import javax.inject.{Provider, Singleton}

import scala.concurrent.duration.FiniteDuration

/**
 * Configuration provider returns a connection settings to Redis server. The application can run in various
 * different environments and in many of them there might be very different connection settings. For example,
 * for running the application on localhost is sufficient to set up configuration into the configuration file.
 * Although for deploying the application into some PaaS it might be injected directly by the platform itself,
 * e.g., via environment variable. This trait abstracts the configuration and allows its multiple implementations
 * for every required environment.
 *
 * @author Karel Cemus
 */
trait Configuration {

  /** the name of the invocation context executing all commands to Redis */
  def invocationContext: String

  /** timeout of cache commands */
  def timeout: FiniteDuration

  /** host with redis server */
  def host: String

  /** port redis listens on */
  def port: Int

  /** Redis database identifier to work with */
  def database: Int

  /** When enabled security, this returns password for the AUTH command */
  def password: Option[ String ]
}

/**
 * This configuration source reads the static configuration in the `application.conf` file and provides settings
 * located in there. This is default configuration provider. It expects all settings under the 'play.cache.redis' node.
 */
@Singleton
class StaticConfiguration extends Configuration {

  import scala.language.implicitConversions

  /** converts java.time.Duration into scala.concurrent.duration.Duration */
  protected implicit def asScalaDuration( duration: java.time.Duration ): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos( duration.toNanos )

  /** cache configuration root */
  protected def config = com.typesafe.config.ConfigFactory.load( ).getConfig( "play.cache.redis" )

  /** the name of the invocation context executing all commands to Redis */
  def invocationContext = config.getString( "dispatcher" )

  /** timeout of cache commands */
  def timeout = config.getDuration( "timeout" )

  /** host with redis server */
  def host = config.getString( "host" )

  /** port redis listens on */
  def port = config.getInt( "port" )

  /** Redis database identifier to work with */
  def database = config.getInt( "database" )

  /** When enabled security, this returns password for the AUTH command */
  override def password: Option[ String ] = if ( config.getIsNull( "password" ) ) None else Some( config.getString( "password" ) )
}

/**
 * Environment configuration expects the configuration to be injected through environment variable containing 
 * the connection string. This configuration is often used by PaaS environments.
 */
@Singleton
class EnvironmentConfiguration(

  /** host with redis server */
  override val host: String,

  /** port redis listens on */
  override val port: Int,

  /** authentication password */
  override val password: Option[ String ]

) extends StaticConfiguration

/**
 * Reads environment variables for the connection string and returns EnvironmentConfiguration instance.
 * This configuration instance is designed to work in PaaS environments such as Heroku.
 *
 * @param variable name of the variable with the connection string in the environment
 */
class EnvironmentConfigurationProvider( variable: String ) extends Provider[ EnvironmentConfiguration ] {

  /** expected format of the environment variable */
  private val REDIS_URL = "redis://((?<user>[^:]+):(?<password>[^@]+)@)?(?<host>[^:]+):(?<port>[0-9]+)".r( "auth", "user", "password", "host", "port" )

  /** read environment url or throw an exception */
  override def get( ): EnvironmentConfiguration = url.map( REDIS_URL.findFirstMatchIn ) match {
    // read the environment variable and fill missing information from the local configuration file
    case Some( Some( matcher ) ) => new EnvironmentConfiguration( matcher.group( "host" ), matcher.group( "port" ).toInt, Option( matcher.group( "password" ) ) )
    // value is defined but not in the expected format
    case Some( None ) => throw new IllegalArgumentException( s"Unexpected value in the environment variable '$variable'. Expected format is 'redis://[user:password@]host:port'." )
    // variable is missing
    case None => throw new IllegalArgumentException( s"Expected environment variable '$variable' is missing. Expected value is 'redis://[user:password@]host:port'." )
  }

  /** returns the connection url to redis server */
  protected def url = sys.env.get( variable )
}
