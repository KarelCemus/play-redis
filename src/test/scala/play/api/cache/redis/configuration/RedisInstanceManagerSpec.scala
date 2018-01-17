package play.api.cache.redis.configuration

import scala.concurrent.duration._

import org.specs2.mutable.Specification

class RedisInstanceManagerSpec extends Specification {

  implicit val loader = RedisInstanceManager

  implicit val resolver = new RedisInstanceResolver {
    val resolve = PartialFunction.empty
  }

  "Advanced RedisInstanceManager" should "read" >> {

    "multiple caches" in new WithConfiguration(
      """
        |redis {
        |  instances {
        |    play {
        |      host:       localhost
        |      port:       6379
        |    }
        |    users {
        |      host:       localhost
        |      port:       6380
        |    }
        |    data {
        |      host:       localhost
        |      port:       6381
        |    }
        |  }
        |
        |  dispatcher: default-dispatcher
        |  recovery:   log-and-default
        |  timeout:    1s
        |}
      """.stripMargin
    ) {
      val defaults = RedisSettings( dispatcher = "default-dispatcher", recovery = "log-and-default", timeout = 1.second, source = "standalone" )

      val manager = config.get[ RedisInstanceManager ]( "redis" )
      manager.caches mustEqual Set( "play", "data", "users" )

      manager.instanceOf( "play" ).resolved must beEqualTo( RedisStandalone( "play", RedisHost( "localhost", 6379 ), defaults ) )
      manager.instanceOf( "users" ).resolved must beEqualTo( RedisStandalone( "users", RedisHost( "localhost", 6380 ), defaults ) )
      manager.instanceOf( "data" ).resolved must beEqualTo( RedisStandalone( "data", RedisHost( "localhost", 6381 ), defaults ) )
    }
  }

  "Fallback RedisInstanceManager" should "read" >> {

    "single default cache" in new WithConfiguration(
      """
        |redis {
        |  host:          localhost
        |  port:          6380
        |
        |  default-cache: play
        |  dispatcher:    default-dispatcher
        |  recovery:      log-and-default
        |  timeout:       1s
        |}
      """.stripMargin
    ) {
      val defaults = RedisSettings( dispatcher = "default-dispatcher", recovery = "log-and-default", timeout = 1.second, source = "standalone" )

      val manager = config.get[ RedisInstanceManager ]( "redis" )
      manager.caches mustEqual Set( "play" )

      manager.instanceOf( "play" ).resolved must beEqualTo( RedisStandalone( "play", RedisHost( "localhost", 6380 ), defaults ) )
    }
  }
}
