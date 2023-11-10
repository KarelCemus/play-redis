package play.api.cache.redis

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

trait RedisContainer extends ForAllTestContainer {

  protected def redisConfig: RedisContainerConfig

  private lazy val config = redisConfig

  override val newContainer = GenericContainer(
    dockerImage = config.redisDockerImage,
    exposedPorts = config.redisPorts,
    env = config.redisEnvironment,
    waitStrategy = Wait.forListeningPorts(config.redisPorts: _*),
  )
}

