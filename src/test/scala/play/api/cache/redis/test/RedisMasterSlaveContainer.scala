package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService, SingleContainer}
import org.scalatest.Suite
import org.testcontainers.containers.wait.strategy.Wait

import java.io.File

trait RedisMasterSlaveContainer extends ForAllTestContainer {
  this: Suite =>

  protected val host = "localhost"
  protected val masterPort = 6379
  protected val slavePort = 6479

  private val composeContainerDef = DockerComposeContainer.Def(
    new File("src/test/resources/docker-compose.yml"),
    tailChildContainers = true,
    exposedServices = Seq(
      ExposedService("redis-master", masterPort, Wait.forLogMessage(".*Ready to accept connections tcp.*\\n", 1)),
      ExposedService("redis-slave", slavePort, Wait.forLogMessage(".*MASTER <-> REPLICA sync: Finished with success.*\\n", 1)),
    ),
  )

  private lazy val dockerComposeContainer: DockerComposeContainer = composeContainerDef.createContainer()

  protected def newContainer: SingleContainer[?] =
    throw new UnsupportedOperationException("Creating a single container is not supported.")

  override def beforeAll(): Unit = dockerComposeContainer.start()

  override def afterAll(): Unit = dockerComposeContainer.stop()
}
