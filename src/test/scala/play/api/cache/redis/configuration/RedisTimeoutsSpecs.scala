package play.api.cache.redis.configuration

import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.mutable.Spec

/**
  * @author Karel Cemus
  */
class RedisTimeoutsSpecs extends Spec {
  import Implicits._

  private def orDefault = RedisTimeouts( 1.second, 2.seconds )

  "load defined timeouts" in new WithConfiguration(
    """
      |play.cache.redis {
      |
      |  sync-timeout:    5s
      |  redis-timeout:   7s
      |}
    """
  ) {
    RedisTimeouts.load( config, "play.cache.redis" )( RedisTimeouts.requiredDefault ) mustEqual RedisTimeouts( 5.seconds, 7.seconds )
  }

  "load with default timeouts" in new WithConfiguration(
    """
      |play.cache.redis {
      |}
    """
  ) {
    RedisTimeouts.load( config, "play.cache.redis" )( orDefault ) mustEqual RedisTimeouts( 1.second, 2.seconds )
  }

  "load deprecated timeout option" in new WithConfiguration(
    """
      |play.cache.redis {
      |  timeout: 5s
      |}
    """
  ) {
    RedisTimeouts.load( config, "play.cache.redis" )( orDefault ) mustEqual RedisTimeouts( 5.second, 2.seconds )
  }

  "load defaults" in {
    RedisTimeouts.requiredDefault.sync must throwA[ RuntimeException ]
    RedisTimeouts.requiredDefault.redis must beNone
  }
}
