package play.api.cache.redis

trait StandaloneRedisContainer extends RedisContainer {

  override protected lazy val redisConfig: RedisContainerConfig =
    RedisContainerConfig(
      redisDockerImage = "redis:latest",
      redisPorts = Seq(6379),
      redisEnvironment = Map.empty
    )
}
