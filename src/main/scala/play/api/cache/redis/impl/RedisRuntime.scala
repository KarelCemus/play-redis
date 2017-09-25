package play.api.cache.redis.impl

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import play.api.cache.redis._

import akka.actor.ActorSystem

/**
  * Runtime info about the current cache instance. It includes
  * a configuration, recovery policy, and the execution context.
  *
  * @author Karel Cemus
  */
private[ redis ] trait RedisRuntime extends connector.RedisRuntime {
  implicit def policy: RecoveryPolicy
  implicit def timeout: akka.util.Timeout
}

private[ redis ] case class RedisRuntimeImpl( name: String, context: ExecutionContext, policy: RecoveryPolicy, timeout: akka.util.Timeout ) extends RedisRuntime

private[ redis ] object RedisRuntime {

  def apply( instance: RedisInstance, recovery: RecoveryPolicy )( implicit system: ActorSystem ): RedisRuntime =
    apply( instance.name, instance.timeout, system.dispatchers.lookup( instance.invocationContext ), recovery )

  def apply( name: String, timeout: FiniteDuration, context: ExecutionContext, recovery: RecoveryPolicy ): RedisRuntime =
    RedisRuntimeImpl( name, context, recovery, akka.util.Timeout( timeout ) )
}
