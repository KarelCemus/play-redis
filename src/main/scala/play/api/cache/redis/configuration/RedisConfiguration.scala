package play.api.cache.redis.configuration

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
trait RedisConfiguration {

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

  /** When enabled security, this returns password for the AUTH command */
  def cluster: List[ ClusterHost ]
}

/** Configures a host within a cluster */
case class ClusterHost( host: String, port: Int, password: Option[ String ], database: Option[ Int ] )
