package play.api.cache.redis.connector

import scala.concurrent.{ExecutionContext, Future}

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class ExpectedFutureSpec(implicit ee: ExecutionEnv) extends Specification {

  import ExpectedFutureSpec._

  class Suite(name: String)(implicit f: ExpectationBuilder[String]) {

    name >> {

      "expected value" in {
        Future.successful("expected").asExpected must beEqualTo("ok").await
      }

      "unexpected value" in {
        Future.successful("unexpected").asExpected must throwA[UnexpectedResponseException].await
      }

      "failing expectation" in {
        Future.successful("failing").asExpected must throwA[ExecutionFailedException].await
      }

      "failing future inside redis" in {
        Future.failed[String](TimeoutException(simulatedFailure)).asExpected must throwA[TimeoutException].await
      }

      "failing future with runtime exception" in {
        Future.failed[String](simulatedFailure).asExpected must throwA[ExecutionFailedException].await
      }
    }
  }

  new Suite("Execution without the key")((future: Future[String]) => future.executing(cmd))

  new Suite("Execution with the key")((future: Future[String]) => future.executing(cmd).withKey("key"))

  "building a command" in {
    Future.successful("expected").executing(cmd).toString mustEqual s"ExpectedFuture($cmd)"
    Future.successful("expected").executing(cmd).withKey("key").andParameters("SET").andParameter(1).toString mustEqual s"ExpectedFuture($cmd key SET 1)"
    Future.successful("expected").executing(cmd).withKeys(Seq("key1", "key2")).andParameters(Seq("SET", 1)).toString mustEqual s"ExpectedFuture($cmd key1 key2 SET 1)"

    Future.successful("expected").executing(cmd).withKey("key").asCommand("other 2").toString mustEqual s"ExpectedFuture(TEST CMD other 2)"
  }
}

object ExpectedFutureSpec {

  val cmd = "TEST CMD"

  def simulatedFailure = new RuntimeException("Simulated runtime failure")

  val expectation: PartialFunction[Any, String] = {
    case "failing"  => throw simulatedFailure
    case "expected" => "ok"
  }

  implicit class ExpectationBuilder[T](val f: Future[T] => ExpectedFuture[String]) extends AnyVal {
    def apply(future: Future[T]): ExpectedFuture[String] = f(future)
  }

  implicit class FutureBuilder[T](val future: Future[T]) extends AnyVal {
    def asExpected(implicit ev: ExpectationBuilder[T], context: ExecutionContext): Future[String] = ev(future).expects(expectation)
    def asCommand(implicit ev: ExpectationBuilder[T]): String = ev(future).toString
  }
}
