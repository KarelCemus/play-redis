package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.testcontainers.containers.wait.strategy.Wait

object MasterSlaveRedis {

  val host: String = ComposeRedis.host
  private val redisPort = 6379

  private val ports = FreePorts.range(2)
  val masterPort: Int = ports
  val slavePort: Int = ports + 1

  lazy val container: DockerComposeContainer =
    ComposeRedis.start(
      "master-slave",
      Map(
        "REDIS_MASTER_PORT" -> masterPort.toString,
        "REDIS_SLAVE_PORT"  -> slavePort.toString,
      ),
      ExposedService("redis-master", redisPort, Wait.forLogMessage(".*Ready to accept connections tcp.*\\n", 1)),
      ExposedService("redis-slave", redisPort, Wait.forLogMessage(".*MASTER <-> REPLICA sync: Finished with success.*\\n", 1)),
    )

}
