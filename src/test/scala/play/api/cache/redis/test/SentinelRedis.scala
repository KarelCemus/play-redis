package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.testcontainers.containers.wait.strategy.Wait

object SentinelRedis {

  val host: String = ComposeRedis.host
  val master = "mymaster"
  val sentinels: Int = 3
  private val redisPort = 6379

  private val ports = FreePorts.range(2 + sentinels)
  val masterPort: Int = ports
  val slavePort: Int = ports + 1
  val sentinelPort: Int = ports + 2

  // note: service names must not end with a digit, testcontainers does not find them
  private val sentinelInternalPort = 26379
  private val sentinelReady = Wait.forLogMessage(".*\\+sentinel sentinel.*\\n", sentinels - 1)

  lazy val container: DockerComposeContainer =
    ComposeRedis.start(
      "sentinel",
      Map(
        "REDIS_MASTER_PORT"     -> masterPort.toString,
        "REDIS_SLAVE_PORT"      -> slavePort.toString,
        "REDIS_SENTINEL_1_PORT" -> (sentinelPort + 0).toString,
        "REDIS_SENTINEL_2_PORT" -> (sentinelPort + 1).toString,
        "REDIS_SENTINEL_3_PORT" -> (sentinelPort + 2).toString,
      ),
      ExposedService("redis-master", redisPort, Wait.forLogMessage(".*Ready to accept connections tcp.*\\n", 1)),
      ExposedService("redis-slave", redisPort, Wait.forLogMessage(".*MASTER <-> REPLICA sync: Finished with success.*\\n", 1)),
      // a sentinel is ready once it has discovered the other sentinels
      ExposedService("redis-sentinel-a", sentinelInternalPort, sentinelReady),
      ExposedService("redis-sentinel-b", sentinelInternalPort, sentinelReady),
      ExposedService("redis-sentinel-c", sentinelInternalPort, sentinelReady),
    )

}
