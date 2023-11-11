package play.api.cache.redis.impl

import play.api.cache.redis.test._

import scala.concurrent.Future

class RedisPrefixSpec extends AsyncUnitSpec with RedisRuntimeMock with RedisConnectorMock with ImplicitFutureMaterialization {

  test("apply when defined", prefix = new RedisPrefixImpl("prefix")) { (connector, cache) =>
    for {
      // get one
      _ <- connector.expect.get[String](s"prefix:$cacheKey", None)
      _ <- cache.get[String](cacheKey).assertingEqual(None)
      // get multiple
      _ <- connector.expect.mGet[String](Seq(s"prefix:$cacheKey", s"prefix:$otherKey"), Seq(None, Some(cacheValue)))
      _ <- cache.getAll[String](cacheKey, otherKey).assertingEqual(Seq(None, Some(cacheValue)))
    } yield Passed
  }

  test("not apply when is empty", prefix = RedisEmptyPrefix) { (connector, cache) =>
    for {
      // get one
      _ <- connector.expect.get[String](cacheKey, None)
      _ <- cache.get[String](cacheKey).assertingEqual(None)
      // get multiple
      _ <- connector.expect.mGet[String](Seq(cacheKey, otherKey), Seq(None, Some(cacheValue)))
      _ <- cache.getAll[String](cacheKey, otherKey).assertingEqual(Seq(None, Some(cacheValue)))
    } yield Passed
  }

  private def test(
                    name: String,
                    prefix: RedisPrefix
                  )(
    f: (RedisConnectorMock, AsyncRedis) => Future[Assertion]
  ): Unit = {
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(prefix = prefix)
      val connector = mock[RedisConnectorMock]
      val cache: AsyncRedis = new AsyncRedisImpl(connector)
      f(connector, cache)
    }
  }
}
