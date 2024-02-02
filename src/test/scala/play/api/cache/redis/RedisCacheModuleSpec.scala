package play.api.cache.redis

import akka.actor.ActorSystem
import play.api.cache.redis.configuration.{RedisHost, RedisSettings, RedisStandalone, RedisTimeouts}
import play.api.cache.redis.test._
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.cache.NamedCacheImpl

import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag

class RedisCacheModuleSpec extends IntegrationSpec with RedisStandaloneContainer {
  import Helpers._

  final private val defaultCacheName: String = "play"

  test("bind defaults") {
    _.bindings(new RedisCacheModule).configure("play.cache.redis.port" -> container.mappedPort(defaultPort))
  } { injector =>
    injector.checkBinding[RedisConnector]
    injector.checkBinding[CacheApi]
    injector.checkBinding[CacheAsyncApi]
    injector.checkBinding[play.cache.AsyncCacheApi]
    injector.checkBinding[play.cache.SyncCacheApi]
    injector.checkBinding[play.cache.redis.AsyncCacheApi]
    injector.checkBinding[play.api.cache.AsyncCacheApi]
    injector.checkBinding[play.api.cache.SyncCacheApi]
  }

  test("not bind defaults") {
    _.bindings(new RedisCacheModule)
      .configure("play.cache.redis.bind-default" -> false)
      .configure("play.cache.redis.port" -> container.mappedPort(defaultPort))
  } { injector =>
    // bind named caches
    injector.checkNamedBinding[CacheApi]
    injector.checkNamedBinding[CacheAsyncApi]

    // but do not bind defaults
    assertThrows[com.google.inject.ConfigurationException] {
      injector.instanceOf[CacheApi]
    }
    assertThrows[com.google.inject.ConfigurationException] {
      injector.instanceOf[CacheAsyncApi]
    }
  }

  test("bind named cache in simple mode") {
    _.bindings(new RedisCacheModule)
  } { injector =>
    injector.checkNamedBinding[RedisConnector]
    injector.checkNamedBinding[CacheApi]
    injector.checkNamedBinding[CacheAsyncApi]
    injector.checkNamedBinding[play.cache.AsyncCacheApi]
    injector.checkNamedBinding[play.cache.SyncCacheApi]
    injector.checkNamedBinding[play.api.cache.AsyncCacheApi]
    injector.checkNamedBinding[play.api.cache.SyncCacheApi]
  }

  test("bind named caches") {
    _.bindings(new RedisCacheModule).configure(
      configuration.fromHocon(
        s"""
           |play.cache.redis {
           |  instances {
           |    play {
           |      host:     ${container.host}
           |      port:     ${container.mappedPort(defaultPort)}
           |      database: 1
           |    }
           |    other {
           |      host:     ${container.host}
           |      port:     ${container.mappedPort(defaultPort)}
           |      database: 2
           |      password: something
           |    }
           |  }
           |  default-cache: other
           |}
        """.stripMargin,
      ),
    )
  } { injector =>
    val other = "other"

    // something is bound to the default cache name
    injector.checkNamedBinding[RedisConnector]
    injector.checkNamedBinding[CacheApi]
    injector.checkNamedBinding[CacheAsyncApi]
    injector.checkNamedBinding[play.cache.AsyncCacheApi]
    injector.checkNamedBinding[play.cache.SyncCacheApi]
    injector.checkNamedBinding[play.api.cache.AsyncCacheApi]
    injector.checkNamedBinding[play.api.cache.SyncCacheApi]

    // something is bound to the other cache name
    injector.checkNamedBinding[RedisConnector](other)
    injector.checkNamedBinding[CacheApi](other)
    injector.checkNamedBinding[CacheAsyncApi](other)
    injector.checkNamedBinding[play.cache.AsyncCacheApi](other)
    injector.checkNamedBinding[play.cache.SyncCacheApi](other)
    injector.checkNamedBinding[play.api.cache.AsyncCacheApi](other)
    injector.checkNamedBinding[play.api.cache.SyncCacheApi](other)

    // the other cache is a default
    injector.instanceOf(binding[RedisConnector].namedCache(other)) mustEqual injector.instanceOf[RedisConnector]
    injector.instanceOf(binding[CacheApi].namedCache(other)) mustEqual injector.instanceOf[CacheApi]
    injector.instanceOf(binding[CacheAsyncApi].namedCache(other)) mustEqual injector.instanceOf[CacheAsyncApi]
    injector.instanceOf(binding[play.cache.AsyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.cache.AsyncCacheApi]
    injector.instanceOf(binding[play.cache.SyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.cache.SyncCacheApi]
    injector.instanceOf(binding[play.api.cache.AsyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.api.cache.AsyncCacheApi]
    injector.instanceOf(binding[play.api.cache.SyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.api.cache.SyncCacheApi]
  }

  test("resolve custom redis instance") {
    _.bindings(new RedisCacheModule)
      .configure("play.cache.redis.source" -> "custom")
      .bindings(binding[RedisInstance].namedCache(defaultCacheName).to(MyRedisInstance))
  } { injector =>
    injector.checkBinding[RedisConnector]
    injector.checkBinding[CacheApi]
    injector.checkBinding[CacheAsyncApi]
  }

  private lazy val MyRedisInstance: RedisStandalone =
    RedisStandalone(
      name = defaultCacheName,
      host = RedisHost(
        host = container.host,
        port = container.mappedPort(defaultPort),
        database = None,
        username = None,
        password = None,
      ),
      settings = RedisSettings(
        dispatcher = "akka.actor.default-dispatcher",
        invocationPolicy = "lazy",
        timeout = RedisTimeouts(1.second),
        recovery = "log-and-default",
        source = "my-instance",
        prefix = None,
      ),
    )

  private def binding[T: ClassTag]: BindingKey[T] =
    BindingKey(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  implicit private class RichBindingKey[T](private val key: BindingKey[T]) {
    def namedCache(name: String): BindingKey[T] = key.qualifiedWith(new NamedCacheImpl(name))
  }

  implicit private class InjectorAssertions(private val injector: Injector) {

    def checkBinding[T <: AnyRef: ClassTag]: Assertion =
      injector.instanceOf(binding[T]) mustBe a[T]

    def checkNamedBinding[T <: AnyRef: ClassTag]: Assertion =
      checkNamedBinding(defaultCacheName)

    def checkNamedBinding[T <: AnyRef: ClassTag](name: String): Assertion =
      injector.instanceOf(binding[T].namedCache(name)) mustBe a[T]

  }

  private def test(name: String)(createBuilder: GuiceApplicationBuilder => GuiceApplicationBuilder)(f: Injector => Assertion): Unit =
    s"should $name" in {

      val builder = createBuilder(new GuiceApplicationBuilder)
      val injector = builder.injector()
      val application = StoppableApplication(injector.instanceOf[ActorSystem])

      application.runInApplication {
        f(injector)
      }
    }

}
