package play.api.cache.redis.test

import com.dimafeng.testcontainers.ContainerDef
import com.dimafeng.testcontainers.lifecycle.Stoppable
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.lifecycle.Startable

trait ForAllTestContainer extends BeforeAndAfterAll { this: Suite =>

  protected type TestContainer <: Startable with Stoppable

  protected type TestContainerDef[C <: Startable with Stoppable] = ContainerDef { type Container = C }

  protected def newContainer: TestContainerDef[TestContainer]

  final protected def container: TestContainer = containerHandle.getOrElse {
    throw new IllegalStateException("Container not yet started")
  }

  final private var containerHandle: Option[TestContainer] = None

  override def beforeAll(): Unit =
    containerHandle = Some(newContainer.start())

  override def afterAll(): Unit =
    container.stop()

}
