package play.api.cache.redis.impl

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import play.api.Environment
import play.api.cache.redis._

import org.mockito.InOrder
import org.specs2.matcher.Matchers
import org.specs2.specification.Scope

object RedisCacheImplicits {
  import MockitoImplicits._

  type Future[T] = scala.concurrent.Future[T]

  def anyClassTag[T: ClassTag] = org.mockito.ArgumentMatchers.any[ClassTag[T]]

  //noinspection UnitMethodIsParameterless
  def unit: Unit = ()

  def execDone: Done = Done
  def beDone = Matchers.beEqualTo(Done)

  def anyVarArgs[T] = org.mockito.ArgumentMatchers.any[T]

  def beEq[T](value: T) = org.mockito.ArgumentMatchers.eq(value)

  def there = MockitoImplicits.there
  def one[T <: AnyRef](mock: T)(implicit anOrder: Option[InOrder] = inOrder()) = MockitoImplicits.one(mock)
  def two[T <: AnyRef](mock: T)(implicit anOrder: Option[InOrder] = inOrder()) = MockitoImplicits.two(mock)

  val ex = TimeoutException(new IllegalArgumentException("Simulated failure."))

  trait AbstractMocked extends Scope {
    val key = "key"
    val value = "value"
    val other = "other"

    protected def invocation = LazyInvocation

    protected def policy: RecoveryPolicy = new RecoverWithDefault {}

    protected implicit val runtime = mock[RedisRuntime]
    runtime.context returns ExecutionContext.global
    runtime.invocation returns invocation
    runtime.prefix returns RedisEmptyPrefix
    runtime.policy returns policy
  }

  class MockedConnector extends AbstractMocked {
    protected val connector = mock[RedisConnector]
  }

  class MockedCache extends MockedConnector {
    protected val cache = new RedisCache(connector, Builders.AsynchronousBuilder)
  }

  class MockedList extends MockedCache {
    val data = new mutable.ListBuffer[String]
    data.append(other, value, value)

    protected val list = cache.list[String]("key")
  }

  class MockedSet extends MockedCache {
    val data = mutable.Set[String](other, value)

    protected val set = cache.set[String]("key")
  }

  class MockedMap extends MockedCache {
    val field = "field"

    protected val map = cache.map[String]("key")
  }

  class MockedAsyncRedis extends MockedConnector {
    protected val cache = new AsyncRedis(connector)
  }

  class MockedSyncRedis extends MockedConnector {
    protected val cache = new SyncRedis(connector)
    runtime.timeout returns akka.util.Timeout(1.second)
  }

  class MockedJavaRedis extends AbstractMocked {
    val expiration = 5.seconds

    protected val environment = mock[Environment]
    protected val async = mock[AsyncRedis]
    protected val cache: play.cache.AsyncCacheApi = new JavaRedis(async, environment)

    environment.classLoader returns getClass.getClassLoader
  }

  trait OrElse extends Scope {

    protected var orElse = 0

    def doElse[T](value: T): T = {
      orElse += 1
      value
    }

    def doFuture[T](value: T): Future[T] = {
      Future.successful(doElse(value))
    }

    def failedFuture: Future[Nothing] = {
      orElse += 1
      Future.failed(failure)
    }

    def fail = throw failure

    private def failure = TimeoutException(new IllegalArgumentException("This should no be reached"))
  }

  trait Attempts extends Scope {

    protected var attempts = 0

    def attempt[T](f: => T): T = {
      attempts += 1
      f
    }
  }
}
