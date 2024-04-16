package play.api.cache.redis.test

import org.scalatest.Suite
import play.api.Logger

import scala.concurrent.duration._

trait RedisClusterContainer extends RedisContainer { this: Suite =>

  protected val log: Logger = Logger("play.api.cache.redis.test")

  protected def redisMaster = 4

  protected def redisSlaves = 1

  final protected def initialPort = 9001

  private val waitForStart = 6.seconds

  override protected lazy val redisConfig: RedisContainerConfig =
    RedisContainerConfig(
      redisDockerImage = "grokzen/redis-cluster:7.0.10",
      redisMappedPorts = Seq.empty,
      redisFixedPorts = 0.until(redisMaster * (redisSlaves + 1)).map(initialPort + _),
      redisEnvironment = Map(
        "IP"                -> "0.0.0.0",
        "INITIAL_PORT"      -> initialPort.toString,
        "MASTERS"           -> redisMaster.toString,
        "SLAVES_PER_MASTER" -> redisSlaves.toString,
      ),
    )

  @SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
  override def beforeAll(): Unit = {
    super.beforeAll()
    log.info(s"Waiting for Redis Cluster to start on ${container.containerIpAddress}, will wait for $waitForStart")
    Thread.sleep(waitForStart.toMillis)
    log.info(s"Finished waiting for Redis Cluster to start on ${container.containerIpAddress}")
  }

}
