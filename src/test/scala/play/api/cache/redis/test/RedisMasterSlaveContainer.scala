package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.scalatest.Suite
import org.testcontainers.containers.wait.strategy.Wait

import java.io.File

trait RedisMasterSlaveContainer extends ForAllTestContainer {
  this: Suite =>

  override protected type TestContainer = DockerComposeContainer

  final protected val host = "localhost"
  final protected val masterPort = 6379
  final protected val slavePort = 6479

  protected def newContainer: TestContainerDef[TestContainer] =
    DockerComposeContainer.Def(
      new File("src/test/resources/docker-compose.yml"),
      tailChildContainers = true,
      env = Map(
        "REDIS_MASTER_PORT" -> s"$masterPort",
        "REDIS_SLAVE_PORT"  -> s"$slavePort",
      ),
      exposedServices = Seq(
        ExposedService("redis-master", masterPort, Wait.forLogMessage(".*Ready to accept connections tcp.*\\n", 1)),
        ExposedService("redis-slave", slavePort, Wait.forLogMessage(".*MASTER <-> REPLICA sync: Finished with success.*\\n", 1)),
      ),
    )

}
