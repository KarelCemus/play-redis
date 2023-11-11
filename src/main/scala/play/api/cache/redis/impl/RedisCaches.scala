package play.api.cache.redis.impl

import javax.inject.Provider
import play.api.Environment
import play.api.cache.redis._
import play.api.inject.ApplicationLifecycle
import akka.actor.ActorSystem
import play.api.cache.{AsyncCacheApi, DefaultSyncCacheApi}

/**
  * Aggregates all available redis APIs into a single handler. This simplifies
  * binding, construction, and accessing all APIs.
  */
trait RedisCaches {
  def redisConnector: connector.RedisConnector
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

  private implicit def implicitEnvironment: Environment = environment

  lazy val get = new RedisCaches {
    lazy val redisConnector: RedisConnector = new connector.RedisConnectorProvider(instance, serializer).get
    lazy val async: AsyncRedis = new AsyncRedisImpl(redisConnector)
    lazy val sync: CacheApi = new SyncRedis(redisConnector)
    lazy val scalaSync: play.api.cache.SyncCacheApi = new play.api.cache.DefaultSyncCacheApi(async)
    lazy val scalaAsync: play.api.cache.AsyncCacheApi = async
    lazy val java: AsyncJavaRedis = new AsyncJavaRedis(async)
    lazy val javaAsync: play.cache.redis.AsyncCacheApi = java
    lazy val javaSync: play.cache.SyncCacheApi = new play.cache.DefaultSyncCacheApi(java)
  }
}
