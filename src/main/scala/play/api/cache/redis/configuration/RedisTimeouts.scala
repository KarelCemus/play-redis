package play.api.cache.redis.configuration

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import play.api.Logger

import com.typesafe.config.Config

/**
  * Aggregates the timeout configuration settings
  *
  * @author Karel Cemus
  */
trait RedisTimeouts {

  /** sync timeout applies in sync API and indicates how long to wait before the future is resolved */
  def sync: FiniteDuration

  /** redis timeout indicates how long to wait for the response */
  def redis: Option[ FiniteDuration ]
}

case class RedisTimeoutsImpl
(
  /** sync timeout applies in sync API and indicates how long to wait before the future is resolved */
  sync: FiniteDuration,

  /** redis timeout indicates how long to wait for the response */
  redis: Option[ FiniteDuration ]

) extends RedisTimeouts {

  // $COVERAGE-OFF$
  override def equals( obj: scala.Any ) = obj match {
    case that: RedisTimeouts => this.sync == that.sync && this.redis == that.redis
    case _ => false
  }
  // $COVERAGE-ON$
}


object RedisTimeouts {
  import RedisConfigLoader._

  private def log = Logger( "play.api.cache.redis" )

  def requiredDefault: RedisTimeouts = new RedisTimeouts {
    def sync = required( "sync-timeout" )
    def redis = None
  }

  @inline
  def apply( sync: FiniteDuration, redis: Option[ FiniteDuration ] = None ): RedisTimeouts =
    RedisTimeoutsImpl( sync, redis )

  def load( config: Config, path: String )( default: RedisTimeouts ) = RedisTimeouts(
    sync = loadSyncTimeout( config, path ) getOrElse default.sync,
    redis = loadRedisTimeout( config, path ) orElse default.redis
  )

  @scala.deprecated( "Property 'timeout' was deprecated in 2.1.0 and was replaced by the 'sync-timeout' with the identical use and meaning.", since = "2.1.0" )
  private def loadTimeout( config: Config, path: String ): Option[ FiniteDuration ] =
    config.getOption( path / "timeout", _.getDuration ).map { duration =>
      log.warn(
        """
          |Deprecated settings: Property 'timeout' was deprecated in 2.1.0 and was replaced
          |by the 'sync-timeout' with the identical use and meaning. The property, the fallback,
          |and this warning will be removed in 2.2.0.
        """.stripMargin.replace( "\n", " " )
      )
      FiniteDuration( duration.getSeconds, TimeUnit.SECONDS )
    }

  private def loadSyncTimeout( config: Config, path: String ): Option[ FiniteDuration ] =
    loadTimeout( config, path ) orElse {
      config.getOption( path / "sync-timeout", _.getDuration ).map( duration => FiniteDuration( duration.getSeconds, TimeUnit.SECONDS ) )
    }

  private def loadRedisTimeout( config: Config, path: String ): Option[ FiniteDuration ] =
    config.getOption( path / "redis-timeout", _.getDuration ).map {
      duration => FiniteDuration( duration.getSeconds, TimeUnit.SECONDS )
    }
}
