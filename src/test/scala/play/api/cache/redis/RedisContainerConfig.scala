package play.api.cache.redis

final case class RedisContainerConfig(
  redisDockerImage: String,
  redisPorts: Seq[Int],
  redisEnvironment: Map[String, String],
)
