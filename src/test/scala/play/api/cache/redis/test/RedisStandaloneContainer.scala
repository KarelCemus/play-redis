package play.api.cache.redis.test

import com.dimafeng.testcontainers.DockerComposeContainer
import org.scalatest.{BeforeAndAfterAll, Suite}

trait RedisStandaloneContainer extends BeforeAndAfterAll { this: Suite =>

  final protected def host: String = StandaloneRedis.host
  final protected def port: Int = StandaloneRedis.port

  protected def container: DockerComposeContainer = StandaloneRedis.container

  /**
    * The database exclusive to this suite, so suites sharing the container can
    * run in parallel.
    */
  protected lazy val database: Int = StandaloneRedis.newDatabase()

  override protected def beforeAll(): Unit = {
    container
    super.beforeAll()
  }

}
