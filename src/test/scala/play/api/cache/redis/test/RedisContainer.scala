package play.api.cache.redis.test

import com.dimafeng.testcontainers.GenericContainer
import org.scalatest.Suite
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import scala.annotation.nowarn

trait RedisContainer extends ForAllTestContainer { this: Suite =>

  protected def redisConfig: RedisContainerConfig

  private lazy val config = redisConfig

  @nowarn("cat=deprecation")
  @SuppressWarnings(Array("org.wartremover.warts.ForeachEntry"))
  final override protected val newContainer: GenericContainer = {
    val container: FixedHostPortGenericContainer[?] = new FixedHostPortGenericContainer(config.redisDockerImage)
    container.withExposedPorts(config.redisMappedPorts.map(int2Integer): _*)
    config.redisEnvironment.foreach { case (k, v) => container.withEnv(k, v) }
    container.waitingFor(Wait.forListeningPorts(config.redisMappedPorts ++ config.redisFixedPorts: _*))
    config.redisFixedPorts.foreach(port => container.withFixedExposedPort(port, port))
    new GenericContainer(container)
  }

}
