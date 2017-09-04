package play.api.cache.redis.configuration

import javax.inject.{Inject, Singleton}

import scala.concurrent.duration.FiniteDuration

import play.api.Configuration

/**
  * This configuration source reads the static configuration in the `application.conf` file and provides settings
  * located in there. This is default configuration provider. It expects all settings under the 'play.cache.redis' node.
  */
@Singleton
private[ redis ] class ConfigurationFile @Inject()( implicit configuration: Configuration ) extends RedisConfiguration {

  import scala.language.implicitConversions

  /** converts java.time.Duration into scala.concurrent.duration.Duration */
  protected implicit def asScalaDuration( duration: java.time.Duration ): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos( duration.toNanos )

  /** cache configuration root */
  protected def config = configuration.get[ Configuration ]( "play.cache.redis" )

  /** the name of the invocation context executing all commands to Redis */
  def invocationContext = config.get[ String ]( "dispatcher" )

  /** timeout of cache commands */
  def timeout = config.get[ FiniteDuration ]( "timeout" )

  /** host with redis server */
  def host = config.get[ String ]( "host" )

  /** port redis listens on */
  def port = config.get[ Int ]( "port" )

  /** Redis database identifier to work with */
  def database = config.get[ Int ]( "database" )

  /** When enabled security, this returns password for the AUTH command */
  def cluster = config.get[ Seq[ Configuration ] ]( "cluster" ).map {
    config =>
      ClusterHost(
        host = config.get[ String ]( "host" ),
        port = config.get[ Int ]( "port" ),
        password = if ( config.has( "password" ) ) config.getOptional[ String ]( "password" ).filterNot( _.trim == "" ) else None
      )
  }.toList

  /** When enabled security, this returns password for the AUTH command */
  override def password: Option[ String ] = if ( config.has( "password" ) ) config.getOptional[ String ]( "password" ).filterNot( _.trim == "" ) else None
}
