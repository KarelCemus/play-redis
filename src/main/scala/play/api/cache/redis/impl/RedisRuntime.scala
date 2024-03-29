package play.api.cache.redis.impl

import org.apache.pekko.actor.ActorSystem
import play.api.cache.redis._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Runtime info about the current cache instance. It includes a configuration,
  * recovery policy, and the execution context.
  */
private[redis] trait RedisRuntime extends connector.RedisRuntime {
  implicit def policy: RecoveryPolicy
  implicit def invocation: InvocationPolicy
  implicit def prefix: RedisPrefix
  implicit def timeout: org.apache.pekko.util.Timeout
}

final private[redis] case class RedisRuntimeImpl(
  name: String,
  context: ExecutionContext,
  policy: RecoveryPolicy,
  invocation: InvocationPolicy,
  prefix: RedisPrefix,
  timeout: org.apache.pekko.util.Timeout,
) extends RedisRuntime

private[redis] object RedisRuntime {

  implicit def string2prefix(prefix: Option[String]): RedisPrefix =
    prefix.fold[RedisPrefix](RedisEmptyPrefix)(new RedisPrefixImpl(_))

  implicit def string2recovery(policy: String)(implicit resolver: RecoveryPolicyResolver): RecoveryPolicy =
    resolver resolve policy

  implicit def string2invocation(invocation: String): InvocationPolicy = invocation.toLowerCase.trim match {
    case "lazy"  => LazyInvocation
    case "eager" => EagerInvocation
    case _       => throw new IllegalArgumentException("Illegal invocation policy. Valid values are 'lazy' and 'eager'. See the documentation for more details.")
  }

  def apply(instance: RedisInstance, recovery: RecoveryPolicy, invocation: InvocationPolicy, prefix: RedisPrefix)(implicit system: ActorSystem): RedisRuntime =
    apply(instance.name, instance.timeout.sync, system.dispatchers.lookup(instance.invocationContext), recovery, invocation, prefix)

  def apply(name: String, syncTimeout: FiniteDuration, context: ExecutionContext, recovery: RecoveryPolicy, invocation: InvocationPolicy, prefix: RedisPrefix = RedisEmptyPrefix): RedisRuntime =
    RedisRuntimeImpl(name, context, recovery, invocation, prefix, org.apache.pekko.util.Timeout(syncTimeout))

}
