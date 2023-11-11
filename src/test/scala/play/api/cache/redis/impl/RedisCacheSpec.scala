package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class RedisCacheSpec extends AsyncUnitSpec  with RedisRuntimeMock with RedisConnectorMock with ImplicitFutureMaterialization {
  import Helpers._

  test("get and miss") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = None)
      _ <- cache.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("get and hit") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = Some(cacheValue))
      _ <- cache.get[String](cacheKey).assertingEqual(Some(cacheValue))
    } yield Passed
  }

  test("get recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = failure)
      _ <- cache.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("get all") { (cache, connector) =>
    for {
      _ <- connector.expect.mGet[String](Seq(cacheKey, cacheKey, cacheKey), result = Seq(Some(cacheValue), None, None))
      _ <- cache.getAll[String](cacheKey, cacheKey, cacheKey).assertingEqual(Seq(Some(cacheValue), None, None))
    } yield Passed
  }

  test("get all recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.mGet[String](Seq(cacheKey, cacheKey, cacheKey), result = failure)
      _ <- cache.getAll[String](cacheKey, cacheKey, cacheKey).assertingEqual(Seq(None, None, None))
    } yield Passed
  }

  test("get all (keys in a collection)") { (cache, connector) =>
    for {
      _ <- connector.expect.mGet[String](Seq(cacheKey, cacheKey, cacheKey), result = Seq(Some(cacheValue), None, None))
      _ <- cache.getAll[String](Seq(cacheKey, cacheKey, cacheKey)).assertingEqual(Seq(Some(cacheValue), None, None))
    } yield Passed
  }

    test("set") { (cache, connector) =>
      for {
        _ <- connector.expect.set(cacheKey, cacheValue, result = true)
        _ <- cache.set(cacheKey, cacheValue).assertingDone
      } yield Passed
    }

  test("set recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.set(cacheKey, cacheValue, result = failure)
      _ <- cache.set(cacheKey, cacheValue).assertingDone
    } yield Passed
  }

  test("set if not exists (exists)") { (cache, connector) =>
    for {
      _ <- connector.expect.set(cacheKey, cacheValue, setIfNotExists=true,result = false)
      _ <- cache.setIfNotExists(cacheKey, cacheValue).assertingEqual(false)
    } yield Passed
  }

  test("set if not exists (not exists)") { (cache, connector) =>
    for {
      _ <- connector.expect.set(cacheKey, cacheValue, setIfNotExists = true, result = true)
      _ <- cache.setIfNotExists(cacheKey, cacheValue).assertingEqual(true)
    } yield Passed
  }

  test("set if not exists (exists) with expiration") { (cache, connector) =>
    for {
      _ <- connector.expect.set(cacheKey, cacheValue, cacheExpiration, setIfNotExists = true, result = false)
      _ <- cache.setIfNotExists(cacheKey, cacheValue, cacheExpiration).assertingEqual(false)
    } yield Passed
  }

  test("set if not exists (not exists) with expiration") { (cache, connector) =>
    for {
      _ <- connector.expect.set(cacheKey, cacheValue, cacheExpiration, setIfNotExists = true, result = true)
      _ <- cache.setIfNotExists(cacheKey, cacheValue, cacheExpiration).assertingEqual(true)
    } yield Passed
  }

  test("set if not exists recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.set(cacheKey, cacheValue, setIfNotExists = true, result = failure)
      _ <- cache.setIfNotExists(cacheKey, cacheValue).assertingEqual(true)
    } yield Passed
  }

  test("set all") { (cache, connector) =>
    for {
      _ <- connector.expect.mSet(Seq(cacheKey -> cacheValue))
      _ <- cache.setAll(cacheKey -> cacheValue).assertingDone
    } yield Passed
  }

  test("set all recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.mSet(Seq(cacheKey -> cacheValue), result = failure)
      _ <- cache.setAll(cacheKey -> cacheValue).assertingDone
    } yield Passed
  }

  test("set all if not exists (exists)") { (cache, connector) =>
    for {
      _ <- connector.expect.mSetIfNotExist(Seq(cacheKey -> cacheValue), result = false)
      _ <- cache.setAllIfNotExist(cacheKey -> cacheValue).assertingEqual(false)
    } yield Passed
  }

  test("set all if not exists (not exists)") { (cache, connector) =>
    for {
      _ <- connector.expect.mSetIfNotExist(Seq(cacheKey -> cacheValue), result = true)
      _ <- cache.setAllIfNotExist(cacheKey -> cacheValue).assertingEqual(true)
    } yield Passed
  }

  test("set all if not exists recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.mSetIfNotExist(Seq(cacheKey -> cacheValue), result = failure)
      _ <- cache.setAllIfNotExist(cacheKey -> cacheValue).assertingEqual(true)
    } yield Passed
  }

  test("append") { (cache, connector) =>
    for {
      _ <- connector.expect.append(cacheKey, cacheValue, result = 10L)
      _ <- cache.append(cacheKey, cacheValue).assertingDone
    } yield Passed
  }

  test("append with expiration (newly set key)") { (cache, connector) =>
    for {
      _ <- connector.expect.append(cacheKey, cacheValue, result = cacheValue.length.toLong)
      _ <- connector.expect.expire(cacheKey, cacheExpiration)
      _ <- cache.append(cacheKey, cacheValue, cacheExpiration).assertingDone
    } yield Passed
  }

  test("append with expiration (already existing key)") { (cache, connector) =>
    for {
      _ <- connector.expect.append(cacheKey, cacheValue, result = cacheValue.length.toLong + 10)
      _ <- cache.append(cacheKey, cacheValue, cacheExpiration).assertingDone
    } yield Passed
  }

  test("append recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.append(cacheKey, cacheValue, result = failure)
      _ <- cache.append(cacheKey, cacheValue).assertingDone
    } yield Passed
  }

  test("expire") { (cache, connector) =>
    for {
      _ <- connector.expect.expire(cacheKey, cacheExpiration)
      _ <- cache.expire(cacheKey, cacheExpiration).assertingDone
    } yield Passed
  }

  test("expire recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.expire(cacheKey, cacheExpiration, result = failure)
      _ <- cache.expire(cacheKey, cacheExpiration).assertingDone
    } yield Passed
  }

  test("expires in") { (cache, connector) =>
    for {
      _ <- connector.expect.expiresIn(cacheKey, result = Some(Duration("1500 ms")))
      _ <- cache.expiresIn(cacheKey).assertingEqual(Some(Duration("1500 ms")))
    } yield Passed
  }

  test("expires in recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.expiresIn(cacheKey, result = failure)
      _ <- cache.expiresIn(cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("matching") { (cache, connector) =>
    for {
      _ <- connector.expect.matching("pattern", result = Seq(cacheKey))
      _ <- cache.matching("pattern").assertingEqual(Seq(cacheKey))
    } yield Passed
  }

  test("matching recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.matching("pattern", result = failure)
      _ <- cache.matching("pattern").assertingEqual(Seq.empty)
    } yield Passed
  }

  test("matching with a prefix", prefix = Some("the-prefix")) { (cache, connector) =>
    for {
      _ <- connector.expect.matching(s"the-prefix:pattern", result = Seq(s"the-prefix:$cacheKey"))
      _ <- cache.matching("pattern").assertingEqual(Seq(cacheKey))
    } yield Passed
  }

  test("get or else (hit)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = Some(cacheValue))
      orElse = probe.orElse.const(otherValue)
      _ <- cache.getOrElse[String](cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 0
    } yield Passed
  }

  test("get or else (miss)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = None)
      orElse = probe.orElse.const(cacheValue)
      _ <- connector.expect.set(cacheKey, cacheValue, result = true)
      _ <- cache.getOrElse[String](cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else (failure)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = failure)
      orElse = probe.orElse.const(cacheValue)
      _ <- cache.getOrElse(cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or else (prefixed,miss)", prefix = Some("the-prefix")) { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](s"the-prefix:$cacheKey", result = None)
      _ <- connector.expect.set(s"the-prefix:$cacheKey", cacheValue, result = true)
      orElse = probe.orElse.const(cacheValue)
      _ <- cache.getOrElse(cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or future (hit)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = Some(cacheValue))
      orElse = probe.orElse.async(otherValue)
      _ <- cache.getOrFuture(cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 0
    } yield Passed
  }

  test("get or future (miss)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = None)
      _ <- connector.expect.set(cacheKey, cacheValue, result = true)
      orElse = probe.orElse.async(cacheValue)
      _ <- cache.getOrFuture(cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or future (failure)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = failure)
      orElse = probe.orElse.async(cacheValue)
      _ <- cache.getOrFuture(cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 1
    } yield Passed
  }

  test("get or future (failing orElse)") { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = None)
      orElse = probe.orElse.failing(failure)
      _ <- cache.getOrFuture[String](cacheKey)(orElse.execute()).assertingFailure[RedisException]
      _ = orElse.calls mustEqual 2
    } yield Passed
  }

  test("get or future (rerun)", policy = recoveryPolicy.rerun) { (cache, connector) =>
    for {
      _ <- connector.expect.get[String](cacheKey, result = None)
      _ <- connector.expect.get[String](cacheKey, result = None)
      _ <- connector.expect.set(cacheKey, cacheValue, result = true)
      orElse = probe.orElse.generic[Future[String]](failure, cacheValue, otherValue)
      _ <- cache.getOrFuture(cacheKey)(orElse.execute()).assertingEqual(cacheValue)
      _ = orElse.calls mustEqual 2
    } yield Passed
  }

  test("remove") { (cache, connector) =>
    for {
      _ <- connector.expect.remove(cacheKey)
      _ <- cache.remove(cacheKey).assertingDone
    } yield Passed
  }

  test("remove recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.remove(Seq(cacheKey), result = failure)
      _ <- cache.remove(cacheKey).assertingDone
    } yield Passed
  }

  test("remove multiple") { (cache, connector) =>
    for {
      _ <- connector.expect.remove(cacheKey, cacheKey, cacheKey, cacheKey)
      _ <- cache.remove(cacheKey, cacheKey, cacheKey, cacheKey).assertingDone
    } yield Passed
  }

  test("remove multiple recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.remove(Seq(cacheKey, cacheKey, cacheKey, cacheKey), result = failure)
      _ <- cache.remove(cacheKey, cacheKey, cacheKey, cacheKey).assertingDone
    } yield Passed
  }

  test("remove all") { (cache, connector) =>
    for {
      _ <- connector.expect.remove(cacheKey, cacheKey, cacheKey, cacheKey)
      _ <- cache.removeAll(Seq(cacheKey, cacheKey, cacheKey, cacheKey): _*).assertingDone
    } yield Passed
  }

  test("remove all recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.remove(Seq(cacheKey, cacheKey, cacheKey, cacheKey), result = failure)
      _ <- cache.removeAll(Seq(cacheKey, cacheKey, cacheKey, cacheKey): _*).assertingDone
    } yield Passed
  }

  test("remove matching") { (cache, connector) =>
    for {
      _ <- connector.expect.matching("pattern", result = Seq(cacheKey, cacheKey))
      _ <- connector.expect.remove(cacheKey, cacheKey)
      _ <- cache.removeMatching("pattern").assertingDone
    } yield Passed
  }

  test("remove matching recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.matching("pattern", result = failure)
      _ <- cache.removeMatching("pattern").assertingDone
    } yield Passed
  }

  test("invalidate") { (cache, connector) =>
    for {
      _ <- connector.expect.invalidate()
      _ <- cache.invalidate().assertingDone
    } yield Passed
  }

  test("invalidate recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.invalidate(result = failure)
      _ <- cache.invalidate().assertingDone
    } yield Passed
  }

  test("exists") { (cache, connector) =>
    for {
      _ <- connector.expect.exists(cacheKey, result = true)
      _ <- cache.exists(cacheKey).assertingEqual(true)
    } yield Passed
  }

  test("exists recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.exists(cacheKey, result = failure)
      _ <- cache.exists(cacheKey).assertingEqual(false)
    } yield Passed
  }

  test("increment") { (cache, connector) =>
    for {
      _ <- connector.expect.increment(cacheKey, 5L, result = 10L)
      _ <- cache.increment(cacheKey, 5L).assertingEqual(10L)
    } yield Passed
  }

  test("increment recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.increment(cacheKey, 5L, result = failure)
      _ <- cache.increment(cacheKey, 5L).assertingEqual(5L)
    } yield Passed
  }

  test("decrement") { (cache, connector) =>
    for {
      _ <- connector.expect.increment(cacheKey, -5L, result = 10L)
      _ <- cache.decrement(cacheKey, 5L).assertingEqual(10L)
    } yield Passed
  }

  test("decrement recover with default") { (cache, connector) =>
    for {
      _ <- connector.expect.increment(cacheKey, -5L, result = failure)
      _ <- cache.decrement(cacheKey, 5L).assertingEqual(-5L)
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default,
    prefix: Option[String] = None,
  )(
    f: (RedisCache[AsynchronousResult], RedisConnectorMock) => Future[Assertion]
  ): Unit = {
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
        prefix = prefix.fold[RedisPrefix](RedisEmptyPrefix)(new RedisPrefixImpl(_)),
      )
      val connector: RedisConnectorMock = mock[RedisConnectorMock]
      val cache: RedisCache[AsynchronousResult] = new RedisCache[AsynchronousResult](connector, Builders.AsynchronousBuilder)
      f(cache, connector)
    }
  }
 }
