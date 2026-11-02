package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.testcontainers.containers.wait.strategy.Wait

object ClusterRedis {

  val host: String = ComposeRedis.host

  // keep in sync with the compose file
  val masters: Int = 4
  private val nodes = masters * 2

  val initialPort: Int = FreePorts.range(nodes)

  lazy val container: DockerComposeContainer =
    ComposeRedis.start(
      "cluster",
      Map(
        "CLUSTER_FIRST_PORT" -> initialPort.toString,
        "CLUSTER_LAST_PORT"  -> (initialPort + nodes - 1).toString,
      ),
      ExposedService("redis-cluster", initialPort, Wait.forLogMessage(".*Cluster is ready.*\\n", 1)),
    )

}
