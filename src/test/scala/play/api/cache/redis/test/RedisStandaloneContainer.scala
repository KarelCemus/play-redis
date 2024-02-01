package play.api.cache.redis.test

import org.scalatest.Suite

trait RedisStandaloneContainer extends RedisContainer { this: Suite =>

  protected lazy val defaultPort: Int = 6379

  override protected lazy val redisConfig: RedisContainerConfig =
    RedisContainerConfig(
      redisDockerImage = "redis:latest",
      redisMappedPorts = Seq(defaultPort),
      redisFixedPorts = Seq.empty,
      redisEnvironment = Map.empty
    )
}
