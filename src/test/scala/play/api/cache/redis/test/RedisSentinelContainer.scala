package play.api.cache.redis.test

import org.scalatest.Suite
import play.api.Logger

import scala.concurrent.duration.DurationInt

trait RedisSentinelContainer extends RedisContainer {
  this: Suite =>

  private val log = Logger("play.api.cache.redis.test")

  protected def nodes: Int = 3

  final protected def initialPort: Int = 7000

  final protected def sentinelPort: Int = initialPort - 2000

  protected def master: String = s"sentinel$initialPort"

  private val waitForStart = 7.seconds

  override protected lazy val redisConfig: RedisContainerConfig =
    RedisContainerConfig(
      redisDockerImage = "grokzen/redis-cluster:7.0.10",
      redisMappedPorts = Seq.empty,
      redisFixedPorts = 0.until(nodes).flatMap(i => Seq(initialPort + i, sentinelPort + i)),
      redisEnvironment = Map(
        "IP"           -> "0.0.0.0",
        "INITIAL_PORT" -> initialPort.toString,
        "SENTINEL"     -> "true",
      ),
    )

  @SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
  override def beforeAll(): Unit = {
    super.beforeAll()
    log.info(s"Waiting for Redis Sentinel to start on ${container.containerIpAddress}, will wait for $waitForStart")
    Thread.sleep(waitForStart.toMillis)
    log.info(s"Finished waiting for Redis Sentinel to start on ${container.containerIpAddress}")
  }

}
