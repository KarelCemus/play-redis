package play.api.cache.redis

trait ClusterRedisContainer extends RedisContainer {

  protected def redisMaster = 4

  protected def redisSlaves = 1

  override protected lazy val redisConfig: RedisContainerConfig =
    RedisContainerConfig(
      "grokzen/redis-cluster:latest",
      0.until(redisMaster * (redisSlaves + 1)).map(7000 + _),
      Map(
        "IP" -> "0.0.0.0",
        "INITIAL_PORT" -> "7000",
        "MASTERS" -> s"$redisMaster",
        "SLAVES_PER_MASTER" -> s"$redisSlaves",
      ),
    )
}
