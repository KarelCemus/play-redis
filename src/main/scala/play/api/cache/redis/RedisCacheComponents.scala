package play.api.cache.redis

import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}

/**
  * <p>Components for compile-time dependency injection. It binds components
  * from configuration package</p>
  */
trait RedisCacheComponents {
  implicit def actorSystem: akka.actor.ActorSystem
  implicit def applicationLifecycle: ApplicationLifecycle
  def configuration: Configuration
  def environment: Environment

  /** default implementation of the empty resolver */
  private lazy val emptyRecoveryResolver = new RecoveryPolicyResolverImpl

  /** override this for providing a custom policy resolver */
  implicit def recoveryPolicyResolver: RecoveryPolicyResolver = emptyRecoveryResolver

  /** default implementation of the empty resolver */
  private lazy val emptyInstanceResolver: RedisInstanceResolver = new RedisInstanceResolver {
    val resolve: PartialFunction[String, RedisInstance] = PartialFunction.empty
  }

  /** override this for providing a custom redis instance resolver */
  implicit def redisInstanceResolver: RedisInstanceResolver = emptyInstanceResolver

  private lazy val akkaSerializer: connector.AkkaSerializer = new connector.AkkaSerializerProvider().get

  private lazy val manager = configuration.get("play.cache.redis")(play.api.cache.redis.configuration.RedisInstanceManager)

  /** translates the cache name into the configuration */
  private def redisInstance(name: String)(implicit resolver: RedisInstanceResolver): RedisInstance = manager.instanceOf(name).resolved(resolver)

  private def cacheApi(instance: RedisInstance): impl.RedisCaches = new impl.RedisCachesProvider(instance, akkaSerializer, environment).get

  def cacheApi(name: String)(implicit resolver: RedisInstanceResolver): RedisCaches = cacheApi(redisInstance(name)(resolver))
}
