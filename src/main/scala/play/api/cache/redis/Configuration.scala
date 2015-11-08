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
}

/**
 * This configuration source reads the application configuration file and provides settings located in there. This
 * is default configuration provider and it expects all settings under the 'play.cache.redis' node.
 */
@Singleton
class LocalConfiguration extends Configuration {

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
}

@Singleton
class HerokuConfiguration(

  /** host with redis server */
  override val host: String,

  /** port redis listens on */
  override val port: Int

) extends LocalConfiguration

/**
 * Reads environment variables for REDIS_URL and returns HerokuConfiguration instance.
 * This configuration instance is designed to work in Heroku PaaS environment.
 */
trait HerokuConfigurationProvider extends Provider[ HerokuConfiguration ] {

  /** expected format of the environment variable */
  private val REDIS_URL = "redis://([^:]+):([^@]+)@([^:]+):([0-9]+)".r

  /** read environment url or throw an exception */
  override def get( ): HerokuConfiguration = url match {
    case Some( REDIS_URL( user, password, host, port ) ) =>
      // read the environment variable and fill missing information from the local configuration file
      new HerokuConfiguration( host, port.toInt )
    case Some( _ ) =>
      // value is defined but not in the expected format
      throw new IllegalArgumentException( "Unexpected value in the environment variable 'REDIS_URL'. Expected format is 'redis://user:password@host:port'." )
    case None =>
      // variable is missing
      throw new IllegalArgumentException( "Expected environment variable 'REDIS_URL' at Heroku PaaS is missing. Expected value is 'redis://user:password@host:port'." )
  }

  /** returns the connection url to redis server */
  protected def url = sys.env.get( "REDIS_URL" )
}

object HerokuConfigurationProvider extends HerokuConfigurationProvider
