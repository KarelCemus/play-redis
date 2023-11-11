package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.impl.Builders.AsynchronousBuilder
import play.api.cache.redis.test._

import scala.concurrent.Future

class RedisMapSpec extends AsyncUnitSpec  with RedisRuntimeMock with RedisConnectorMock with ImplicitFutureMaterialization {

  test("set") { (map, connector) =>
    for {
      _ <- connector.expect.hashSet(cacheKey, field, cacheValue, result = true)
      _ <- map.add(field, cacheValue).assertingEqual(map)
    } yield Passed
  }

  test("set (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashSet(cacheKey, field, cacheValue, result = failure)
      _ <- map.add(field, cacheValue).assertingEqual(map)
    } yield Passed
  }

  test("get") { (map, connector) =>
    for {
      _ <- connector.expect.hashGet[String](cacheKey, field, result = Some(cacheValue))
      _ <- connector.expect.hashGet[String](cacheKey, otherValue, result = None)
      _ <- map.get(field).assertingEqual(Some(cacheValue))
      _ <- map.get(otherValue).assertingEqual(None)
    } yield Passed
  }

  test("get (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashGet[String](cacheKey, field, result = failure)
      _ <- map.get(field).assertingEqual(None)
    } yield Passed
  }

  test("get fields") { (map, connector) =>
    for {
      _ <- connector.expect.hashGet[String](cacheKey, Seq(field, otherValue), result = Seq(Some(cacheValue), None))
      _ <- connector.expect.hashGet[String](cacheKey, Seq(field, otherValue), result = Seq(Some(cacheValue), None))
      _ <- map.getFields(field, otherValue).assertingEqual(Seq(Some(cacheValue), None))
      _ <- map.getFields(Seq(field, otherValue)).assertingEqual(Seq(Some(cacheValue), None))
    } yield Passed
  }

  test("get fields (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashGet[String](cacheKey, Seq(field, otherValue), result = failure)
      _ <- connector.expect.hashGet[String](cacheKey, Seq(field, otherValue), result = failure)
      _ <- map.getFields(field, otherValue).assertingEqual(Seq(None, None))
      _ <- map.getFields(Seq(field, otherValue)).assertingEqual(Seq(None, None))
    } yield Passed
  }

  test("contains") { (map, connector) =>
    for {
      _ <- connector.expect.hashExists(cacheKey, field, result = true)
      _ <- connector.expect.hashExists(cacheKey, otherValue, result = false)
      _ <- map.contains(field).assertingEqual(true)
      _ <- map.contains(otherValue).assertingEqual(false)
    } yield Passed
  }

  test("contains (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashExists(cacheKey, field, result = failure)
      _ <- map.contains(field).assertingEqual(false)
    } yield Passed
  }

  test("remove") { (map, connector) =>
    for {
      _ <- connector.expect.hashRemove(cacheKey, Seq(field))
      _ <- connector.expect.hashRemove(cacheKey, Seq(field, otherValue))
      _ <- map.remove(field).assertingEqual(map)
      _ <- map.remove(field, otherValue).assertingEqual(map)
    } yield Passed
  }

  test("remove (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashRemove(cacheKey, Seq(field), result = failure)
      _ <- map.remove(field).assertingEqual(map)
    } yield Passed
  }

  test("increment") { (map, connector) =>
    for {
      _ <- connector.expect.hashIncrement(cacheKey, field, 2L, result = 5L)
      _ <- map.increment(field, 2L).assertingEqual(5L)
    } yield Passed
  }

  test("increment (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashIncrement(cacheKey, field, 2L, result = failure)
      _ <- map.increment(field, 2L).assertingEqual(2L)
    } yield Passed
  }

  test("toMap") { (map, connector) =>
    for {
      _ <- connector.expect.hashGetAll[String](cacheKey, result = Map(field -> cacheValue))
      _ <- map.toMap.assertingEqual(Map(field -> cacheValue))
    } yield Passed
  }

  test("toMap (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashGetAll[String](cacheKey, result = failure)
      _ <- map.toMap.assertingEqual(Map.empty)
    } yield Passed
  }

  test("keySet") { (map, connector) =>
    for {
      _ <- connector.expect.hashKeys(cacheKey, result = Set(field))
      _ <- map.keySet.assertingEqual(Set(field))
    } yield Passed
  }

  test("keySet (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashKeys(cacheKey, result = failure)
      _ <- map.keySet.assertingEqual(Set.empty)
    } yield Passed
  }

  test("values") { (map, connector) =>
    for {
      _ <- connector.expect.hashValues[String](cacheKey, result = Set(cacheValue))
      _ <- map.values.assertingEqual(Set(cacheValue))
    } yield Passed
  }

  test("values (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashValues[String](cacheKey, result = failure)
      _ <- map.values.assertingEqual(Set.empty)
    } yield Passed
  }

  test("size") { (map, connector) =>
    for {
      _ <- connector.expect.hashSize(cacheKey, result = 2L)
      _ <- map.size.assertingEqual(2L)
    } yield Passed
  }

  test("size (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashSize(cacheKey, result = failure)
      _ <- map.size.assertingEqual(0L)
    } yield Passed
  }

  test("empty map") { (map, connector) =>
    for {
      _ <- connector.expect.hashSize(cacheKey, result = 0L)
      _ <- connector.expect.hashSize(cacheKey, result = 0L)
      _ <- map.isEmpty.assertingEqual(true)
      _ <- map.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  test("non-empty map") { (map, connector) =>
    for {
      _ <- connector.expect.hashSize(cacheKey, result = 1L)
      _ <- connector.expect.hashSize(cacheKey, result = 1L)
      _ <- map.isEmpty.assertingEqual(false)
      _ <- map.nonEmpty.assertingEqual(true)
    } yield Passed
  }

  test("empty/non-empty map (failing)") { (map, connector) =>
    for {
      _ <- connector.expect.hashSize(cacheKey, result = failure)
      _ <- connector.expect.hashSize(cacheKey, result = failure)
      _ <- map.isEmpty.assertingEqual(true)
      _ <- map.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default
  )(
    f: (RedisMap[String, AsynchronousResult], RedisConnectorMock) => Future[Assertion]
  ): Unit = {
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      implicit val builder: Builders.AsynchronousBuilder.type = AsynchronousBuilder
      val connector: RedisConnectorMock = mock[RedisConnectorMock]
      val map: RedisMap[String, AsynchronousResult] = new RedisMapImpl[String, AsynchronousResult](cacheKey, connector)
      f(map, connector)
    }
  }
 }
