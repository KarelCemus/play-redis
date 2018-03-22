package play.api.cache.redis.configuration

import scala.concurrent.duration._

import org.specs2.mutable.Specification

class RedisInstanceSpec extends Specification {

  // settings builder
  def settings( source: String ) = RedisSettings( dispatcher = "default-dispatcher", recovery = "log-and-default", invocationPolicy = "lazy", timeout = RedisTimeouts( 1.second ), source = source )
  // default settings
  implicit val defaults = settings( source = "standalone" )
  // implicitly expose RedisHost config loader
  implicit val loader = RedisInstanceProvider.loader( "play" )
  // instance resolver
  implicit def resolver = new RedisInstanceResolver {
    val resolve: PartialFunction[ String, RedisInstance ] = {
      case name => RedisStandalone( name = s"resolved-$name", host = RedisHost( "localhost", 6380 ), settings = defaults )
    }
  }

  "RedisInstance" should "read" >> {

    "standalone" in new WithConfiguration(
      """
        |redis {
        | host:       localhost
        | port:       6379
        |}
      """
    ) {
      config.get[ RedisInstanceProvider ]( "redis" ).resolved must beEqualTo( RedisStandalone( "play", RedisHost( host = "localhost", port = 6379 ), defaults ) )
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
      config.get[ RedisInstanceProvider ]( "redis" ).resolved must beEqualTo( RedisStandalone( "play", RedisHost( host = "localhost", port = 6379 ), RedisSettings( "custom-dispatcher", invocationPolicy = "lazy", timeout = RedisTimeouts( 2.second ), "custom", "standalone" ) ) )
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
      config.get[ RedisInstanceProvider ]( "redis" ).resolved must beEqualTo( RedisCluster( "play", nodes = List( RedisHost( host = "localhost", port = 6379 ), RedisHost( host = "localhost", port = 6380 ) ), settings( source = "cluster" ) ) )
    }

    "standalone with connection string" in new WithConfiguration(
      """
        |redis {
        | source:             "connection-string"
        | connection-string:  "redis://localhost:6379"
        |}
      """
    ) {
      config.get[ RedisInstanceProvider ]( "redis" ).resolved must beEqualTo( RedisStandalone( "play", RedisHost( host = "localhost", port = 6379 ), settings( source = "connection-string" ) ) )
    }

    "custom" in new WithConfiguration(
      """
        |redis {
        | source:     custom
        |}
      """
    ) {
      config.get[ RedisInstanceProvider ]( "redis" ).resolved must beEqualTo( RedisStandalone( "resolved-play", RedisHost( host = "localhost", port = 6380 ), defaults ) )
    }
  }
}
