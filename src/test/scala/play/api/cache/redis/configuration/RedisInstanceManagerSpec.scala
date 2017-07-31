package play.api.cache.redis.configuration

import scala.concurrent.duration._

import org.specs2.mutable.Specification

class RedisInstanceManagerSpec extends Specification {

  implicit val loader = RedisInstanceManager

  "RedisInstanceManager" should "read" >> {

    "multiple caches" in new WithConfiguration(
      """
        |redis {
        |  instance {
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
      """
    ) {
      val defaults = RedisSettings( dispatcher = "default-dispatcher", recovery = "log-and-default", timeout = 1.second, source = "standalone" )

      val manager = config.get[ RedisInstanceManager ]( "redis" )
      manager.caches mustEqual Set( "play", "data", "users" )

      manager.instanceOf( "play" ).instanceOption must beSome( RedisStandalone( "play", RedisHost( "localhost", 6379 ), defaults ) )
      manager.instanceOf( "users" ).instanceOption must beSome( RedisStandalone( "users", RedisHost( "localhost", 6380 ), defaults ) )
      manager.instanceOf( "data" ).instanceOption must beSome( RedisStandalone( "data", RedisHost( "localhost", 6381 ), defaults ) )
    }
  }
}
