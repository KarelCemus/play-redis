package play.api.cache.redis

import com.dimafeng.testcontainers.SingleContainer
import org.specs2.specification.BeforeAfterAll

trait ForAllTestContainer extends BeforeAfterAll {

  def newContainer: SingleContainer[_]

  final protected lazy val container = newContainer

  override def beforeAll(): Unit = {
    container.start()
  }

  override def afterAll(): Unit = {
    container.stop()
  }
}
