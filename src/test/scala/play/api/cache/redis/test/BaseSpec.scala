package play.api.cache.redis.test

import org.apache.pekko.Done
import org.scalactic.source.Position
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.{AnyWordSpecLike, AsyncWordSpecLike}
import play.api.cache.redis.RedisException
import play.api.cache.redis.configuration._

import java.util.concurrent.{CompletionStage, TimeoutException}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait DefaultValues {

  final protected val defaultCacheName: String = "play"
  final protected val localhost = "localhost"
  final protected val defaultPort: Int = 6379

  val defaultsSettings: RedisSettingsTest =
    RedisSettingsTest(
      invocationContext = "pekko.actor.default-dispatcher",
      invocationPolicy = "lazy",
      timeout = RedisTimeouts(1.second, None, Some(500.millis)),
      recovery = "log-and-default",
      source = "standalone",
      threadPool = RedisThreadPools(1, 1),
    )

  final protected val cacheKey: String = "cache-key"
  final protected val cacheValue: String = "cache-value"
  final protected val otherKey: String = "other-key"
  final protected val otherValue: String = "other-value"
  final protected val field: String = "field"

  final protected val cacheExpiration: FiniteDuration = 1.minute

  final protected val failure: RedisException = SimulatedException.asRedis

}

trait ImplicitOptionMaterialization {
  implicit protected def implicitlyAny2Some[T](value: T): Option[T] = Some(value)
}

trait ImplicitFutureMaterialization {
  implicit protected def implicitlyThrowable2Future[T](cause: Throwable): Future[T] = Future.failed(cause)
  implicit protected def implicitlyAny2Future[T](value: T): Future[T] = Future.successful(value)
}

trait TimeLimitedSpec extends AsyncTestSuiteMixin with AsyncUtilities {
  this: AsyncTestSuite =>

  protected def testTimeout: FiniteDuration = 1.second

  private class TimeLimitedTest(inner: NoArgAsyncTest) extends NoArgAsyncTest {
    override def apply(): FutureOutcome = restrict(inner())

    override val configMap: ConfigMap = inner.configMap
    override val name: String = inner.name
    override val scopes: IndexedSeq[String] = inner.scopes
    override val text: String = inner.text
    override val tags: Set[String] = inner.tags
    override val pos: Option[Position] = inner.pos
  }

  private def restrict(test: FutureOutcome): FutureOutcome = {
    val result: Future[Outcome] = Future.firstCompletedOf(
      Seq(
        Future.after(testTimeout, ()).map(_ => fail(s"Test didn't finish within $testTimeout.")),
        test.toFuture,
      ),
    )
    new FutureOutcome(result)
  }

  abstract override def withFixture(test: NoArgAsyncTest): FutureOutcome =
    super.withFixture(new TimeLimitedTest(test))

}

trait AsyncUtilities { this: AsyncTestSuite =>

  implicit class FutureAsyncUtilities(future: Future.type) {

    def after[T](duration: FiniteDuration, value: => T): Future[T] =
      Future(Await.result(Future.never, duration)).recover(_ => value)

    def waitFor(duration: FiniteDuration): Future[Unit] =
      after(duration, ())

  }

}

trait FutureAssertions extends AsyncUtilities { this: AsyncBaseSpec =>
  import scala.jdk.FutureConverters._

  implicit def completionStageToFutureOps[T](future: CompletionStage[T]): FutureAssertionOps[T] =
    new FutureAssertionOps(future.asScala)

  implicit def completionStageToFutureDoneOps(future: CompletionStage[Done]): FutureAssertionDoneOps =
    new FutureAssertionDoneOps(future.asScala)

  implicit class FutureAssertionOps[T](future: Future[T]) {

    def asserting(f: T => Assertion): Future[Assertion] =
      future.map(f)

    def assertingCondition(f: T => Boolean): Future[Assertion] =
      future.map(v => assert(f(v)))

    def assertingEqual(expected: => T): Future[Assertion] =
      asserting(_ mustEqual expected)

    def assertingTry(f: Try[T] => Assertion): Future[Assertion] =
      future.map(Success.apply).recover { case ex => Failure(ex) }.map(f)

    def assertingFailure[Cause <: Throwable: ClassTag]: Future[Assertion] =
      future.map(value => fail(s"Expected exception but got $value")).recover { case ex => ex mustBe a[Cause] }

    def assertingFailure[Cause <: Throwable: ClassTag, InnerCause <: Throwable: ClassTag]: Future[Assertion] =
      future.map(value => fail(s"Expected exception but got $value")).recover { case ex =>
        ex mustBe a[Cause]
        ex.getCause mustBe a[InnerCause]
      }

    def assertingFailure(cause: Throwable): Future[Assertion] =
      future.map(value => fail(s"Expected exception but got $value")).recover { case ex => ex mustEqual cause }

    def assertingSuccess: Future[Assertion] =
      future.recover(cause => fail("Got unexpected exception", cause)).map(_ => Passed)

    def assertTimeout(timeout: FiniteDuration): Future[Assertion] =
      Future
        .firstCompletedOf(
          Seq(
            Future.after(timeout, throw new TimeoutException(s"Expected timeout after $timeout")),
            future.map(value => fail(s"Expected timeout but got $value")),
          ),
        )
        .assertingFailure[TimeoutException]

  }

  implicit class FutureAssertionDoneOps(future: Future[Done]) {
    def assertingDone: Future[Assertion] = future.assertingEqual(Done)
  }

}

trait BaseSpec extends Matchers {

  protected type Assertion = org.scalatest.Assertion
  protected val Passed: Assertion = org.scalatest.Succeeded
}

trait AsyncBaseSpec extends BaseSpec with AsyncWordSpecLike with AsyncMockFactory with FutureAssertions with AsyncUtilities with TimeLimitedSpec {

  implicit override def executionContext: ExecutionContext = ExecutionContext.global
}

trait UnitSpec extends BaseSpec with AnyWordSpecLike with DefaultValues

trait AsyncUnitSpec extends AsyncBaseSpec with DefaultValues

trait IntegrationSpec extends AsyncBaseSpec
