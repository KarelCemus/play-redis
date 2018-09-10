package play.api.cache.redis.impl

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class InvocationPolicySpecs(implicit ee: ExecutionEnv) extends Specification with ReducedMockito with WithApplication {

  import Implicits._
  import RedisCacheImplicits._

  val andThen = "then"

  def longTask(andThen: => Unit) = Future.after(seconds = 2, { andThen; "result" })

  "InvocationPolicy" should {

    "invoke lazily, i.e., slowly" in {
      var resolved = false
      val promise = longTask { resolved = true }
      LazyInvocation.invoke(promise, andThen) must beEqualTo(andThen).awaitFor(3.seconds)
      resolved mustEqual true
      promise.isCompleted mustEqual true
    }

    "invoke eagerly, i.e., return immediately" in {
      var resolved = false
      val promise = longTask { resolved = true }
      EagerInvocation.invoke(promise, andThen) must beEqualTo(andThen).awaitFor(3.seconds)
      resolved mustEqual false
      promise must beEqualTo("result").awaitFor(3.seconds)
      resolved mustEqual true
    }
  }
}
