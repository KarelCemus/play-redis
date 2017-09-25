package play.api.cache.redis

import scala.language.implicitConversions

import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}

/**
  * <p>Components for compile-time dependency injection.
  * It binds components from configuration package</p>
  *
  * @author Karel Cemus
  */
trait RedisCacheComponents
{
  implicit def actorSystem: akka.actor.ActorSystem
  implicit def applicationLifecycle: ApplicationLifecycle
  def configuration: Configuration
  def environment: Environment

  /** default implementation of the empty resolver */
  private lazy val emptyRecoveryResolver = new RecoveryPolicyResolverImpl

  /** override this for providing a custom policy resolver */
  implicit def recoveryPolicyResolver = emptyRecoveryResolver

  /** default implementation of the empty resolver */
  private lazy val emptyInstanceResolver = new play.api.cache.redis.configuration.RedisInstanceResolver {
    val resolve = PartialFunction.empty
  }

  /** override this for providing a custom redis instance resolver */
  implicit def redisInstanceResolver = emptyInstanceResolver

  private lazy val akkaSerializer: connector.AkkaSerializer = new connector.AkkaSerializerProvider().get

  private def hasInstances = configuration.underlying.hasPath( "play.cache.redis.instances" )

  private def defaultCache = configuration.underlying.getString( "play.cache.redis.default-cache" )

  private lazy val manager = configuration.get( "play.cache.redis" )( play.api.cache.redis.configuration.RedisInstanceManager )

  /** translates the cache name into the configuration  */
  implicit def redisInstance( name: String )( implicit resolver: play.api.cache.redis.configuration.RedisInstanceResolver ): RedisInstance = manager.instanceOf( name ).resolved

  def cacheApi( instance: RedisInstance ): impl.RedisCaches = new impl.RedisCachesProvider( instance, akkaSerializer, environment, recoveryPolicyResolver ).get
}
