package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._

import scala.concurrent.Future
import scala.concurrent.duration._

class AsyncRedisSpec extends AsyncUnitSpec with RedisConnectorMock with RedisRuntimeMock with ImplicitFutureMaterialization {
  import Helpers._

  test("removeAll") { (connector, cache) =>
    for {
      _ <- connector.expect.invalidate()
      _ <- cache.removeAll().assertingDone
    } yield Passed
  }

  test("getOrElseUpdate (hit)") { (connector, cache) =>
    for {
      _ <- connector.expect.get(cacheKey, Some(cacheValue))
      orElse = probe.orElse.async(cacheValue)
      _ <- cache.getOrElseUpdate[String](cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 0
    } yield Passed
  }

  test("getOrElseUpdate (miss)") { (connector, cache) =>
    for {
      _ <- connector.expect.get[String](cacheKey, None)
      _ <- connector.expect.set(cacheKey, cacheValue, Duration.Inf, result = true)
      orElse = probe.orElse.async(cacheValue)
      _ <- cache.getOrElseUpdate[String](cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("getOrElseUpdate (failure)") { (connector, cache) =>
    for {
      _ <- connector.expect.get[String](cacheKey, SimulatedException.asRedis)
      orElse = probe.orElse.async(cacheValue)
      _ <- cache.getOrElseUpdate[String](cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("getOrElseUpdate (failing orElse)") { (connector, cache) =>
    for {
      _ <- connector.expect.get[String](cacheKey, None)
      orElse = probe.orElse.failing(SimulatedException.asRedis)
      _ <- cache.getOrElseUpdate[String](cacheKey)(orElse.execute()).assertingFailure[TimeoutException]
      _ = orElse.calls mustEqual 2
    } yield Passed
  }

  test("getOrElseUpdate (rerun)", policy = recoveryPolicy.rerun) { (connector, cache) =>
    for {
      _ <- connector.expect.get[String](cacheKey, None)
      _ <- connector.expect.get[String](cacheKey, None)
      _ <- connector.expect.set(cacheKey, cacheValue, Duration.Inf, result = true)
      orElse = probe
                 .orElse
                 .generic(
                   Future.failed(SimulatedException.asRedis),
                   Future.successful(cacheValue),
                 )
      _ <- cache.getOrElseUpdate[String](cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 2
    } yield Passed
  }

  private def test(name: String, policy: RecoveryPolicy = recoveryPolicy.default)(f: (RedisConnectorMock, AsyncRedis) => Future[Assertion]): Unit =
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      val connector = mock[RedisConnectorMock]
      val cache: AsyncRedis = new AsyncRedisImpl(connector)

      f(connector, cache)
    }

}
