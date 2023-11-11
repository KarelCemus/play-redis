package play.api.cache.redis

import akka.actor.ActorSystem
import play.api._
import play.api.cache.redis.test._
import play.api.inject.ApplicationLifecycle

class RedisCacheComponentsSpec extends IntegrationSpec with RedisStandaloneContainer {

  private  val prefix = "components-sync"

  test("miss on get") { cache =>
    cache.get[String](s"$prefix-test-1") mustEqual None
  }

  test(" hit after set") { cache =>
    cache.set(s"$prefix-test-2", "value")
    cache.get[String](s"$prefix-test-2") mustEqual Some("value")
  }

  test("return positive exists on existing keys") { cache =>
    cache.set(s"$prefix-test-11", "value")
    cache.exists(s"$prefix-test-11") mustEqual true
  }

  private def test(name: String)(cache: CacheApi => Assertion): Unit = {
    s"should $name" in {

      val components: TestComponents = new TestComponents {
        override lazy val configuration: Configuration = Helpers.configuration.fromHocon(
          s"play.cache.redis.port: ${container.mappedPort(defaultPort)}"
        )
        override lazy val actorSystem: ActorSystem = system
        override lazy val applicationLifecycle: ApplicationLifecycle = injector.instanceOf[ApplicationLifecycle]
        override lazy val environment: Environment = injector.instanceOf[Environment]
        override lazy val syncRedis: CacheApi = cacheApi("play").sync
      }

      components.runInApplication {
        cache(components.syncRedis)
      }
    }
  }

  private trait TestComponents extends RedisCacheComponents with FakeApplication {
    def syncRedis: CacheApi
  }
}
