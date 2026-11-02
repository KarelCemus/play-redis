package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.testcontainers.containers.wait.strategy.{Wait, WaitStrategy}

import java.io.File
import java.time.Duration

object ComposeRedis {

  val host: String = "localhost"

  /**
    * The default 60s is too tight for shared CI runners running suites in
    * parallel.
    */
  def waitForLog(regex: String): WaitStrategy =
    Wait.forLogMessage(regex, 1).withStartupTimeout(Duration.ofMinutes(2))

  /**
    * Starts the compose project from `docker/<name>`; it is stopped when the
    * JVM exits.
    */
  def start(name: String, env: Map[String, String], services: ExposedService*): DockerComposeContainer = {
    val compose = DockerComposeContainer
      .Def(new File(s"docker/$name/docker-compose.yml"), env = env, exposedServices = services)
      .start()
    sys.addShutdownHook(compose.stop())
    compose
  }

}
