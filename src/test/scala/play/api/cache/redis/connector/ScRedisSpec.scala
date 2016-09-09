package play.api.cache.redis.connector

import scala.concurrent.Future

import play.api.cache.redis.{Redis, RedisInstance}

import org.specs2.matcher.MustExpectable
import org.specs2.mutable.Specification

/**
 * <p>Test of brando to be sure that it works etc.</p>
 */
class ScRedisSpec extends Specification with Redis with RedisInstance {

  sequential

  private implicit def theFutureValue[ T ]( t: => Future[ T ] ): MustExpectable[ T ] = createMustExpectable( t.sync )

  "ScRedis" should {

    "ping" in {
      redis.ping( ) must beEqualTo( "PONG" )
    }

    "set value" in {
      redis.set( "some-key", "this-value" ) must beTrue
    }

    "get stored value" in {
      redis.get[ String ]( "some-key" ) must beSome( "this-value" )
    }

    "get non-existing value" in {
      redis.get[ String ]( "non-existing" ) must beNone
    }

    "determine whether it contains already stored key" in {
      redis.exists( "some-key" ) must beTrue
    }

    "determine whether it contains non-existent key" in {
      redis.exists( "non-existing" ) must beFalse
    }

    "delete stored value" in {
      redis.del( "some-key" ) must beEqualTo( 1L )
    }

    "delete already deleted value" in {
      redis.del( "some-key" ) must beEqualTo( 0L )
    }

    "delete non-existing value" in {
      redis.del( "non-existing" ) must beEqualTo( 0L )
    }

    "determine whether it contains deleted key" in {
      redis.exists( "some-key" ) must beFalse
    }
  }
}
