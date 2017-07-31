package play.api.cache.redis

import play.api._
import play.api.inject.ApplicationLifecycle

import org.specs2.mutable.Specification

/**
  * @author Karel Cemus
  */
class RedisComponentsSpecs extends Specification with Redis {

  object components extends RedisCacheComponents {
    def actorSystem = system
    def applicationLifecycle = injector.instanceOf[ ApplicationLifecycle ]
    def environment = injector.instanceOf[ Environment ]
    def configuration = injector.instanceOf[ Configuration ]

    val syncRedis = syncRedisCacheApi( "play" )
  }

  private type Cache = CacheApi

  private val Cache = components.syncRedis

  val prefix = "components-sync"

  "SynchronousCacheApi" should {

    "miss on get" in {
      Cache.get[ String ]( s"$prefix-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( s"$prefix-test-2", "value" )
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome[ Any ]
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome( "value" )
    }

    "positive exists on existing keys" in {
      Cache.set( s"$prefix-test-11", "value" )
      Cache.exists( s"$prefix-test-11" ) must beTrue
    }
  }
}
