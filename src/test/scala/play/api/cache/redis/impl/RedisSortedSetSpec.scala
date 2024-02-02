package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.impl.Builders.AsynchronousBuilder
import play.api.cache.redis.test._

import scala.concurrent.Future

class RedisSortedSetSpec extends AsyncUnitSpec with RedisRuntimeMock with RedisConnectorMock with ImplicitFutureMaterialization {

  private val scoreValue: (Double, String) = (1.0, "cacheValue")
  private val otherScoreValue: (Double, String) = (2.0, "other")

  test("add") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetAdd(cacheKey, Seq(scoreValue))
      _ <- connector.expect.sortedSetAdd(cacheKey, Seq(scoreValue, otherScoreValue))
      _ <- set.add(scoreValue).assertingEqual(set)
      _ <- set.add(scoreValue, otherScoreValue).assertingEqual(set)
    } yield Passed
  }

  test("add (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetAdd(cacheKey, Seq(scoreValue), result = failure)
      _ <- set.add(scoreValue).assertingEqual(set)
    } yield Passed
  }

  test("contains (hit)") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetScore(cacheKey, otherValue, result = Some(1d))
      _ <- set.contains(otherValue).assertingEqual(true)
    } yield Passed
  }

  test("contains (miss)") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetScore(cacheKey, otherValue, result = None)
      _ <- set.contains(otherValue).assertingEqual(false)
    } yield Passed
  }

  test("remove") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetRemove(cacheKey, Seq(cacheValue))
      _ <- connector.expect.sortedSetRemove(cacheKey, Seq(otherValue, cacheValue))
      _ <- set.remove(cacheValue).assertingEqual(set)
      _ <- set.remove(otherValue, cacheValue).assertingEqual(set)
    } yield Passed
  }

  test("remove (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetRemove(cacheKey, Seq(cacheValue), result = failure)
      _ <- set.remove(cacheValue).assertingEqual(set)
    } yield Passed
  }

  test("range") { (set, connector) =>
    val data = Seq(cacheValue, otherValue)
    for {
      _ <- connector.expect.sortedSetRange[String](cacheKey, 1, 5, result = data)
      _ <- set.range(1, 5).assertingEqual(data)
    } yield Passed
  }

  test("range (reversed)") { (set, connector) =>
    val data = Seq(cacheValue, otherValue)
    for {
      _ <- connector.expect.sortedSetReverseRange[String](cacheKey, 1, 5, result = data)
      _ <- set.range(1, 5, isReverse = true).assertingEqual(data)
    } yield Passed
  }

  test("size") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetSize(cacheKey, result = 2L)
      _ <- set.size.assertingEqual(2L)
    } yield Passed
  }

  test("size (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetSize(cacheKey, result = failure)
      _ <- set.size.assertingEqual(0L)
    } yield Passed
  }

  test("empty set") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetSize(cacheKey, result = 0L)
      _ <- connector.expect.sortedSetSize(cacheKey, result = 0L)
      _ <- set.isEmpty.assertingEqual(true)
      _ <- set.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  test("non-empty set") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetSize(cacheKey, result = 1L)
      _ <- connector.expect.sortedSetSize(cacheKey, result = 1L)
      _ <- set.isEmpty.assertingEqual(false)
      _ <- set.nonEmpty.assertingEqual(true)
    } yield Passed
  }

  test("empty/non-empty set (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.sortedSetSize(cacheKey, result = failure)
      _ <- connector.expect.sortedSetSize(cacheKey, result = failure)
      _ <- set.isEmpty.assertingEqual(true)
      _ <- set.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default,
  )(
    f: (RedisSortedSet[String, AsynchronousResult], RedisConnectorMock) => Future[Assertion],
  ): Unit =
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      implicit val builder: Builders.AsynchronousBuilder.type = AsynchronousBuilder
      val connector: RedisConnectorMock = mock[RedisConnectorMock]
      val set: RedisSortedSet[String, AsynchronousResult] = new RedisSortedSetImpl[String, AsynchronousResult](cacheKey, connector)
      f(set, connector)
    }

}
