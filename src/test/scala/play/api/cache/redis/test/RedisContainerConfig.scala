package play.api.cache.redis.test

final case class RedisContainerConfig(
  redisDockerImage: String,
  redisMappedPorts: Seq[Int],
  redisFixedPorts: Seq[Int],
  redisEnvironment: Map[String, String],
)
