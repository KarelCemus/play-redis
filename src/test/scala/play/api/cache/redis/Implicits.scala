package play.api.cache.redis

import java.util.concurrent.Callable

import scala.collection.mutable
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util._

import play.api.cache.redis.configuration._

import akka.actor.ActorSystem

import org.specs2.execute.{AsResult, Result}
import org.specs2.matcher.Expectations
import org.specs2.mock.mockito._
import org.specs2.specification.{Around, Scope}

object Implicits {

  val defaultCacheName = "play"
  val localhost = "localhost"
  val localhostIp = "127.0.0.1"
  val dockerIp = localhostIp
  val defaultPort = 6379

  val defaults = RedisSettingsTest("akka.actor.default-dispatcher", "lazy", RedisTimeouts(1.second, None, 500.millis), "log-and-default", "standalone")

  val defaultInstance = RedisStandalone(defaultCacheName, RedisHost(localhost, defaultPort), defaults)

  implicit def implicitlyImmutableSeq[T](value: mutable.ListBuffer[T]): Seq[T] = value.toSeq

  implicit def implicitlyAny2Some[T](value: T): Option[T] = Some(value)

  implicit def implicitlyAny2future[T](value: T): Future[T] = Future.successful(value)

  implicit def implicitlyEx2future(ex: Throwable): Future[Nothing] = Future.failed(ex)

  implicit def implicitlyAny2success[T](value: T): Try[T] = Success(value)

  implicit def implicitlyAny2failure(ex: Throwable): Try[Nothing] = Failure(ex)

  implicit class FutureAwait[T](val future: Future[T]) extends AnyVal {
    def awaitForFuture = Await.result(future, 2.minutes)
  }

  implicit def implicitlyAny2Callable[T](f: => T): Callable[T] = new Callable[T] {
    def call() = f
  }

  implicit class RichFutureObject(val future: Future.type) extends AnyVal {
    /** returns a future resolved in given number of seconds */
    def after[T](seconds: Int, value: => T)(implicit system: ActorSystem, ec: ExecutionContext): Future[T] = {
      val promise = Promise[T]()
      // after a timeout, resolve the promise
      akka.pattern.after(seconds.seconds, system.scheduler) {
        promise.success(value)
        promise.future
      }
      // return the promise
      promise.future
    }

    def after(seconds: Int)(implicit system: ActorSystem, ec: ExecutionContext): Future[Unit] = {
      after(seconds, ())
    }
  }
}

trait ReducedMockito extends MocksCreation
  with CalledMatchers
  with MockitoStubs
  with CapturedArgument
  // with MockitoMatchers
  // with ArgThat
  with Expectations
  with MockitoFunctions {

  override def argThat[T, U <: T](m: org.specs2.matcher.Matcher[U]): T = super.argThat(m)
}

object MockitoImplicits extends ReducedMockito

trait WithApplication {
  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder

  protected def builder = new GuiceApplicationBuilder()

  private lazy val theBuilder = builder

  protected lazy val injector = theBuilder.injector()

  protected lazy val application: Application = injector.instanceOf[Application]

  implicit protected lazy val system: ActorSystem = injector.instanceOf[ActorSystem]
}

trait WithHocon {
  import play.api.Configuration

  import com.typesafe.config.ConfigFactory

  protected def hocon: String

  protected lazy val config = {
    val reference = ConfigFactory.load()
    val local = ConfigFactory.parseString(hocon.stripMargin)
    local.withFallback(reference)
  }

  protected lazy val configuration = Configuration(config)
}

abstract class WithConfiguration(val hocon: String) extends WithHocon with Around with Scope {
  def around[T: AsResult](t: => T): Result = AsResult.effectively(t)
}
