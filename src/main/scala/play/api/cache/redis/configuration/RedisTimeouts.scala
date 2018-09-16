package play.api.cache.redis.configuration

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import play.api.Logger

import com.typesafe.config.Config

/**
  * Aggregates the timeout configuration settings
  */
trait RedisTimeouts {

  /** sync timeout applies in sync API and indicates how long to wait before the future is resolved */
  def sync: FiniteDuration

  /** redis timeout indicates how long to wait for the response */
  def redis: Option[FiniteDuration]

  /** connection timeout applies when the connection is not established to fail requests eagerly */
  def connection: Option[FiniteDuration]
}

case class RedisTimeoutsImpl(
  /** sync timeout applies in sync API and indicates how long to wait before the future is resolved */
  sync: FiniteDuration,

  /** redis timeout indicates how long to wait for the response */
  redis: Option[FiniteDuration],

  /** fail after timeout applies when the connection is not established to fail requests eagerly */
  connection: Option[FiniteDuration]

) extends RedisTimeouts {

  // $COVERAGE-OFF$
  override def equals(obj: scala.Any) = obj match {
    case that: RedisTimeouts => this.sync == that.sync && this.redis == that.redis && this.connection == that.connection
    case _                   => false
  }
  // $COVERAGE-ON$
}

object RedisTimeouts {
  import RedisConfigLoader._

  def requiredDefault: RedisTimeouts = new RedisTimeouts {
    def sync = required("sync-timeout")
    def redis = None
    def connection = None
  }

  @inline
  def apply(sync: FiniteDuration, redis: Option[FiniteDuration] = None, connection: Option[FiniteDuration] = None): RedisTimeouts =
    RedisTimeoutsImpl(sync, redis, connection)

  def load(config: Config, path: String)(default: RedisTimeouts) = RedisTimeouts(
    sync = loadSyncTimeout(config, path) getOrElse default.sync,
    redis = loadRedisTimeout(config, path) getOrElse default.redis,
    connection = loadConnectionTimeout(config, path) getOrElse default.connection
  )

  private def loadSyncTimeout(config: Config, path: String): Option[FiniteDuration] = {
    config.getOption(path / "sync-timeout", _.getDuration).map(duration => FiniteDuration(duration.getSeconds, TimeUnit.SECONDS))
  }

  private def loadRedisTimeout(config: Config, path: String): Option[Option[FiniteDuration]] = {
    config.getNullable(path / "redis-timeout", _.getDuration).map {
      _.map(duration => FiniteDuration(duration.getSeconds, TimeUnit.SECONDS))
    }
  }

  private def loadConnectionTimeout(config: Config, path: String): Option[Option[FiniteDuration]] = {
    config.getNullable(path / "connection-timeout", _.getDuration).map {
      _.map(duration => FiniteDuration(duration.getSeconds, TimeUnit.SECONDS) + FiniteDuration(duration.getNano, TimeUnit.NANOSECONDS))
    }
  }
}
