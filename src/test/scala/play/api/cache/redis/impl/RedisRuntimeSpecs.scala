package play.api.cache.redis.impl

import play.api.cache.redis._

import org.specs2.mutable.Specification

class RedisRuntimeSpecs extends Specification with WithApplication {

  import Implicits._

  implicit val recoveryResolver = new RecoveryPolicyResolverImpl

  "RedisRuntime" should {
    import RedisRuntime._

    "be build from config (A)" in {
      val runtime = RedisRuntime(
        instance = defaultInstance,
        recovery = "log-and-fail",
        invocation = "eager",
        prefix = None
      )

      runtime.timeout mustEqual akka.util.Timeout(defaultInstance.timeout.sync)
      runtime.policy must beAnInstanceOf[LogAndFailPolicy]
      runtime.invocation mustEqual EagerInvocation
      runtime.prefix mustEqual RedisEmptyPrefix
    }

    "be build from config (B)" in {
      val runtime = RedisRuntime(
        instance = defaultInstance,
        recovery = "log-and-default",
        invocation = "lazy",
        prefix = Some("prefix")
      )

      runtime.policy must beAnInstanceOf[LogAndDefaultPolicy]
      runtime.invocation mustEqual LazyInvocation
      runtime.prefix mustEqual new RedisPrefixImpl("prefix")
    }

    "be build from config (C)" in {
      RedisRuntime(
        instance = defaultInstance,
        recovery = "log-and-default",
        invocation = "other",
        prefix = Some("prefix")
      ) must throwA[IllegalArgumentException]
    }
  }
}
