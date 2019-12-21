package play.api.cache.redis.configuration

import scala.concurrent.duration._

import org.specs2.mutable.Specification

class RedisInstanceManagerSpec extends Specification {
  import play.api.cache.redis.Implicits._

  private implicit def implicitlyInstance2resolved(instance: RedisInstance): RedisInstanceProvider = new ResolvedRedisInstance(instance)
  private implicit def implicitlyString2unresolved(name: String): RedisInstanceProvider = new UnresolvedRedisInstance(name)

  private val extras = RedisSettingsTest("my-dispatcher", "eager", RedisTimeouts(5.minutes, 5.seconds, 300.millis), "log-and-fail", "standalone", "redis.")

  "default configuration" in new WithRedisInstanceManager(
    """
      |play.cache.redis {}
    """
  ) {
    val defaultCache: RedisInstanceProvider = RedisStandalone(defaultCacheName, RedisHost(localhost, defaultPort, database = 0), defaults)

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(defaultCache)

    manager.instanceOf(defaultCacheName) mustEqual defaultCache
    manager.instanceOfOption(defaultCacheName) must beSome(defaultCache)
    manager.instanceOfOption("other") must beNone

    manager.defaultInstance mustEqual defaultCache
  }

  "single default instance" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  host:          redis.localhost.cz
      |  port:          6378
      |  database:      2
      |  password:      something
      |
      |  sync-timeout:  5 minutes
      |  redis-timeout: 5 seconds
      |  connection-timeout: 300 ms
      |
      |  prefix:        "redis."
      |  dispatcher:    my-dispatcher
      |  invocation:    eager
      |  recovery:      log-and-fail
      |}
    """
  ) {
    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisStandalone(defaultCacheName, RedisHost("redis.localhost.cz", 6378, database = 2, password = "something"), extras)
    )
  }

  "named caches" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  instances {
      |
      |    play {
      |      host:     localhost
      |      port:     6379
      |      database: 1
      |
      |      sync-timeout:  5 minutes
      |      redis-timeout: 5 seconds
      |      connection-timeout: 300 ms
      |
      |      prefix:        "redis."
      |      dispatcher:    my-dispatcher
      |      invocation:    eager
      |      recovery:      log-and-fail
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
    """
  ) {
    val defaultCache: RedisInstanceProvider = RedisStandalone(defaultCacheName, RedisHost(localhost, defaultPort, database = 1), extras)
    val otherCache: RedisInstanceProvider = RedisStandalone("other", RedisHost("redis.localhost.cz", 6378, database = 2, password = "something"), defaults)

    manager mustEqual RedisInstanceManagerTest("other")(defaultCache, otherCache)
    manager.instanceOf(defaultCacheName) mustEqual defaultCache
    manager.instanceOf("other") mustEqual otherCache
    manager.defaultInstance mustEqual otherCache
  }

  "cluster mode" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  instances {
      |    play {
      |      cluster: [
      |        { host: "localhost", port: 6380 }
      |        { host: "localhost", port: 6381 }
      |        { host: "localhost", port: 6382 }
      |        { host: "localhost", port: 6383 }
      |      ]
      |      source: cluster
      |    }
      |  }
      |}
    """
  ) {
    def node(port: Int) = RedisHost(localhost, port)

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisCluster(defaultCacheName, node(6380) :: node(6381) :: node(6382) :: node(6383) :: Nil, defaults.copy(source = "cluster"))
    )
  }

  "AWS cluster mode" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  instances {
      |    play {
      |      host:    localhost
      |      source:  aws-cluster
      |    }
      |  }
      |}
    """
  ) {
    val provider = manager.defaultInstance.asInstanceOf[ResolvedRedisInstance]
    val instance = provider.instance.asInstanceOf[RedisCluster]
    instance.nodes must contain(RedisHost("127.0.0.1", 6379))
  }

  "sentinel mode" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  instances {
      |    play {
      |      sentinels: [
      |        { host: "localhost", port: 6380 }
      |        { host: "localhost", port: 6381 }
      |        { host: "localhost", port: 6382 }
      |      ]
      |      master-group: primary
      |      password: "my-password"
      |      database: 1
      |      source: sentinel
      |    }
      |  }
      |}
    """
  ) {
    def node(port: Int) = RedisHost(localhost, port)

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisSentinel(defaultCacheName, "primary", node(6380) :: node(6381) :: node(6382) :: Nil, defaults.copy(source = "sentinel"), password = "my-password", database = 1)
    )
  }

  "connection string mode" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  source:            "connection-string"
      |  connection-string: "redis://localhost:6379"
      |}
    """
  ) {
    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisStandalone(defaultCacheName, RedisHost(localhost, defaultPort), defaults.copy(source = "connection-string"))
    )
  }

  "custom mode" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  source: custom
      |}
    """
  ) {
    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(defaultCacheName)
  }

  "typo in mode with simple syntax" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  source: typo
      |}
    """
  ) {
    manager.defaultInstance must throwA[IllegalStateException]
  }

  "typo in mode with advanced syntax" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  instances {
      |    play {
      |      source: typo
      |    }
      |  }
      |}
    """
  ) {
    manager.defaultInstance must throwA[IllegalStateException]
  }

  "fail when requesting undefined cache" in new WithRedisInstanceManager(
    """
      |play.cache.redis {
      |  instances {
      |    play {
      |      host: localhost
      |      port: 6379
      |    }
      |  }
      |  default-cache:  other
      |}
    """
  ) {

    manager.instanceOfOption(defaultCacheName) must beSome[RedisInstanceProvider]
    manager.instanceOfOption("other") must beNone

    manager.instanceOf("other") must throwA[IllegalArgumentException]
    manager.defaultInstance must throwA[IllegalArgumentException]
  }
}
