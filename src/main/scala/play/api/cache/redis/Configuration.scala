package play.api.cache.redis

import javax.inject.Singleton

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
class HerokuConfiguration

