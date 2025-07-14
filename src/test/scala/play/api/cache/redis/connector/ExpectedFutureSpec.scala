package play.api.cache.redis.connector

import play.api.cache.redis._
import play.api.cache.redis.test._

import scala.concurrent.{ExecutionContext, Future}

class ExpectedFutureSpec extends AsyncUnitSpec {
  import ExpectedFutureSpec._

  private class TestSuite(name: String)(implicit f: ExpectationBuilder[String]) {

    name should {

      "expected value" in {
        Future.successful("expected").asExpected.assertingEqual("ok")
      }

      "unexpected value" in {
        Future.successful("unexpected").asExpected.assertingFailure[UnexpectedResponseException]
      }

      "failing expectation" in {
        Future.successful("failing").asExpected.assertingFailure[ExecutionFailedException]
      }

      "failing future inside redis" in {
        Future.failed[String](TimeoutException(SimulatedException)).asExpected.assertingFailure[TimeoutException]
      }

      "failing future with runtime exception" in {
        Future.failed[String](SimulatedException).asExpected.assertingFailure[ExecutionFailedException]
      }
    }

  }

  new TestSuite("Execution without the key")((future: Future[String]) => future.executing(cmd))

  new TestSuite("Execution with the key")((future: Future[String]) => future.executing(cmd).withKey("key"))

  "building a command" in {
    Future.successful("expected").executing(cmd).toString mustEqual s"ExpectedFuture($cmd)"
    Future.successful("expected").executing(cmd).withKey("key").andParameters("SET").andParameter(1).toString mustEqual s"ExpectedFuture($cmd key SET 1)"
    Future.successful("expected").executing(cmd).withKeys(Seq("key1", "key2")).andParameters(Seq[Any]("SET", 1)).toString mustEqual s"ExpectedFuture($cmd key1 key2 SET 1)"

    Future.successful("expected").executing(cmd).withKey("key").asCommand("other 2").toString mustEqual "ExpectedFuture(TEST CMD other 2)"
  }

}

object ExpectedFutureSpec {

  private val cmd = "TEST CMD"

  private val expectation: PartialFunction[Any, String] = {
    case "failing"  => throw SimulatedException
    case "expected" => "ok"
  }

  private type ExpectationBuilder[T] = Future[T] => ExpectedFuture[String]

  implicit private class FutureBuilder[T](private val future: Future[T]) extends AnyVal {
    def asExpected(implicit ev: ExpectationBuilder[T], context: ExecutionContext): Future[String] = ev(future).expects(expectation)
    def asCommand(implicit ev: ExpectationBuilder[T]): String = ev(future).toString
  }

}
