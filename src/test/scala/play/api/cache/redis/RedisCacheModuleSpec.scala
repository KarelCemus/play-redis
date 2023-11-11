package play.api.cache.redis

import javax.inject.Provider

import scala.concurrent.duration._
import scala.reflect.ClassTag

import play.api.inject._

import akka.actor.ActorSystem
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._
import org.specs2.specification.Scope

/**
  * <p>This specification tests expiration conversion</p>
  */
class RedisCacheModuleSpec extends Specification {

  import Implicits._
  import RedisCacheModuleSpec._

  "RedisCacheModule" should {

    "bind defaults" in new WithApplication with Scope with Around {
      override protected def builder = super.builder.bindings(new RedisCacheModule)
      def around[T: AsResult](t: => T): Result = runAndStop(t)

      injector.instanceOf[RedisConnector] must beAnInstanceOf[RedisConnector]
      injector.instanceOf[CacheApi] must beAnInstanceOf[CacheApi]
      injector.instanceOf[CacheAsyncApi] must beAnInstanceOf[CacheAsyncApi]
      injector.instanceOf[play.cache.AsyncCacheApi] must beAnInstanceOf[play.cache.AsyncCacheApi]
      injector.instanceOf[play.cache.SyncCacheApi] must beAnInstanceOf[play.cache.SyncCacheApi]
      injector.instanceOf[play.cache.redis.AsyncCacheApi] must beAnInstanceOf[play.cache.redis.AsyncCacheApi]
      injector.instanceOf[play.api.cache.AsyncCacheApi] must beAnInstanceOf[play.api.cache.AsyncCacheApi]
      injector.instanceOf[play.api.cache.SyncCacheApi] must beAnInstanceOf[play.api.cache.SyncCacheApi]
    }

    "not bind defaults" in new WithHocon with WithApplication with Scope with Around {
      override protected def builder = super.builder.bindings(new RedisCacheModule).configure(configuration)
      def around[T: AsResult](t: => T): Result = runAndStop(t)
      protected def hocon = "play.cache.redis.bind-default: false"

      // bind named caches
      injector.instanceOf(binding[CacheApi].namedCache(defaultCacheName)) must beAnInstanceOf[CacheApi]
      injector.instanceOf(binding[CacheAsyncApi].namedCache(defaultCacheName)) must beAnInstanceOf[CacheAsyncApi]

      // but do not bind defaults
      injector.instanceOf[CacheApi] must throwA[com.google.inject.ConfigurationException]
      injector.instanceOf[CacheAsyncApi] must throwA[com.google.inject.ConfigurationException]
    }

    "bind named cache in simple mode" in new WithApplication with Scope with Around {
      override protected def builder = super.builder.bindings(new RedisCacheModule)
      def around[T: AsResult](t: => T): Result = runAndStop(t)
      def checkBinding[T <: AnyRef: ClassTag] = {
        injector.instanceOf(binding[T].namedCache(defaultCacheName)) must beAnInstanceOf[T]
      }

      checkBinding[RedisConnector]
      checkBinding[CacheApi]
      checkBinding[CacheAsyncApi]
      checkBinding[play.cache.AsyncCacheApi]
      checkBinding[play.cache.SyncCacheApi]
      checkBinding[play.api.cache.AsyncCacheApi]
      checkBinding[play.api.cache.SyncCacheApi]
    }

    "bind named caches" in new WithHocon with WithApplication with Scope with Around {
      override protected def builder = super.builder.bindings(new RedisCacheModule).configure(configuration)
      def around[T: AsResult](t: => T): Result = runAndStop(t)
      protected def hocon =
        """
          |play.cache.redis {
          |  instances {
          |
          |    play {
          |      host:     localhost
          |      port:     6379
          |      database: 1
          |    }
          |
          |    other {
          |      host:     redis.localhost.cz
          |      port:     6378
          |      database: 2
          |      password: something
          |    }
          |  }
          |
          |  default-cache: other
          |}
        """.stripMargin
      val other = "other"

      // something is bound to the default cache name
      injector.instanceOf(binding[RedisConnector].namedCache(defaultCacheName)) must beAnInstanceOf[RedisConnector]
      injector.instanceOf(binding[CacheApi].namedCache(defaultCacheName)) must beAnInstanceOf[CacheApi]
      injector.instanceOf(binding[CacheAsyncApi].namedCache(defaultCacheName)) must beAnInstanceOf[CacheAsyncApi]
      injector.instanceOf(binding[play.cache.AsyncCacheApi].namedCache(defaultCacheName)) must beAnInstanceOf[play.cache.AsyncCacheApi]
      injector.instanceOf(binding[play.cache.SyncCacheApi].namedCache(defaultCacheName)) must beAnInstanceOf[play.cache.SyncCacheApi]
      injector.instanceOf(binding[play.api.cache.AsyncCacheApi].namedCache(defaultCacheName)) must beAnInstanceOf[play.api.cache.AsyncCacheApi]
      injector.instanceOf(binding[play.api.cache.SyncCacheApi].namedCache(defaultCacheName)) must beAnInstanceOf[play.api.cache.SyncCacheApi]

      // something is bound to the other cache name
      injector.instanceOf(binding[RedisConnector].namedCache(other)) must beAnInstanceOf[RedisConnector]
      injector.instanceOf(binding[CacheApi].namedCache(other)) must beAnInstanceOf[CacheApi]
      injector.instanceOf(binding[CacheAsyncApi].namedCache(other)) must beAnInstanceOf[CacheAsyncApi]
      injector.instanceOf(binding[play.cache.AsyncCacheApi].namedCache(other)) must beAnInstanceOf[play.cache.AsyncCacheApi]
      injector.instanceOf(binding[play.cache.SyncCacheApi].namedCache(other)) must beAnInstanceOf[play.cache.SyncCacheApi]
      injector.instanceOf(binding[play.api.cache.AsyncCacheApi].namedCache(other)) must beAnInstanceOf[play.api.cache.AsyncCacheApi]
      injector.instanceOf(binding[play.api.cache.SyncCacheApi].namedCache(other)) must beAnInstanceOf[play.api.cache.SyncCacheApi]

      // the other cache is a default
      injector.instanceOf(binding[RedisConnector].namedCache(other)) mustEqual injector.instanceOf[RedisConnector]
      injector.instanceOf(binding[CacheApi].namedCache(other)) mustEqual injector.instanceOf[CacheApi]
      injector.instanceOf(binding[CacheAsyncApi].namedCache(other)) mustEqual injector.instanceOf[CacheAsyncApi]
      injector.instanceOf(binding[play.cache.AsyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.cache.AsyncCacheApi]
      injector.instanceOf(binding[play.cache.SyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.cache.SyncCacheApi]
      injector.instanceOf(binding[play.api.cache.AsyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.api.cache.AsyncCacheApi]
      injector.instanceOf(binding[play.api.cache.SyncCacheApi].namedCache(other)) mustEqual injector.instanceOf[play.api.cache.SyncCacheApi]
    }

    "resolve custom redis instance" in new WithHocon with WithApplication with Scope with Around {
      override protected def builder = super.builder.bindings(new RedisCacheModule).configure(configuration).bindings(
        binding[RedisInstance].namedCache(defaultCacheName).to(MyRedisInstance)
      )
      def around[T: AsResult](t: => T): Result = runAndStop(t)
      protected def hocon = "play.cache.redis.source: custom"

      // bind named caches
      injector.instanceOf[RedisConnector] must beAnInstanceOf[RedisConnector]
      injector.instanceOf[CacheApi] must beAnInstanceOf[CacheApi]
      injector.instanceOf[CacheAsyncApi] must beAnInstanceOf[CacheAsyncApi]

      // Note: there should be tested which recovery policy instance is actually used
    }
  }
}

object RedisCacheModuleSpec {
  import Implicits._
  import play.api.cache.redis.configuration._
  import play.cache.NamedCacheImpl

  class AnyProvider[T](instance: => T) extends Provider[T] {
    lazy val get = instance
  }

  def binding[T: ClassTag]: BindingKey[T] = BindingKey(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  implicit class RichBindingKey[T](val key: BindingKey[T]) {
    def namedCache(name: String) = key.qualifiedWith(new NamedCacheImpl(name))
  }

  def runAndStop[T: AsResult](t: => T)(implicit system: ActorSystem) = {
    try {
      AsResult.effectively(t)
    } finally {
      Shutdown.run(system)
    }
  }

  object MyRedisInstance extends RedisStandalone {

    override def name = defaultCacheName
    override def invocationContext = "akka.actor.default-dispatcher"
    override def invocationPolicy = "lazy"
    override def timeout = RedisTimeouts(1.second)
    override def recovery = "log-and-default"
    override def source = "my-instance"
    override def prefix = None
    override def host = localhost
    override def port = defaultPort
    override def database = None
    override def username = None
    override def password = None
  }
}
