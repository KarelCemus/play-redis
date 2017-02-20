package play.api.cache.redis.connector

import scala.concurrent.Future
import play.api.cache.redis.{Redis, RedisInstance}
import org.specs2.matcher.MustExpectable
import org.specs2.mutable.Specification

import scala.collection.immutable.List

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

    "set values" in {
      redis.mSet( Map("some-key1" -> "this-value", "some-key2" -> "that-value")) must not beNull
    }

    "get stored value" in {
      redis.get[ String ]( "some-key" ) must beSome( "this-value" )
    }

    "get stored values" in {
      redis.mGet[ String ]( "some-key1", "some-key2" ) must beEqualTo(List(Some("this-value"), Some("that-value")))
    }

    "get stored key values" in {
      redis.mGetAsMap[ String ]( "some-key1", "some-key2" ) must beEqualTo(Map("some-key1" -> "this-value", "some-key2" -> "that-value"))
    }

    "get non-existing value" in {
      redis.get[ String ]( "non-existing" ) must beNone
    }

    "determine whether it contains already stored keys" in {
      redis.exists( "some-key" ) must beTrue
      redis.exists( "some-key1" ) must beTrue
      redis.exists( "some-key2" ) must beTrue
    }

    "determine whether it contains non-existent key" in {
      redis.exists( "non-existing" ) must beFalse
    }

    "delete stored value" in {
      redis.del( "some-key", "some-key1", "some-key2" ) must beEqualTo( 3L )
    }

    "delete already deleted value" in {
      redis.del( "some-key" ) must beEqualTo( 0L )
    }

    "delete non-existing value" in {
      redis.del( "non-existing" ) must beEqualTo( 0L )
    }

    "determine whether it contains deleted keys" in {
      redis.exists( "some-key" ) must beFalse
      redis.exists( "some-key1" ) must beFalse
      redis.exists( "some-key2" ) must beFalse
    }
  }
}
