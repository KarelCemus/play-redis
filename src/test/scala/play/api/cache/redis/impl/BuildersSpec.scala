package play.api.cache.redis.impl

import org.apache.pekko.pattern.AskTimeoutException

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import play.api.cache.redis._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification._

class BuildersSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito with WithApplication {

  import Builders._
  import BuildersSpec._
  import Implicits._

  def defaultTask = Future.successful("default")

  def regularTask = Future("response")

  def longTask = Future.after(seconds = 2, "response")

  def failingTask = Future.failed(TimeoutException(new IllegalArgumentException("Simulated failure.")))

  "AsynchronousBuilder" should {

    "match on name" in {
      AsynchronousBuilder.name mustEqual "AsynchronousBuilder"
    }

    "run" in new RuntimeMock {
      AsynchronousBuilder.toResult(regularTask, defaultTask) must beEqualTo("response").await
    }

    "run long running task" in new RuntimeMock {
      AsynchronousBuilder.toResult(longTask, defaultTask) must beEqualTo("response").awaitFor(3.seconds)
    }

    "recover with default policy" in new RuntimeMock {
      runtime.policy returns defaultPolicy
      AsynchronousBuilder.toResult(failingTask, defaultTask) must beEqualTo("default").await
    }

    "recover with fail through policy" in new RuntimeMock {
      runtime.policy returns failThrough
      AsynchronousBuilder.toResult(failingTask, defaultTask) must throwA[TimeoutException].await
    }

    "map value" in new RuntimeMock {
      AsynchronousBuilder.map(Future(5))(_ + 5) must beEqualTo(10).await
    }
  }

  "SynchronousBuilder" should {

    "match on name" in {
      SynchronousBuilder.name mustEqual "SynchronousBuilder"
    }

    "run" in new RuntimeMock {
      SynchronousBuilder.toResult(regularTask, defaultTask) must beEqualTo("response")
    }

    "recover from failure with default policy" in new RuntimeMock {
      runtime.policy returns defaultPolicy
      SynchronousBuilder.toResult(failingTask, defaultTask) must beEqualTo("default")
    }

    "recover from failure with fail through policy" in new RuntimeMock {
      runtime.policy returns failThrough
      SynchronousBuilder.toResult(failingTask, defaultTask) must throwA[TimeoutException]
    }

    "recover from timeout due to long running task" in new RuntimeMock {
      runtime.policy returns failThrough
      SynchronousBuilder.toResult(longTask, defaultTask) must throwA[TimeoutException]
    }

    "recover from pekko ask timeout" in new RuntimeMock {
      runtime.policy returns failThrough
      val actorFailure = Future.failed(new AskTimeoutException("Simulated actor ask timeout"))
      SynchronousBuilder.toResult(actorFailure, defaultTask) must throwA[TimeoutException]
    }

    "map value" in new RuntimeMock {
      SynchronousBuilder.map(5)(_ + 5) must beEqualTo(10)
    }
  }
}

object BuildersSpec {

  trait RuntimeMock extends Scope {

    import MockitoImplicits._

    private val timeout = org.apache.pekko.util.Timeout(1.second)
    implicit protected val runtime: RedisRuntime = mock[RedisRuntime]

    runtime.timeout returns timeout
    runtime.context returns ExecutionContext.global

    protected def failThrough = new FailThrough {}

    protected def defaultPolicy = new RecoverWithDefault {}
  }
}
