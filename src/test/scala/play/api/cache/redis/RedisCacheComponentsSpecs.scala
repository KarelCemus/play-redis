package play.api.cache.redis

import play.api._
import play.api.inject.ApplicationLifecycle

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

/**
  * @author Karel Cemus
  */
class RedisCacheComponentsSpecs extends Specification with WithApplication with AfterAll {

  object components extends RedisCacheComponents {
    def actorSystem = system
    def applicationLifecycle = injector.instanceOf[ ApplicationLifecycle ]
    def environment = injector.instanceOf[ Environment ]
    def configuration = injector.instanceOf[ Configuration ]
    val syncRedis = cacheApi( "play" ).sync
  }

  private type Cache = CacheApi

  private val cache = components.syncRedis

  val prefix = "components-sync"

  "RedisComponents" should {

    "provide api" >> {
      "miss on get" in {
        cache.get[ String ]( s"$prefix-test-1" ) must beNone
      }

      "hit after set" in {
        cache.set( s"$prefix-test-2", "value" )
        cache.get[ String ]( s"$prefix-test-2" ) must beSome[ Any ]
        cache.get[ String ]( s"$prefix-test-2" ) must beSome( "value" )
      }

      "positive exists on existing keys" in {
        cache.set( s"$prefix-test-11", "value" )
        cache.exists( s"$prefix-test-11" ) must beTrue
      }
    }
  }

  def afterAll( ) = {
    components.applicationLifecycle.stop()
  }
}
