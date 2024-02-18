package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.impl.Builders.AsynchronousBuilder
import play.api.cache.redis.test._

import scala.concurrent.Future

class RedisSetSpec extends AsyncUnitSpec with RedisRuntimeMock with RedisConnectorMock with ImplicitFutureMaterialization {

  test("add") { (set, connector) =>
    for {
      _ <- connector.expect.setAdd(cacheKey, Seq(cacheValue))
      _ <- connector.expect.setAdd(cacheKey, Seq(cacheValue, otherValue))
      _ <- set.add(cacheValue).assertingEqual(set)
      _ <- set.add(cacheValue, otherValue).assertingEqual(set)
    } yield Passed
  }

  test("add (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.setAdd(cacheKey, Seq(cacheValue), result = failure)
      _ <- set.add(cacheValue).assertingEqual(set)
    } yield Passed
  }

  test("contains") { (set, connector) =>
    for {
      _ <- connector.expect.setIsMember(cacheKey, cacheValue, result = true)
      _ <- connector.expect.setIsMember(cacheKey, otherValue, result = false)
      _ <- set.contains(cacheValue).assertingEqual(true)
      _ <- set.contains(otherValue).assertingEqual(false)
    } yield Passed
  }

  test("contains (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.setIsMember(cacheKey, cacheValue, result = failure)
      _ <- set.contains(cacheValue).assertingEqual(false)
    } yield Passed
  }

  test("remove") { (set, connector) =>
    for {
      _ <- connector.expect.setRemove(cacheKey, Seq(cacheValue))
      _ <- connector.expect.setRemove(cacheKey, Seq(otherValue, cacheValue))
      _ <- set.remove(cacheValue).assertingEqual(set)
      _ <- set.remove(otherValue, cacheValue).assertingEqual(set)
    } yield Passed
  }

  test("remove (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.setRemove(cacheKey, Seq(cacheValue), result = failure)
      _ <- set.remove(cacheValue).assertingEqual(set)
    } yield Passed
  }

  test("toSet") { (set, connector) =>
    for {
      _ <- connector.expect.setMembers[String](cacheKey, result = Set(cacheValue, otherValue))
      _ <- set.toSet.assertingEqual(Set(cacheValue, otherValue))
    } yield Passed
  }

  test("toSet (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.setMembers[String](cacheKey, result = failure)
      _ <- set.toSet.assertingEqual(Set.empty)
    } yield Passed
  }

  test("size") { (set, connector) =>
    for {
      _ <- connector.expect.setSize(cacheKey, result = 2L)
      _ <- set.size.assertingEqual(2L)
    } yield Passed
  }

  test("size (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.setSize(cacheKey, result = failure)
      _ <- set.size.assertingEqual(0L)
    } yield Passed
  }

  test("empty set") { (set, connector) =>
    for {
      _ <- connector.expect.setSize(cacheKey, result = 0L)
      _ <- connector.expect.setSize(cacheKey, result = 0L)
      _ <- set.isEmpty.assertingEqual(true)
      _ <- set.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  test("non-empty set") { (set, connector) =>
    for {
      _ <- connector.expect.setSize(cacheKey, result = 1L)
      _ <- connector.expect.setSize(cacheKey, result = 1L)
      _ <- set.isEmpty.assertingEqual(false)
      _ <- set.nonEmpty.assertingEqual(true)
    } yield Passed
  }

  test("empty/non-empty set (failing)") { (set, connector) =>
    for {
      _ <- connector.expect.setSize(cacheKey, result = failure)
      _ <- connector.expect.setSize(cacheKey, result = failure)
      _ <- set.isEmpty.assertingEqual(true)
      _ <- set.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default,
  )(
    f: (RedisSet[String, AsynchronousResult], RedisConnectorMock) => Future[Assertion],
  ): Unit =
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      implicit val builder: Builders.AsynchronousBuilder.type = AsynchronousBuilder
      val connector: RedisConnectorMock = mock[RedisConnectorMock]
      val set: RedisSet[String, AsynchronousResult] = new RedisSetImpl[String, AsynchronousResult](cacheKey, connector)
      f(set, connector)
    }

}
