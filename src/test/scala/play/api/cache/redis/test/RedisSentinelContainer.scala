package play.api.cache.redis.test

import com.dimafeng.testcontainers.DockerComposeContainer
import org.scalatest.{BeforeAndAfterAll, Suite}

trait RedisSentinelContainer extends BeforeAndAfterAll { this: Suite =>

  final protected def host: String = SentinelRedis.host
  final protected def master: String = SentinelRedis.master
  final protected def sentinels: Int = SentinelRedis.sentinels
  final protected def masterPort: Int = SentinelRedis.masterPort
  final protected def slavePort: Int = SentinelRedis.slavePort
  final protected def sentinelPort: Int = SentinelRedis.sentinelPort

  protected def container: DockerComposeContainer = SentinelRedis.container

  override protected def beforeAll(): Unit = {
    container
    super.beforeAll()
  }

}
