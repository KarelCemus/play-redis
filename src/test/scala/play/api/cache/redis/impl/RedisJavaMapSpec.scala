package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._
import play.cache.redis.AsyncRedisMap

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class RedisJavaMapSpec extends AsyncUnitSpec with RedisMapJavaMock with RedisRuntimeMock {

  test("set") { (cache, internal) =>
    for {
      _ <- internal.expect.add(cacheKey, cacheValue)
      _ <- cache.add(cacheKey, cacheValue).assertingEqual(cache)
    } yield Passed
  }

  test("get") { (cache, internal) =>
    for {
      _ <- internal.expect.get(cacheKey, Some(cacheValue))
      _ <- cache.get(cacheKey).assertingEqual(Option(cacheValue).toJava)
    } yield Passed
  }

  test("contains") { (cache, internal) =>
    for {
      _ <- internal.expect.contains(cacheKey, result = true)
      _ <- cache.contains(cacheKey).assertingEqual(true)
    } yield Passed
  }

  test("remove") { (cache, internal) =>
    for {
      _ <- internal.expect.remove(cacheKey, otherKey)
      _ <- cache.remove(cacheKey, otherKey).assertingEqual(cache)
    } yield Passed
  }

  test("increment") { (cache, internal) =>
    for {
      _ <- internal.expect.increment(cacheKey, by = 1L, result = 4L)
      _ <- cache.increment(cacheKey).assertingEqual(4L)
    } yield Passed
  }

  test("increment by") { (cache, internal) =>
    for {
      _ <- internal.expect.increment(cacheKey, by = 2L, result = 6L)
      _ <- cache.increment(cacheKey, 2L).assertingEqual(6L)
    } yield Passed
  }

  test("toMap") { (cache, internal) =>
    for {
      _ <- internal.expect.toMap(cacheKey -> cacheValue)
      _ <- cache.toMap.assertingEqual(Map(cacheKey -> cacheValue).asJava)
    } yield Passed
  }

  test("keySet") { (cache, internal) =>
    for {
      _ <- internal.expect.keySet(cacheKey, otherKey)
      _ <- cache.keySet().assertingEqual(Set(cacheKey, otherKey).asJava)
    } yield Passed
  }

  test("values") { (cache, internal) =>
    for {
      _ <- internal.expect.values(otherValue, cacheValue)
      _ <- cache.values().assertingEqual(Set(otherValue, cacheValue).asJava)
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default
  )(
    f: (AsyncRedisMap[String], RedisMapMock) => Future[Assertion]
  ): Unit = {
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      val internal: RedisMapMock = mock[RedisMapMock]
      val map: AsyncRedisMap[String] = new RedisMapJavaImpl(internal)
  
      f(map, internal)
    }
  }
}
