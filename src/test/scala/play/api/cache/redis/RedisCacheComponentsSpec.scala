package play.api.cache.redis

import play.api._
import play.api.inject.ApplicationLifecycle
import org.specs2.mutable.Specification

class RedisCacheComponentsSpec extends Specification with WithApplication with StandaloneRedisContainer {
  import Implicits._

  object components extends RedisCacheComponents with WithHocon {
    def actorSystem = system
    def applicationLifecycle = injector.instanceOf[ApplicationLifecycle]
    def environment = injector.instanceOf[Environment]
    lazy val syncRedis = cacheApi("play").sync
    override lazy val configuration = Configuration(config)
    override protected def hocon: String = s"play.cache.redis.port: ${container.mappedPort(defaultPort)}"
  }

  private type Cache = CacheApi

  private lazy val cache = components.syncRedis

  val prefix = "components-sync"

  "RedisComponents" should {

    "provide api" >> {
      "miss on get" in {
        cache.get[String](s"$prefix-test-1") must beNone
      }

      "hit after set" in {
        cache.set(s"$prefix-test-2", "value")
        cache.get[String](s"$prefix-test-2") must beSome[Any]
        cache.get[String](s"$prefix-test-2") must beSome("value")
      }

      "positive exists on existing keys" in {
        cache.set(s"$prefix-test-11", "value")
        cache.exists(s"$prefix-test-11") must beTrue
      }
    }
  }

  override def afterAll() = {
    Shutdown.run.awaitForFuture
    super.afterAll()
  }
}
