package play.api.cache.redis.configuration

import javax.inject.Singleton

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import play.api.cache.redis.util.config._

/**
  * This configuration source reads the static configuration in the `application.conf` file and provides settings
  * located in there. This is default configuration provider. It expects all settings under the 'play.cache.redis' node.
  */
@Singleton
private[ redis ] class ConfigurationFile extends RedisConfiguration {

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
  def cluster = config.getConfigList( "cluster" ).asScala.map {
    config =>
      ClusterHost(
        host = config.getString( "host" ),
        port = config.getInt( "port" ),
        password = config.getStringOpt("password")
      )
  }.toList

  /** When enabled security, this returns password for the AUTH command */
  override def password: Option[ String ] = if ( config.getIsNull( "password" ) ) None else Some( config.getString( "password" ) )
}
