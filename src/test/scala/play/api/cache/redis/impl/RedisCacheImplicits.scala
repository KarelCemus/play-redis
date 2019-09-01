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

  object NoSuchElementException extends NoSuchElementException

  trait AbstractMocked extends Scope {
    protected val key = "key"
    protected val value = "value"
    protected val other = "other"

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
    protected lazy val cache = new RedisCache(connector, Builders.AsynchronousBuilder)
  }

  class MockedList extends MockedCache {
    protected val data = new mutable.ListBuffer[String]
    data.appendAll(Iterable(other, value, value))

    protected val list = cache.list[String]("key")
  }

  class MockedSet extends MockedCache {
    protected val data = mutable.Set[String](other, value)

    protected val set = cache.set[String]("key")
  }

  class MockedMap extends MockedCache {
    protected val field = "field"

    protected val map = cache.map[String]("key")
  }

  class MockedAsyncRedis extends MockedConnector {
    protected val cache: AsyncRedis = new AsyncRedisImpl(connector)
  }

  class MockedSyncRedis extends MockedConnector {
    protected val cache = new SyncRedis(connector)
    runtime.timeout returns akka.util.Timeout(1.second)
  }

  class MockedJavaRedis extends AbstractMocked {
    protected val expiration = 5.seconds
    protected val expirationLong = expiration.toSeconds
    protected val expirationInt = expirationLong.intValue
    protected val classTag = "java.lang.String"
    protected val classTagKey = s"classTag::$key"
    protected val classTagOther = s"classTag::$other"

    protected implicit val environment = mock[Environment]
    protected val async = mock[AsyncRedis]
    protected val cache: play.cache.redis.AsyncCacheApi = new AsyncJavaRedis(async)

    environment.classLoader returns getClass.getClassLoader
  }

  class MockedJavaList extends AbstractMocked {
    protected val internal = mock[RedisList[String, Future]]
    protected val view = mock[internal.RedisListView]
    protected val modifier = mock[internal.RedisListModification]
    protected val list = new RedisListJavaImpl(internal)
    internal.view returns view
    internal.modify returns modifier
  }

  class MockedJavaSet extends MockedJavaRedis {
    protected val internal = mock[RedisSet[String, Future]]
    protected val set = new RedisSetJavaImpl(internal)
  }

  class MockedJavaMap extends MockedJavaRedis {
    val field = "field"
    protected val internal = mock[RedisMap[String, Future]]
    protected val map = new RedisMapJavaImpl(internal)
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
