package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.testcontainers.containers.wait.strategy.Wait

object SentinelRedis {

  val host: String = ComposeRedis.host
  val master: String = "mymaster"
  val sentinels: Int = 3

  // ports are consecutive: master, replica, sentinels
  val masterPort: Int = FreePorts.range(2 + sentinels)
  val slavePort: Int = masterPort + 1
  val sentinelPort: Int = masterPort + 2

  lazy val container: DockerComposeContainer =
    ComposeRedis.start(
      "sentinel",
      Map(
        "SENTINEL_FIRST_PORT" -> masterPort.toString,
        "SENTINEL_LAST_PORT"  -> (sentinelPort + sentinels - 1).toString,
      ),
      ExposedService("redis-sentinel", masterPort, Wait.forLogMessage(".*Sentinels are ready.*\\n", 1)),
    )

}
