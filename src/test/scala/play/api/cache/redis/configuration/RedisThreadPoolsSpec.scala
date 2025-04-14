package play.api.cache.redis.configuration

import play.api.cache.redis.test.{ Helpers, ImplicitOptionMaterialization, UnitSpec }

class RedisThreadPoolsSpec extends UnitSpec with ImplicitOptionMaterialization {

  private def orDefault = RedisThreadPools(8, 8)

  "load defined thread pools" in {
    val configuration = Helpers.configuration.fromHocon {
      """
        |play.cache.redis {
        |
        |  io-thread-pool-size:          5
        |  computation-thread-pool-size: 7
        |}
      """.stripMargin
    }
    val expected = RedisThreadPools(5, 7)
    val actual = RedisThreadPools.load(configuration.underlying, "play.cache.redis")(RedisThreadPools.requiredDefault)
    actual mustEqual expected
  }

  "load with default thread pools" in {
    val configuration = Helpers.configuration.fromHocon {
      """
        |play.cache.redis {
        |}
      """.stripMargin
    }
    val expected = RedisThreadPools(8, 8)
    val actual = RedisThreadPools.load(configuration.underlying, "play.cache.redis")(orDefault)
    actual mustEqual expected
  }

}
