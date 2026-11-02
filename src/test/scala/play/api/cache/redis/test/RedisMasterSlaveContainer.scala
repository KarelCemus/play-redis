package play.api.cache.redis.test

import com.dimafeng.testcontainers.DockerComposeContainer
import org.scalatest.{BeforeAndAfterAll, Suite}

trait RedisMasterSlaveContainer extends BeforeAndAfterAll { this: Suite =>

  final protected def host: String = MasterSlaveRedis.host
  final protected def masterPort: Int = MasterSlaveRedis.masterPort
  final protected def slavePort: Int = MasterSlaveRedis.slavePort

  protected def container: DockerComposeContainer = MasterSlaveRedis.container

  override protected def beforeAll(): Unit = {
    container
    super.beforeAll()
  }

}
