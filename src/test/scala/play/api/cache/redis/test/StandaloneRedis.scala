package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.testcontainers.containers.wait.strategy.Wait

import java.util.concurrent.atomic.AtomicInteger

/**
  * A single Redis shared by all suites in the JVM. Suites are isolated from
  * each other by using distinct databases.
  */
object StandaloneRedis {

  val host: String = ComposeRedis.host
  val port: Int = FreePorts.one()

  // must match `--databases` in the compose file
  private val databases = 256
  private val nextDatabase = new AtomicInteger(0)

  lazy val container: DockerComposeContainer =
    ComposeRedis.start(
      "standalone",
      Map("REDIS_PORT" -> port.toString),
      ExposedService("redis", 6379, Wait.forLogMessage(".*Ready to accept connections tcp.*\\n", 1)),
    )

  def newDatabase(): Int = {
    val database = nextDatabase.getAndIncrement()
    require(database < databases, s"Out of databases, increase --databases ($databases)")
    database
  }

}
