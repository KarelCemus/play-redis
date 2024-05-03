package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._

import scala.concurrent._
import scala.util.Success

class InvocationPolicySpec extends UnitSpec {

  implicit private val ec: ExecutionContext = scala.concurrent.ExecutionContext.parasitic

  private class Probe {
    private val promise = Promise[Unit]()
    def resolve(): Unit = promise.success(())
    def run(): Future[Unit] = promise.future
  }

  "invoke lazily, i.e., slowly" in {
    val probe = new Probe
    val outcome = LazyInvocation.invoke(probe.run(), Done)
    outcome.value mustEqual None
    probe.resolve()
    outcome.value mustEqual Some(Success(Done))
  }

  "invoke eagerly, i.e., return immediately" in {
    val probe = new Probe
    val outcome = EagerInvocation.invoke(probe.run(), Done)
    outcome.isCompleted mustEqual true
    probe.resolve()
    outcome.isCompleted mustEqual true
  }

}
