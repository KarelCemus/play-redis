package play.api.cache.redis.test

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService, ServiceLogConsumer}
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.wait.strategy.{Wait, WaitStrategy}

import java.io.File
import java.time.Duration
import java.util.function.Consumer
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object ComposeRedis {

  val host: String = "localhost"

  private val startupTimeout: Duration = Duration.ofSeconds(45)

  private val attempts: Int = 3

  def waitForLog(regex: String): WaitStrategy =
    Wait.forLogMessage(regex, 1).withStartupTimeout(startupTimeout)

  /**
    * Starts the compose project from `docker/<name>`; it is stopped when the
    * JVM exits. A project not ready within the startup timeout is torn down and
    * created anew, as a Redis cluster occasionally fails to form on busy CI
    * runners.
    */
  def start(name: String, env: Map[String, String], services: ExposedService*): DockerComposeContainer = {
    // container output makes a failed startup diagnosable from the CI log
    val logs = services.map { service =>
      val consumer: Consumer[OutputFrame] = frame => print(s"[${service.name}] ${frame.getUtf8String}")
      ServiceLogConsumer(service.name, consumer)
    }
    val definition =
      DockerComposeContainer.Def(new File(s"docker/$name/docker-compose.yml"), env = env, exposedServices = services, logConsumers = logs)

    @tailrec
    def attempt(number: Int): DockerComposeContainer = {
      val compose = definition.createContainer()
      Try(compose.start()) match {
        case Success(_)     => compose
        case Failure(cause) =>
          Try(compose.stop())
          if (number >= attempts) throw cause
          println(s"Compose '$name' did not start (attempt $number of $attempts): ${cause.getMessage}")
          attempt(number + 1)
      }
    }

    val compose = attempt(1)
    sys.addShutdownHook(compose.stop())
    compose
  }

}
