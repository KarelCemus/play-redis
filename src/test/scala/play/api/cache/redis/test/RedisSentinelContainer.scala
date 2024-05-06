package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.scalatest.Suite
import org.testcontainers.containers.wait.strategy.Wait

import java.io.File

trait RedisSentinelContainer extends ForAllTestContainer {
  this: Suite =>

  override protected type TestContainer = DockerComposeContainer

  protected def master: String = "mymaster"

  final protected val host = "localhost"

  final private val initialPort = 7000

  final protected val masterPort = initialPort
  final protected val slavePort = masterPort + 1

  protected def sentinels: Int = 3

  final protected def sentinelPort: Int = initialPort - 2000

  // note: waiting for sentinels is not working
  // private def sentinelWaitStrategy = new WaitAllStrategy()
  //   .withStrategy(Wait.forLogMessage(".*\\+monitor master.*\\n", 1))
  //   .withStrategy(Wait.forLogMessage(".*\\+slave slave.*\\n", 1))

  protected def newContainer: TestContainerDef[TestContainer] =
    DockerComposeContainer.Def(
      new File("docker/sentinel/docker-compose.yml"),
      tailChildContainers = true,
      env = Map(
        "REDIS_MASTER_PORT"     -> s"$masterPort",
        "REDIS_SLAVE_PORT"      -> s"$slavePort",
        "REDIS_SENTINEL_1_PORT" -> s"${sentinelPort + 0}",
        "REDIS_SENTINEL_2_PORT" -> s"${sentinelPort + 1}",
        "REDIS_SENTINEL_3_PORT" -> s"${sentinelPort + 2}",
      ),
      exposedServices = Seq(
        ExposedService("redis-master", masterPort, Wait.forLogMessage(".*Ready to accept connections tcp.*\\n", 1)),
        ExposedService("redis-slave", slavePort, Wait.forLogMessage(".*MASTER <-> REPLICA sync: Finished with success.*\\n", 1)),
        // note: waiting for sentinels doesn't work, it says "service is not running"
        // ExposedService("redis-sentinel-1", sentinelPort + 0, sentinelWaitStrategy),
        // ExposedService("redis-sentinel-2", sentinelPort + 1, sentinelWaitStrategy),
        // ExposedService("redis-sentinel-3", sentinelPort + 2, sentinelWaitStrategy),
      ),
    )

}
