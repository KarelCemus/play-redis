package play.api.cache.redis.test

import com.dimafeng.testcontainers.SingleContainer
import org.scalatest.{BeforeAndAfterAll, Suite}

trait ForAllTestContainer extends BeforeAndAfterAll {this: Suite =>

  protected def newContainer: SingleContainer[_]

  final protected lazy val container = newContainer

  override def beforeAll(): Unit = {
    container.start()
  }

  override def afterAll(): Unit = {
    container.stop()
  }
}
