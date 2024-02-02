package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._
import play.cache.redis.AsyncRedisSet

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class RedisJavaSetSpec extends AsyncUnitSpec with RedisSetJavaMock with RedisRuntimeMock {

  test("add") { (set, internal) =>
    for {
      _ <- internal.expect.add(cacheKey, cacheValue)
      _ <- set.add(cacheKey, cacheValue).assertingEqual(set)
    } yield Passed
  }


  test("contains") { (set, internal) =>
    for {
      _ <- internal.expect.contains(cacheKey, result = true)
      _ <- set.contains(cacheKey).assertingEqual(true)
    } yield Passed
  }

  test("remove") { (set, internal) =>
    for {
      _ <- internal.expect.remove(cacheKey, cacheValue)
      _ <- set.remove(cacheKey, cacheValue).assertingEqual(set)
    } yield Passed
  }

  test("toSet") { (set, internal) =>
    for {
      _ <- internal.expect.toSet(cacheKey, cacheValue)
      _ <- set.toSet.assertingEqual(Set(cacheKey, cacheValue).asJava)
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default
  )(
    f: (AsyncRedisSet[String], RedisSetMock) => Future[Assertion]
  ): Unit = {
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      val internal: RedisSetMock = mock[RedisSetMock]
      val set: AsyncRedisSet[String] = new RedisSetJavaImpl(internal)

      f(set, internal)
    }
  }
}
