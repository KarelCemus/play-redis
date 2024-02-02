package play.api.cache.redis.impl

import akka.pattern.AskTimeoutException
import play.api.cache.redis._
import play.api.cache.redis.test._

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

class BuildersSpec extends AsyncUnitSpec with RedisRuntimeMock {
  import Builders._

  private case class Task(
    response: String,
    execution: () => Future[String],
  ) extends (() => Future[String]) {

    def this(response: String)(f: String => Future[String]) =
      this(response, () => f(response))

    def apply(): Future[String] = execution()
  }

  private object Task {
    val resolved: Task = new Task("default response")(Future.successful)
    val regular: Task = new Task("regular response")(Future(_))
    def failing(): Future[String] = Future.failed(TimeoutException(SimulatedException))
    def infinite(): Future[String] = Promise[String]().future
  }

  "AsynchronousBuilder" should {

    "match on name" in {
      AsynchronousBuilder.name mustEqual "AsynchronousBuilder"
    }

    "run regular task" in {
      implicit val runtime: RedisRuntime = redisRuntime()
      AsynchronousBuilder.toResult(Task.regular(), Task.resolved()).assertingEqual(Task.regular.response)
    }

    "run resolved task" in {
      implicit val runtime: RedisRuntime = redisRuntime()
      AsynchronousBuilder.toResult(Task.resolved(), Task.regular()).assertingEqual(Task.resolved.response)
    }

    "recover with default policy" in {
      implicit val runtime: RedisRuntime = redisRuntime(recoveryPolicy = recoveryPolicy.default)
      AsynchronousBuilder.toResult(Task.failing(), Task.resolved()).assertingEqual(Task.resolved.response)
    }

    "recover with fail through policy" in {
      implicit val runtime: RedisRuntime = redisRuntime(recoveryPolicy = recoveryPolicy.failThrough)
      AsynchronousBuilder.toResult(Task.failing(), Task.resolved()).assertingFailure[TimeoutException]
    }

    "map value" in {
      implicit val runtime: RedisRuntime = redisRuntime(recoveryPolicy = recoveryPolicy.failThrough)
      AsynchronousBuilder.map(Future(5))(_ + 5).assertingEqual(10)
    }
  }

  "SynchronousBuilder" should {

    "match on name" in {
      SynchronousBuilder.name mustEqual "SynchronousBuilder"
    }

    "run regular task" in {
      implicit val runtime: RedisRuntime = redisRuntime()
      SynchronousBuilder.toResult(Task.regular(), Task.resolved()) mustEqual Task.regular.response
    }

    "run resolved task" in {
      implicit val runtime: RedisRuntime = redisRuntime()
      SynchronousBuilder.toResult(Task.resolved(), Task.regular()) mustEqual Task.resolved.response
    }

    "recover from failure with default policy" in {
      implicit val runtime: RedisRuntime = redisRuntime(recoveryPolicy = recoveryPolicy.default)
      SynchronousBuilder.toResult(Task.failing(), Task.resolved()) mustEqual Task.resolved.response
    }

    "don't recover from failure with fail through policy" in {
      implicit val runtime: RedisRuntime = redisRuntime(recoveryPolicy = recoveryPolicy.failThrough)
      assertThrows[TimeoutException] {
        SynchronousBuilder.toResult(Task.failing(), Task.resolved())
      }
    }

    "don't recover on timeout due to long running task with fail through policy" in {
      implicit val runtime: RedisRuntime = redisRuntime(
        recoveryPolicy = recoveryPolicy.failThrough,
        timeout = 1.millis,
      )
      assertThrows[TimeoutException] {
        SynchronousBuilder.toResult(Task.infinite(), Task.resolved())
      }
    }

    "recover from timeout due to long running task with default policy" in {
      implicit val runtime: RedisRuntime = redisRuntime(
        recoveryPolicy = recoveryPolicy.default,
        timeout = 1.millis,
      )
      SynchronousBuilder.toResult(Task.infinite(), Task.resolved()) mustEqual Task.resolved.response
    }

    "recover from akka ask timeout" in {
      implicit val runtime: RedisRuntime = redisRuntime(recoveryPolicy = recoveryPolicy.default)
      val actorFailure = Future.failed(new AskTimeoutException("Simulated actor ask timeout"))
      SynchronousBuilder.toResult(actorFailure, Task.resolved()) mustEqual Task.resolved.response
    }

    "map value" in {
      implicit val runtime: RedisRuntime = redisRuntime()
      SynchronousBuilder.map(5)(_ + 5) mustEqual 10
    }
  }

}
