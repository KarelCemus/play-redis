package play.api.cache.redis.configuration

import com.typesafe.config.Config
import play.api.cache.redis._

trait RedisThreadPools {
  def ioSize: Int
  def computationSize: Int
}

final case class RedisThreadPoolsImpl(
  ioSize: Int,
  computationSize: Int,
) extends RedisThreadPools {

  // $COVERAGE-OFF$
  override def equals(obj: scala.Any): Boolean = obj match {
    case that: RedisThreadPools => this.ioSize === that.ioSize && this.computationSize === that.computationSize
    case _                      => false
  }
  // $COVERAGE-ON$

}

object RedisThreadPools {
  import RedisConfigLoader._

  def requiredDefault: RedisThreadPools = new RedisThreadPools {
    override def ioSize: Int = 8
    override def computationSize: Int = 8
  }

  @inline
  def apply(ioSize: Int, computationSize: Int): RedisThreadPools =
    RedisThreadPoolsImpl(ioSize, computationSize)

  def load(config: Config, path: String)(default: RedisThreadPools): RedisThreadPools = RedisThreadPools(
    ioSize = loadIoSize(config, path) getOrElse default.ioSize,
    computationSize = loadComputationSize(config, path) getOrElse default.computationSize,
  )

  private def loadIoSize(config: Config, path: String): Option[Int] =
    config.getOption(path / "io-thread-pool-size", _.getInt)

  private def loadComputationSize(config: Config, path: String): Option[Int] =
    config.getOption(path / "computation-thread-pool-size", _.getInt)

}
