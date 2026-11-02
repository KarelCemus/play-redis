package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}

import java.io.File

object ComposeRedis {

  val host: String = "localhost"

  /** Starts the compose project from `docker/<name>`; it is stopped when the JVM exits. */
  def start(name: String, env: Map[String, String], services: ExposedService*): DockerComposeContainer = {
    val compose = DockerComposeContainer
      .Def(new File(s"docker/$name/docker-compose.yml"), env = env, exposedServices = services)
      .start()
    sys.addShutdownHook(compose.stop())
    compose
  }

}
