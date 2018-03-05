package play.api.cache.redis.configuration

import scala.concurrent.duration._

import play.api.Configuration

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

class RedisSettingsSpec extends Specification {

  // implicitly expose RedisHost config loader
  implicit val loader = RedisSettings

  // default settings
  val defaults = RedisSettings( dispatcher = "default-dispatcher", invocationPolicy = "lazy", recovery = "log-and-default", timeout = RedisTimeouts( 1.second ), source = "standalone" )

  "RedisSettings" should "read" >> {

    "defaults" in new WithConfiguration(
      """
        |redis {
        | dispatcher: default-dispatcher
        | recovery:   log-and-default
        | invocation: lazy
        | timeout:    1s
        | source:     standalone
        |}
      """
    ) {
      config.get[ RedisSettings ]( "redis" ) mustEqual defaults
    }

    "empty but with fallback" in new WithConfiguration(
      """
        |redis {
        |}
      """
    ) {
      config.get( "redis" )( RedisSettings.withFallback( defaults ) ) mustEqual RedisSettings( dispatcher = "default-dispatcher", invocationPolicy = "lazy", recovery = "log-and-default", timeout = RedisTimeouts( 1.second ), source = "standalone" )
    }

    "filled with fallback" in new WithConfiguration(
      """
        |redis {
        | dispatcher: custom-dispatcher
        | recovery:   custom
        | invocation: lazy
        | timeout:    2s
        | source:     cluster
        |}
      """
    ) {
      config.get( "redis" )( RedisSettings.withFallback( defaults ) ) mustEqual RedisSettings( dispatcher = "custom-dispatcher", invocationPolicy = "lazy", recovery = "custom", timeout = RedisTimeouts( 2.second ), source = "cluster" )
    }
  }
}
