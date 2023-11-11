package play.api.cache.redis.configuration

import scala.concurrent.duration._
import play.api.cache.redis._
import play.api.cache.redis.test.{Helpers, ImplicitOptionMaterialization, UnitSpec}

class RedisTimeoutsSpec extends UnitSpec  with ImplicitOptionMaterialization{

  private def orDefault = RedisTimeouts(1.second, None, 500.millis)

  "load defined timeouts" in {
    val configuration = Helpers.configuration.fromHocon {
      """
        |play.cache.redis {
        |
        |  sync-timeout:        5s
        |  redis-timeout:       7s
        |  connection-timeout:  300ms
        |}
    """
    }
    val expected = RedisTimeouts(5.seconds, 7.seconds, 300.millis)
    val actual = RedisTimeouts.load(configuration.underlying, "play.cache.redis")(RedisTimeouts.requiredDefault)
    actual mustEqual expected
  }

  "load defined high timeouts" in {
    val configuration = Helpers.configuration.fromHocon {
      """
        |play.cache.redis {
        |
        |  sync-timeout:        500s
        |  redis-timeout:       700s
        |  connection-timeout:  900s
        |}
    """
    }
    val expected = RedisTimeouts(500.seconds, 700.seconds, 900.seconds)
    val actual = RedisTimeouts.load(configuration.underlying, "play.cache.redis")(RedisTimeouts.requiredDefault)
    actual mustEqual expected
  }

  "load with default timeouts" in {
    val configuration = Helpers.configuration.fromHocon {
      """
        |play.cache.redis {
        |}
    """
    }
    val expected = RedisTimeouts(1.second, None, connection = 500.millis)
    val actual = RedisTimeouts.load(configuration.underlying, "play.cache.redis")(orDefault)
    actual mustEqual expected
  }

  "load with disabled timeouts" in {
    val configuration = Helpers.configuration.fromHocon {
      """
        |play.cache.redis {
        |  redis-timeout:       null
        |  connection-timeout:  null
        |}
    """
    }
    val expected = RedisTimeouts(sync = 1.second, redis = None, connection = None)
    val actual = RedisTimeouts.load(configuration.underlying, "play.cache.redis")(orDefault)
     actual mustEqual expected
  }

  "load defaults" in {
    the[RuntimeException] thrownBy RedisTimeouts.requiredDefault.sync
    RedisTimeouts.requiredDefault.redis mustEqual None
    RedisTimeouts.requiredDefault.connection mustEqual None
  }
}
