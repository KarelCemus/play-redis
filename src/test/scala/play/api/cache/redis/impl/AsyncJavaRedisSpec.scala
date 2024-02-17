package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._
import play.api.{Environment, Mode}
import play.cache.redis._

import java.util.Optional
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.IterableHasAsScala

class AsyncJavaRedisSpec extends AsyncUnitSpec with MockedAsyncRedis with RedisRuntimeMock {
  import Helpers._

  private val expiration = 5.seconds
  private val expirationLong = expiration.toSeconds
  private val expirationInt = expirationLong.intValue
  private val classTag = "java.lang.String"

  test("get and miss") { (async, cache) =>
    for {
      _ <- async.expect.getClassTag(cacheKey, None)
      _ <- cache.get[String](cacheKey).assertingEqual(Optional.empty)
    } yield Passed
  }

  test("get and hit") { (async, cache) =>
    for {
      _ <- async.expect.getClassTag(cacheKey, Some(classTag))
      _ <- async.expect.get[String](cacheKey, Some(cacheValue))
      _ <- cache.get[String](cacheKey).assertingEqual(Optional.of(cacheValue))
    } yield Passed
  }

  test("get null") { (async, cache) =>
    for {
      _ <- async.expect.getClassTag(cacheKey, Some("null"))
      _ <- cache.get[String](cacheKey).assertingEqual(Optional.empty)
    } yield Passed
  }

  test("set") { (async, cache) =>
    for {
      _ <- async.expect.set(cacheKey, cacheValue, Duration.Inf)
      _ <- cache.set(cacheKey, cacheValue).assertingDone
    } yield Passed
  }

  test("set with expiration") { (async, cache) =>
    for {
      _ <- async.expect.set(cacheKey, cacheValue, expiration)
      _ <- cache.set(cacheKey, cacheValue, expiration.toSeconds.toInt).assertingDone
    } yield Passed
  }

  test("set null") { (async, cache) =>
    for {
      _ <- async.expect.set[AnyRef](cacheKey, null, Duration.Inf)
      _ <- cache.set(cacheKey, null: AnyRef).assertingDone
    } yield Passed
  }

  test("get or else (sync)") { (async, cache) =>
    for {
      _ <- async.expect.getClassTag(cacheKey, None)
      _ <- async.expect.set(cacheKey, cacheValue, Duration.Inf)
      _ <- async.expect.getClassTag(cacheKey, Some(classTag))
      _ <- async.expect.get[String](cacheKey, Some(cacheValue))
      orElse = probe.orElse.const(cacheValue)
      _ <- cache.getOrElse(cacheKey, orElse.execute _).assertingEqual(cacheValue)
      _ <- cache.getOrElse(cacheKey, orElse.execute _).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else (async)") { (async, cache) =>
    for {
      _ <- async.expect.getClassTag(cacheKey, None)
      _ <- async.expect.set(cacheKey, cacheValue, Duration.Inf)
      _ <- async.expect.getClassTag(cacheKey, Some(classTag))
      _ <- async.expect.get[String](cacheKey, Some(cacheValue))
      orElse = probe.orElse.asyncJava(cacheValue)
      _ <- cache.getOrElseUpdate(cacheKey, orElse.execute _).assertingEqual(cacheValue)
      _ <- cache.getOrElseUpdate(cacheKey, orElse.execute _).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else with expiration (sync)") { (async, cache) =>
    for {
      _ <- async.expect.getClassTag(cacheKey, None)
      _ <- async.expect.set(cacheKey, cacheValue, expiration)
      _ <- async.expect.getClassTag(cacheKey, Some(classTag))
      _ <- async.expect.get[String](cacheKey, Some(cacheValue))
      orElse = probe.orElse.const(cacheValue)
      _ <- cache.getOrElse(cacheKey, orElse.execute _, expiration.toSeconds.toInt).assertingEqual(cacheValue)
      _ <- cache.getOrElse(cacheKey, orElse.execute _, expiration.toSeconds.toInt).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else with expiration (async)") { (async, cache) =>
    for {
      _ <- async.expect.getClassTag(cacheKey, None)
      _ <- async.expect.set(cacheKey, cacheValue, expiration)
      _ <- async.expect.getClassTag(cacheKey, Some(classTag))
      _ <- async.expect.get[String](cacheKey, Some(cacheValue))
      orElse = probe.orElse.asyncJava(cacheValue)
      _ <- cache.getOrElseUpdate(cacheKey, orElse.execute _, expiration.toSeconds.toInt).assertingEqual(cacheValue)
      _ <- cache.getOrElseUpdate(cacheKey, orElse.execute _, expiration.toSeconds.toInt).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("remove") { (async, cache) =>
    for {
      _ <- async.expect.remove(cacheKey)
      _ <- cache.remove(cacheKey).assertingDone
    } yield Passed
  }

  test("get and set 'byte'") { (async, cache) =>
    val byte = JavaTypes.byteValue
    for {
      // set a cacheValue
      // note: there should be hit on "byte" but the cacheValue is wrapped instead
      _ <- async.expect.setValue(cacheKey, byte, Duration.Inf)
      _ <- async.expect.setClassTag(cacheKey, "java.lang.Byte", Duration.Inf)
      _ <- cache.set(cacheKey, byte).assertingDone
      // hit on GET
      _ <- async.expect.getClassTag(cacheKey, Some("java.lang.Byte"))
      _ <- async.expect.get[java.lang.Byte](cacheKey, Some(byte))
      _ <- cache.get[Byte](cacheKey).assertingEqual(Optional.ofNullable(byte))
    } yield Passed
  }

  test("get and set 'byte[]'") { (async, cache) =>
    val scalaBytes = JavaTypes.bytesValue
    val javaBytes = scalaBytes.map[java.lang.Byte](x => x)
    for {
      // set a cacheValue
      _ <- async.expect.setValue(cacheKey, scalaBytes, Duration.Inf)
      _ <- async.expect.setClassTag(cacheKey, "byte[]", Duration.Inf)
      _ <- cache.set(cacheKey, scalaBytes).assertingDone
      // hit on GET
      _ <- async.expect.getClassTag(cacheKey, Some("byte[]"))
      _ <- async.expect.get[Array[java.lang.Byte]](cacheKey, Some(javaBytes))
      _ <- cache.get[Array[java.lang.Byte]](cacheKey).assertingEqual(Optional.ofNullable(javaBytes))
    } yield Passed
  }

  test("get all") { (async, cache) =>
    for {
      _ <- async.expect.getAllKeys[String](Iterable(cacheKey, cacheKey, cacheKey), Seq(Some(cacheValue), None, None))
      _ <- cache
             .getAll(classOf[String], cacheKey, cacheKey, cacheKey)
             .asserting(_.asScala.toList mustEqual List(Optional.of(cacheValue), Optional.empty, Optional.empty))
    } yield Passed
  }

  test("get all (keys in a collection)") { (async, cache) =>
    import JavaCompatibility.JavaList
    for {
      _ <- async.expect.getAllKeys[String](Iterable(cacheKey, cacheKey, cacheKey), Seq(Some(cacheValue), None, None))
      _ <- cache
             .getAll(classOf[String], JavaList(cacheKey, cacheKey, cacheKey))
             .asserting(_.asScala.toList mustEqual List(Optional.of(cacheValue), Optional.empty, Optional.empty))
    } yield Passed
  }

  test("set if not exists (exists)") { (async, cache) =>
    for {
      _ <- async.expect.setIfNotExists(cacheKey, cacheValue, Duration.Inf, exists = false)
      _ <- cache.setIfNotExists(cacheKey, cacheValue).assertingEqual(false)
    } yield Passed
  }

  test("set if not exists (not exists)") { (async, cache) =>
    for {
      _ <- async.expect.setIfNotExists(cacheKey, cacheValue, Duration.Inf, exists = true)
      _ <- cache.setIfNotExists(cacheKey, cacheValue).assertingEqual(true)
    } yield Passed
  }

  test("set if not exists (exists) with expiration") { (async, cache) =>
    for {
      _ <- async.expect.setIfNotExists(cacheKey, cacheValue, expiration, exists = false)
      _ <- cache.setIfNotExists(cacheKey, cacheValue, expirationInt).assertingEqual(false)
    } yield Passed
  }

  test("set if not exists (not exists) with expiration") { (async, cache) =>
    for {
      _ <- async.expect.setIfNotExists(cacheKey, cacheValue, expiration, exists = true)
      _ <- cache.setIfNotExists(cacheKey, cacheValue, expirationInt).assertingEqual(true)
    } yield Passed
  }

  test("set all") { (async, cache) =>
    val values = Seq((cacheKey, cacheValue), (otherKey, otherValue))
    for {
      _ <- async.expect.setAll(values: _*)
      javaValues = values.map { case (k, v) => new KeyValue(k, v) }
      _ <- cache.setAll(javaValues: _*).assertingDone
    } yield Passed
  }

  test("set all if not exists (exists)") { (async, cache) =>
    val values = Seq((cacheKey, cacheValue), (otherKey, otherValue))
    for {
      _ <- async.expect.setAllIfNotExist(values, exists = false)
      javaValues = values.map { case (k, v) => new KeyValue(k, v) }
      _ <- cache.setAllIfNotExist(javaValues: _*).assertingEqual(false)
    } yield Passed
  }

  test("set all if not exists (not exists)") { (async, cache) =>
    val values = Seq((cacheKey, cacheValue), (otherKey, otherValue))
    for {
      _ <- async.expect.setAllIfNotExist(values, exists = true)
      javaValues = values.map { case (k, v) => new KeyValue(k, v) }
      _ <- cache.setAllIfNotExist(javaValues: _*).assertingEqual(true)
    } yield Passed
  }

  test("append") { (async, cache) =>
    for {
      _ <- async.expect.append(cacheKey, cacheValue, Duration.Inf)
      _ <- async.expect.setClassTagIfNotExists(cacheKey, cacheValue, Duration.Inf, exists = false)
      _ <- cache.append(cacheKey, cacheValue).assertingDone
    } yield Passed
  }

  test("append with expiration") { (async, cache) =>
    for {
      _ <- async.expect.append(cacheKey, cacheValue, expiration)
      _ <- async.expect.setClassTagIfNotExists(cacheKey, cacheValue, expiration, exists = false)
      _ <- cache.append(cacheKey, cacheValue, expirationInt).assertingDone
    } yield Passed
  }

  test("expire") { (async, cache) =>
    for {
      _ <- async.expect.expire(cacheKey, expiration)
      _ <- cache.expire(cacheKey, expirationInt).assertingDone
    } yield Passed
  }

  test("expires in (defined)") { (async, cache) =>
    for {
      _ <- async.expect.expiresIn(cacheKey, Some(expiration))
      _ <- cache.expiresIn(cacheKey).assertingEqual(Optional.of(expirationLong))
    } yield Passed
  }

  test("expires in (undefined)") { (async, cache) =>
    for {
      _ <- async.expect.expiresIn(cacheKey, None)
      _ <- cache.expiresIn(cacheKey).assertingEqual(Optional.empty)
    } yield Passed
  }

  test("matching") { (async, cache) =>
    for {
      _ <- async.expect.matching("pattern", Seq(cacheKey))
      _ <- cache.matching("pattern").asserting(_.asScala.toList mustEqual List(cacheKey))
    } yield Passed
  }

  test("remove multiple") { (async, cache) =>
    for {
      _ <- async.expect.removeAll(cacheKey, otherKey)
      _ <- cache.remove(cacheKey, otherKey).assertingDone
    } yield Passed
  }

  test("remove all (invalidate)") { (async, cache) =>
    for {
      _ <- async.expect.invalidate()
      _ <- cache.removeAll().assertingDone
    } yield Passed
  }

  test("remove all (some keys provided)") { (async, cache) =>
    for {
      _ <- async.expect.removeAll(cacheKey, otherKey)
      _ <- cache.removeAllKeys(cacheKey, otherKey).assertingDone
    } yield Passed
  }

  test("remove matching") { (async, cache) =>
    for {
      _ <- async.expect.removeMatching("pattern")
      _ <- cache.removeMatching("pattern").assertingDone
    } yield Passed
  }

  test("exists") { (async, cache) =>
    for {
      _ <- async.expect.exists(cacheKey, exists = true)
      _ <- cache.exists(cacheKey).assertingEqual(true)
    } yield Passed
  }

  test("increment") { (async, cache) =>
    for {
      _ <- async.expect.increment(cacheKey, 1L, result = 10L)
      _ <- async.expect.increment(cacheKey, 2L, result = 20L)
      _ <- cache.increment(cacheKey).assertingEqual(10L)
      _ <- cache.increment(cacheKey, 2L).assertingEqual(20L)
    } yield Passed
  }

  test("decrement") { (async, cache) =>
    for {
      _ <- async.expect.decrement(cacheKey, 1L, result = 10L)
      _ <- async.expect.decrement(cacheKey, 2L, result = 20L)
      _ <- cache.decrement(cacheKey).assertingEqual(10L)
      _ <- cache.decrement(cacheKey, 2L).assertingEqual(20L)
    } yield Passed
  }

  test("create list") { (async, cache) =>
    trait RedisListMock extends RedisList[String, Future]
    val list = mock[RedisListMock]
    for {
      _ <- async.expect.list[String](cacheKey, list)
      _ <- cache.list(cacheKey, classOf[String]) mustBe a[AsyncRedisList[?]]
    } yield Passed
  }

  test("create set") { (async, cache) =>
    trait RedisSetMock extends RedisSet[String, Future]
    val set = mock[RedisSetMock]
    for {
      _ <- async.expect.set[String](cacheKey, set)
      _ <- cache.set(cacheKey, classOf[String]) mustBe a[AsyncRedisSet[?]]
    } yield Passed
  }

  test("create map") { (async, cache) =>
    trait RedisMapMock extends RedisMap[String, Future]
    val map = mock[RedisMapMock]
    for {
      _ <- async.expect.map[String](cacheKey, map)
      _ <- cache.map(cacheKey, classOf[String]) mustBe a[AsyncRedisMap[?]]
    } yield Passed
  }

  private def test(name: String)(f: (AsyncRedisMock, play.cache.redis.AsyncCacheApi) => Future[Assertion]): Unit =
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = recoveryPolicy.default,
      )
      implicit val environment: Environment = Environment(
        rootPath = new java.io.File("."),
        classLoader = getClass.getClassLoader,
        mode = Mode.Test,
      )
      val (async: AsyncRedis, asyncMock: AsyncRedisMock) = AsyncRedisMock.mock(this)
      val cache: play.cache.redis.AsyncCacheApi = new AsyncJavaRedis(async)

      f(asyncMock, cache)
    }

}
