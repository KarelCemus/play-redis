package play.api.cache.redis.impl

import akka.actor.ActorSystem
import akka.util.Timeout
import play.api.cache.redis._
import play.api.cache.redis.configuration.{RedisHost, RedisStandalone}
import play.api.cache.redis.test.UnitSpec

class RedisRuntimeSpec extends UnitSpec {
  import RedisRuntime._

  implicit private val recoveryResolver: RecoveryPolicyResolver =
    new RecoveryPolicyResolverImpl

  implicit private val system: ActorSystem = ActorSystem("test")

  "be build from config (A)" in {
    val instance = RedisStandalone(
      name = "standalone",
      host = RedisHost(localhost, defaultPort),
      settings = defaultsSettings,
    )
    val runtime = RedisRuntime(
      instance = instance,
      recovery = "log-and-fail",
      invocation = "eager",
      prefix = None,
    )
    runtime.timeout mustEqual Timeout(instance.timeout.sync)
    runtime.policy mustBe a[LogAndFailPolicy]
    runtime.invocation mustEqual EagerInvocation
    runtime.prefix mustEqual RedisEmptyPrefix
  }

  "be build from config (B)" in {
    val instance = RedisStandalone(
      name = "standalone",
      host = RedisHost(localhost, defaultPort),
      settings = defaultsSettings,
    )
    val runtime = RedisRuntime(
      instance = instance,
      recovery = "log-and-default",
      invocation = "lazy",
      prefix = Some("prefix"),
    )
    runtime.policy mustBe a[LogAndDefaultPolicy]
    runtime.invocation mustEqual LazyInvocation
    runtime.prefix mustEqual new RedisPrefixImpl("prefix")
  }

  "be build from config (C)" in {
    val instance = RedisStandalone(
      name = "standalone",
      host = RedisHost(localhost, defaultPort),
      settings = defaultsSettings,
    )
    assertThrows[IllegalArgumentException] {
      RedisRuntime(
        instance = instance,
        recovery = "log-and-default",
        invocation = "other",
        prefix = Some("prefix"),
      )
    }
  }

}
