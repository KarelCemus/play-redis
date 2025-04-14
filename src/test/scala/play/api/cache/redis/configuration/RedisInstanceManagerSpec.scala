package play.api.cache.redis.configuration

import play.api.cache.redis.test._
import play.api.{ConfigLoader, Configuration}

import scala.concurrent.duration._

class RedisInstanceManagerSpec extends UnitSpec with ImplicitOptionMaterialization {

  "default configuration" in new TestCase {

    override protected def hocon: String =
      """
          |play.cache.redis {}
        """

    private val defaultCache: RedisInstanceProvider =
      RedisStandalone(
        name = defaultCacheName,
        host = RedisHost(localhost, defaultPort, database = 0),
        settings = defaultsSettings,
      )

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(defaultCache)

    manager.instanceOf(defaultCacheName) mustEqual defaultCache
    manager.instanceOfOption(defaultCacheName) mustEqual Some(defaultCache)
    manager.instanceOfOption("other") mustEqual None

    manager.defaultInstance mustEqual defaultCache
  }

  "single default instance" in new TestCase {

    override protected def hocon: String =
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

    private val settings: RedisSettings = RedisSettingsTest(
      invocationContext = "my-dispatcher",
      invocationPolicy = "eager",
      timeout = RedisTimeouts(5.minutes, 5.seconds, 300.millis),
      recovery = "log-and-fail",
      source = "standalone",
      prefix = "redis.",
      threadPool = RedisThreadPools(1, 1),
    )

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisStandalone(defaultCacheName, RedisHost("redis.localhost.cz", 6378, database = 2, password = "something"), settings),
    )

  }

  "named caches" in new TestCase {

    override protected def hocon: String =
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

    private val otherSettings: RedisSettings = RedisSettingsTest(
      invocationContext = "my-dispatcher",
      invocationPolicy = "eager",
      timeout = RedisTimeouts(5.minutes, 5.seconds, 300.millis),
      recovery = "log-and-fail",
      source = "standalone",
      prefix = "redis.",
      threadPool = RedisThreadPools(1, 1),
    )

    private val defaultCache: RedisInstanceProvider = RedisStandalone(defaultCacheName, RedisHost(localhost, defaultPort, database = 1), otherSettings)
    private val otherCache: RedisInstanceProvider = RedisStandalone("other", RedisHost("redis.localhost.cz", 6378, database = 2, password = "something"), defaultsSettings)

    manager mustEqual RedisInstanceManagerTest("other")(defaultCache, otherCache)
    manager.instanceOf(defaultCacheName) mustEqual defaultCache
    manager.instanceOf("other") mustEqual otherCache
    manager.defaultInstance mustEqual otherCache
  }

  "cluster mode" in new TestCase {

    override protected def hocon: String =
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

    private def node(port: Int) = RedisHost(localhost, port)

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisCluster(name = defaultCacheName, nodes = node(6380) :: node(6381) :: node(6382) :: node(6383) :: Nil, settings = defaultsSettings.copy(source = "cluster")),
    )

  }

  "AWS cluster mode" in new TestCase {

    override protected def hocon: String =
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

    private val provider = manager.defaultInstance.asInstanceOf[ResolvedRedisInstance]
    private val instance = provider.instance.asInstanceOf[RedisCluster]
    instance.nodes must contain(RedisHost("127.0.0.1", 6379))
  }

  "sentinel mode" in new TestCase {

    override protected def hocon: String =
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

    private def node(port: Int) = RedisHost(localhost, port)

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisSentinel(
        name = defaultCacheName,
        masterGroup = "primary",
        sentinels = node(6380) :: node(6381) :: node(6382) :: Nil,
        settings = defaultsSettings.copy(source = "sentinel"),
        password = "my-password",
        database = 1,
      ),
    )

  }

  "connection string mode" in new TestCase {

    override protected def hocon: String =
      """
      |play.cache.redis {
      |  source:            "connection-string"
      |  connection-string: "redis://localhost:6379"
      |}
    """

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisStandalone(defaultCacheName, RedisHost(localhost, defaultPort), defaultsSettings.copy(source = "connection-string")),
    )

  }

  "master-slaves mode" in new TestCase {

    override protected def hocon: String =
      """
        |play.cache.redis {
        |  instances {
        |    play {
        |      master: { host: "localhost", port: 6380 }
        |      slaves: [
        |        { host: "localhost", port: 6381 }
        |        { host: "localhost", port: 6382 }
        |      ]
        |      password: "my-password"
        |      database: 1
        |      source: master-slaves
        |    }
        |  }
        |}
    """

    private def node(port: Int) = RedisHost(localhost, port)

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(
      RedisMasterSlaves(
        name = defaultCacheName,
        master = node(6380),
        slaves = node(6381) :: node(6382) :: Nil,
        settings = defaultsSettings.copy(source = "master-slaves"),
        password = "my-password",
        database = 1,
      ),
    )

  }

  "custom mode" in new TestCase {

    override protected def hocon: String =
      """
      |play.cache.redis {
      |  source: custom
      |}
    """

    manager mustEqual RedisInstanceManagerTest(defaultCacheName)(defaultCacheName)
  }

  "typo in mode with simple syntax" in new TestCase {

    override protected def hocon: String =
      """
      |play.cache.redis {
      |  source: typo
      |}
    """

    the[IllegalStateException] thrownBy manager.defaultInstance
  }

  "typo in mode with advanced syntax" in new TestCase {

    override protected def hocon: String =
      """
      |play.cache.redis {
      |  instances {
      |    play {
      |      source: typo
      |    }
      |  }
      |}
    """

    the[IllegalStateException] thrownBy manager.defaultInstance
  }

  "fail when requesting undefined cache" in new TestCase {

    override protected def hocon: String =
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

    manager.instanceOfOption(defaultCacheName) mustBe a[Some[?]]
    manager.instanceOfOption("other") mustEqual None

    the[IllegalArgumentException] thrownBy manager.instanceOf("other")
    the[IllegalArgumentException] thrownBy manager.defaultInstance
  }

  private trait TestCase {

    implicit private val loader: ConfigLoader[RedisInstanceManager] = RedisInstanceManager

    private val configuration: Configuration = Helpers.configuration.fromHocon(hocon)

    protected val manager: RedisInstanceManager = configuration.get[RedisInstanceManager]("play.cache.redis")

    protected def hocon: String

    implicit protected def implicitlyInstance2resolved(instance: RedisInstance): RedisInstanceProvider =
      new ResolvedRedisInstance(instance)

    implicit protected def implicitlyString2unresolved(name: String): RedisInstanceProvider =
      new UnresolvedRedisInstance(name)

    final protected case class RedisInstanceManagerTest(
      default: String,
    )(
      providers: RedisInstanceProvider*,
    ) extends RedisInstanceManager {

      override def caches: Set[String] = providers.map(_.name).toSet

      override def instanceOfOption(name: String): Option[RedisInstanceProvider] = providers.find(_.name === name)

      override def defaultInstance: RedisInstanceProvider = providers.find(_.name === default) getOrElse {
        throw new RuntimeException("Default instance is not defined.")
      }

    }

  }

}
