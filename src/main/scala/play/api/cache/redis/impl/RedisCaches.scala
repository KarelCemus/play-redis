package play.api.cache.redis.impl

import javax.inject.Provider

import play.api.Environment
import play.api.cache.redis._
import play.api.inject.ApplicationLifecycle

import akka.actor.ActorSystem

/**
  * Aggregates all available redis APIs into a single handler. This simplifies
  * binding, construction, and accessing all APIs.
  */
trait RedisCaches {
  def sync: CacheApi
  def async: CacheAsyncApi
  def scalaAsync: play.api.cache.AsyncCacheApi
  def scalaSync: play.api.cache.SyncCacheApi
  def javaSync: play.cache.SyncCacheApi
  def javaAsync: play.cache.redis.AsyncCacheApi
}

private[redis] class RedisCachesProvider(instance: RedisInstance, serializer: connector.AkkaSerializer, environment: Environment)(implicit system: ActorSystem, lifecycle: ApplicationLifecycle, recovery: RecoveryPolicyResolver) extends Provider[RedisCaches] {
  import RedisRuntime._

  private implicit lazy val runtime: RedisRuntime = RedisRuntime(instance, instance.recovery, instance.invocationPolicy, instance.prefix)(system)

  private implicit def implicitEnvironment = environment

  private lazy val redisConnector = new connector.RedisConnectorProvider(instance, serializer).get

  lazy val get = new RedisCaches {
    lazy val async = new AsyncRedis(redisConnector)
    lazy val sync = new SyncRedis(redisConnector)
    lazy val scalaSync = new play.api.cache.DefaultSyncCacheApi(async)
    lazy val scalaAsync = async
    lazy val java = new AsyncJavaRedis(async)
    lazy val javaAsync = java
    lazy val javaSync = new play.cache.DefaultSyncCacheApi(java)
  }
}
