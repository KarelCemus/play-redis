package play.api.cache.redis.configuration

import scala.concurrent.duration._

import play.api.Configuration

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

class RedisInstanceSpec extends Specification {

  // settings builder
  def settings( source: String ) = RedisSettings( dispatcher = "default-dispatcher", recovery = "log-and-default", timeout = 1.second, source = source )
  // default settings
  implicit val defaults = settings( source = "standalone" )
  // implicitly expose RedisHost config loader
  implicit val loader = RedisInstanceBinder.loader( "play" )

  "RedisInstance" should "read" >> {

    "standalone" in new WithConfiguration(
      """
        |redis {
        | host:       localhost
        | port:       6379
        |}
      """
    ) {
      config.get[ RedisInstanceBinder ]( "redis" ).instanceOption must beSome( RedisStandalone( "play", RedisHost( host = "localhost", port = 6379 ), defaults ) )
    }

    "standalone overriding defaults" in new WithConfiguration(
      """
        |redis {
        | host:       localhost
        | port:       6379
        | dispatcher: custom-dispatcher
        | recovery:   custom
        | timeout:    2s
        | source:     standalone
        |}
      """
    ) {
      config.get[ RedisInstanceBinder ]( "redis" ).instanceOption must beSome( RedisStandalone( "play", RedisHost( host = "localhost", port = 6379 ), RedisSettings( "custom-dispatcher", 2.seconds, "custom", "standalone" ) ) )
    }

    "cluster" in new WithConfiguration(
      """
        |redis {
        | source:     cluster
        | cluster:    [
        |   { host: localhost, port: 6379 },
        |   { host: localhost, port: 6380 }
        | ]
        |}
      """
    ) {
      config.get[ RedisInstanceBinder ]( "redis" ).instanceOption must beSome( RedisCluster( "play", nodes = List( RedisHost( host = "localhost", port = 6379 ), RedisHost( host = "localhost", port = 6380 ) ), settings( source = "cluster" ) ) )
    }

    "standalone with connection string" in new WithConfiguration(
      """
        |redis {
        | source:             "connection-string"
        | connection-string:  "redis://localhost:6379"
        |}
      """
    ) {
      config.get[ RedisInstanceBinder ]( "redis" ).instanceOption must beSome( RedisStandalone( "play", RedisHost( host = "localhost", port = 6379 ), settings( source = "connection-string" ) ) )
    }

    "custom" in new WithConfiguration(
      """
        |redis {
        | source:     custom
        |}
      """
    ) {
      config.get[ RedisInstanceBinder ]( "redis" ).instanceOption must beNone
    }
  }
}
