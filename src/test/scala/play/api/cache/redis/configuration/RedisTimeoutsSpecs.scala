package play.api.cache.redis.configuration

import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.mutable.Specification

/**
  * @author Karel Cemus
  */
class RedisTimeoutsSpecs extends Specification {
  import Implicits._

  private def orDefault = RedisTimeouts( 1.second, None, 500.millis )

  "load defined timeouts" in new WithConfiguration(
    """
      |play.cache.redis {
      |
      |  sync-timeout:        5s
      |  redis-timeout:       7s
      |  connection-timeout:  300ms
      |}
    """
  ) {
    RedisTimeouts.load( config, "play.cache.redis" )( RedisTimeouts.requiredDefault ) mustEqual RedisTimeouts( 5.seconds, 7.seconds, 300.millis )
  }

  "load defined high timeouts" in new WithConfiguration(
    """
      |play.cache.redis {
      |
      |  sync-timeout:        500s
      |  redis-timeout:       700s
      |  connection-timeout:  900s
      |}
    """
  ) {
    RedisTimeouts.load( config, "play.cache.redis" )( RedisTimeouts.requiredDefault ) mustEqual RedisTimeouts( 500.seconds, 700.seconds, 900.seconds )
  }

  "load with default timeouts" in new WithConfiguration(
    """
      |play.cache.redis {
      |}
    """
  ) {
    RedisTimeouts.load( config, "play.cache.redis" )( orDefault ) mustEqual RedisTimeouts( 1.second, None, connection = 500.millis )
  }

  "load with disabled timeouts" in new WithConfiguration(
    """
      |play.cache.redis {
      |  redis-timeout:       null
      |  connection-timeout:  null
      |}
    """
  ) {
    RedisTimeouts.load( config, "play.cache.redis" )( orDefault ) mustEqual RedisTimeouts( sync = 1.second, redis = None, connection = None )
  }

  "load defaults" in {
    RedisTimeouts.requiredDefault.sync must throwA[ RuntimeException ]
    RedisTimeouts.requiredDefault.redis must beNone
    RedisTimeouts.requiredDefault.connection must beNone
  }
}
