package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._

import scala.concurrent.Future

class SyncRedisSpec extends AsyncUnitSpec  with RedisRuntimeMock with RedisConnectorMock with ImplicitFutureMaterialization {
  import Helpers._

  test("get or else (hit)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = Some(cacheValue))
      orElse = probe.orElse.const(otherValue)
      _ = cache.getOrElse(cacheKey)(orElse.execute()) mustEqual cacheValue
      _ = orElse.calls mustEqual 0
    } yield Passed
  }

  test("get or else (miss)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = None)
      _ <- connector.expect.set(cacheKey, cacheValue, result = true)
      orElse = probe.orElse.const(cacheValue)
      _ = cache.getOrElse(cacheKey)(orElse.execute()) mustEqual cacheValue
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else (failure in get)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = failure)
      _ <- connector.expect.set(cacheKey, cacheValue, result = true)
      orElse = probe.orElse.const(cacheValue)
      _ = cache.getOrElse(cacheKey)(orElse.execute()) mustEqual cacheValue
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else (failure in set)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = None)
      _ <- connector.expect.set(cacheKey, cacheValue, result = failure)
      orElse = probe.orElse.const(cacheValue)
      _ = cache.getOrElse(cacheKey)(orElse.execute()) mustEqual cacheValue
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else (failure in orElse)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = failure)
      _ <- connector.expect.set(cacheKey, cacheValue, result = true)
      orElse = probe.orElse.const(cacheValue)
      _ = cache.getOrElse(cacheKey)(orElse.execute()) mustEqual cacheValue
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else (prefixed,miss)", prefix = Some("the-prefix")) { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](s"the-prefix:$cacheKey", result = None)
      _ <- connector.expect.set(s"the-prefix:$cacheKey", cacheValue, result = true)
      orElse = probe.orElse.const(cacheValue)
      _ = cache.getOrElse(cacheKey)(orElse.execute()) mustEqual cacheValue
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default,
    prefix: Option[String] = None,
  )(
    f: (RedisCache[SynchronousResult], RedisConnectorMock) => Future[Assertion]
  ): Unit = {
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
        prefix = prefix.fold[RedisPrefix](RedisEmptyPrefix)(new RedisPrefixImpl(_)),
      )
      val connector: RedisConnectorMock = mock[RedisConnectorMock]
      val cache: RedisCache[SynchronousResult] = new SyncRedis(connector)
      f(cache, connector)
    }
  }
 }
