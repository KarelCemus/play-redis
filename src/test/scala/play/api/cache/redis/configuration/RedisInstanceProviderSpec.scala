package play.api.cache.redis.configuration

import org.specs2.mutable.Specification

class RedisInstanceProviderSpec extends Specification {
  import play.api.cache.redis.Implicits._

  val defaultCache = RedisStandalone(defaultCacheName, RedisHost(localhost, defaultPort, database = 0), defaults)

  implicit val resolver = new RedisInstanceResolver {
    def resolve = {
      case `defaultCacheName` => defaultCache
    }
  }

  "resolve already resolved" in {
    new ResolvedRedisInstance(defaultCache).resolved mustEqual defaultCache
  }

  "resolve unresolved" in {
    new UnresolvedRedisInstance(defaultCacheName).resolved mustEqual defaultCache
  }

  "fail when not able to resolve" in {
    new UnresolvedRedisInstance("other").resolved must throwA[Exception]
  }
}
