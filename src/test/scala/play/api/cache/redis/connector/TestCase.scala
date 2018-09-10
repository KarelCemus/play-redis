package play.api.cache.redis.connector

import java.util.concurrent.atomic.AtomicInteger

import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.{Around, Scope}

abstract class TestCase extends Around with Scope {

  protected val idx = TestCase.last.incrementAndGet()

  override def around[T: AsResult](t: => T): Result = {
    AsResult.effectively(t)
  }
}

object TestCase {

  val last = new AtomicInteger()
}
