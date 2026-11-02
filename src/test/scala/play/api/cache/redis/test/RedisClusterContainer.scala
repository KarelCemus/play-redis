package play.api.cache.redis.test

import com.dimafeng.testcontainers.DockerComposeContainer
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.Logger

trait RedisClusterContainer extends BeforeAndAfterAll { this: Suite =>

  protected val log: Logger = Logger("play.api.cache.redis.test")

  final protected def host: String = ClusterRedis.host
  final protected def redisMaster: Int = ClusterRedis.masters
  final protected def initialPort: Int = ClusterRedis.initialPort

  protected def container: DockerComposeContainer = ClusterRedis.container

  override protected def beforeAll(): Unit = {
    container
    super.beforeAll()
  }

}
