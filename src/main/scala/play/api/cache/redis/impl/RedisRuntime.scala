package play.api.cache.redis.impl

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

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
  implicit def prefix: RedisPrefix
  implicit def timeout: akka.util.Timeout
}

private[ redis ] case class RedisRuntimeImpl( name: String, context: ExecutionContext, policy: RecoveryPolicy, prefix: RedisPrefix, timeout: akka.util.Timeout ) extends RedisRuntime

private[ redis ] object RedisRuntime {

  implicit def string2prefix( prefix: Option[ String ] ): RedisPrefix =
    prefix.fold[ RedisPrefix ]( RedisEmptyPrefix )( new RedisPrefixImpl( _ ) )

  implicit def string2recovery( policy: String )( implicit resolver: RecoveryPolicyResolver ): RecoveryPolicy =
    resolver resolve policy

  def apply( instance: RedisInstance, recovery: RecoveryPolicy, prefix: RedisPrefix )( implicit system: ActorSystem ): RedisRuntime =
    apply( instance.name, instance.timeout, system.dispatchers.lookup( instance.invocationContext ), recovery, prefix )

  def apply( name: String, timeout: FiniteDuration, context: ExecutionContext, recovery: RecoveryPolicy, prefix: RedisPrefix = RedisEmptyPrefix ): RedisRuntime =
    RedisRuntimeImpl( name, context, recovery, prefix, akka.util.Timeout( timeout ) )
}
