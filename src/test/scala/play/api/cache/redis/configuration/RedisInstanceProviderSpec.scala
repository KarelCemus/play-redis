package play.api.cache.redis.configuration

import play.api.cache.redis.test.UnitSpec

class RedisInstanceProviderSpec extends UnitSpec {

  private val defaultCache: RedisStandalone =
    RedisStandalone(name = defaultCacheName, host = RedisHost(localhost, defaultPort, database = Some(0)), settings = defaultsSettings)

  implicit private val resolver: RedisInstanceResolver = new RedisInstanceResolver {

    def resolve: PartialFunction[String, RedisStandalone] = { case `defaultCacheName` =>
      defaultCache
    }

  }

  "resolve already resolved" in {
    new ResolvedRedisInstance(defaultCache).resolved mustEqual defaultCache
  }

  "resolve unresolved" in {
    new UnresolvedRedisInstance(defaultCacheName).resolved mustEqual defaultCache
  }

  "fail when not able to resolve" in {
    the[Exception] thrownBy new UnresolvedRedisInstance("other").resolved
  }

}
