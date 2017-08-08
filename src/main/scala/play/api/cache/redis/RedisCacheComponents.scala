package play.api.cache.redis

import scala.language.implicitConversions

import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}

package configuration {

  private[ redis ] trait RedisConfigurationComponents {

    def configuration: Configuration

    implicit lazy val redisDefaults = configuration.get( "play.cache.redis" )( RedisSettings )

    /** override this method to provide custom configuration for some instances */
    def redisInstanceConfiguration: PartialFunction[ String, RedisInstance ] = PartialFunction.empty

    implicit def redisInstance( name: String ): RedisInstance =
      configuration.get( s"play.cache.redis.instance.$name" )( RedisInstanceBinder.loader( name ) ) match {
        case self: RedisInstanceSelfBinder => self.instance
        case _: RedisInstanceCustomBinder => redisInstanceConfiguration( name )
      }
  }
}

package connector {

  private[ redis ] trait RedisConnectorComponents {
    import configuration._

    import akka.actor.ActorSystem
    import redis.RedisCommands

    def actorSystem: ActorSystem

    def applicationLifecycle: ApplicationLifecycle

    private lazy val akkaSerializer: AkkaSerializer = new AkkaSerializerImpl( actorSystem )

    private def redisCommandsFor( instance: RedisInstance ): RedisCommands = instance match {
      case standalone: RedisStandalone => new RedisCommandsStandalone( standalone )( actorSystem, applicationLifecycle ).get
      case cluster: RedisCluster => new RedisCommandsCluster( cluster )( actorSystem, applicationLifecycle ).get
    }

    private[ redis ] def redisConnectorFor( instance: RedisInstance ) =
      new RedisConnectorImpl( akkaSerializer, instance, redisCommandsFor( instance ) )( actorSystem )
  }
}

package impl {

  private[ redis ] trait RedisImplComponents {

    def environment: Environment

    /** overwrite to provide custom recovery policy */
    def redisRecoveryPolicy: PartialFunction[ String, RecoveryPolicy ] = {
      case "log-and-fail" => new LogAndFailPolicy
      case "log-and-default" => new LogAndDefaultPolicy
      case "log-condensed-and-fail" => new LogCondensedAndFailPolicy
      case "log-condensed-and-default" => new LogCondensedAndDefaultPolicy
    }

    private[ redis ] def redisConnectorFor( instance: RedisInstance ): RedisConnector

    private implicit def instance2connector( instance: RedisInstance ): RedisConnector = redisConnectorFor( instance )
    private implicit def instance2policy( instance: RedisInstance ): RecoveryPolicy = redisRecoveryPolicy( instance.recovery )

    // play-redis APIs
    private def asyncRedis( instance: RedisInstance ) = new AsyncRedis( instance.name, redis = instance, policy = instance )

    def syncRedisCacheApi( instance: RedisInstance ): CacheApi = new SyncRedis( instance.name, redis = instance, policy = instance )
    def asyncRedisCacheApi( instance: RedisInstance ): CacheAsyncApi = asyncRedis( instance )

    // scala api defined by Play
    def asyncCacheApi( instance: RedisInstance ): play.api.cache.AsyncCacheApi = asyncRedis( instance )
    private def defaultSyncCache( instance: RedisInstance ) = new play.api.cache.DefaultSyncCacheApi( asyncCacheApi( instance ) )
    @deprecated( message = "Use syncCacheApi or asyncCacheApi.", since = "Play 2.6.0." )
    def defaultCacheApi( instance: RedisInstance ): play.api.cache.CacheApi = defaultSyncCache( instance )
    def syncCacheApi( instance: RedisInstance ): play.api.cache.SyncCacheApi = defaultSyncCache( instance )

    // java api defined by Play
    def javaAsyncCacheApi( instance: RedisInstance ): play.cache.AsyncCacheApi = new JavaRedis( instance.name, asyncRedis( instance ), environment = environment, connector = instance )
    private def javaDefaultSyncCache( instance: RedisInstance ) = new play.cache.DefaultSyncCacheApi( javaAsyncCacheApi( instance ) )
    @deprecated( message = "Use javaSyncCacheApi or javaAsyncCacheApi.", since = "Play 2.6.0." )
    def javaCacheApi( instance: RedisInstance ): play.cache.CacheApi = javaDefaultSyncCache( instance )
    def javaSyncCacheApi( instance: RedisInstance ): play.cache.SyncCacheApi = javaDefaultSyncCache( instance )
  }
}


/**
  * <p>Components for compile-time dependency injection.
  * It binds components from configuration package</p>
  *
  * @author Karel Cemus
  */
trait RedisCacheComponents
  extends configuration.RedisConfigurationComponents
  with connector.RedisConnectorComponents
  with impl.RedisImplComponents
