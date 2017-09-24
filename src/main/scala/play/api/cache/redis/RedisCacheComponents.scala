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

    private def hasInstances = configuration.underlying.hasPath( "play.cache.redis.instances" )

    private def defaultCache = configuration.underlying.getString( "play.cache.redis.default-cache" )

    implicit def redisInstance( name: String ): RedisInstance = configuration.get {
      if ( hasInstances ) s"play.cache.redis.instances.$name"
      else if ( !hasInstances && name == defaultCache ) s"play.cache.redis"
      else throw new IllegalArgumentException( s"Redis cache '$name' is not defined." )
    }( RedisInstanceBinder.loader( name ) ) match {
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

    private[ redis ] def redisConnectorFor( instance: RedisInstance )( implicit runtime: RedisRuntime ) =
      new RedisConnectorImpl( akkaSerializer, redisCommandsFor( instance ) )
  }
}

package impl {

  private[ redis ] trait RedisImplComponents {
    import akka.actor.ActorSystem

    def environment: Environment

    def actorSystem: ActorSystem

    /** overwrite to provide custom recovery policy */
    def redisRecoveryPolicy: PartialFunction[ String, RecoveryPolicy ] = {
      case "log-and-fail" => new LogAndFailPolicy
      case "log-and-default" => new LogAndDefaultPolicy
      case "log-condensed-and-fail" => new LogCondensedAndFailPolicy
      case "log-condensed-and-default" => new LogCondensedAndDefaultPolicy
    }

    private[ redis ] def redisConnectorFor( instance: RedisInstance )( implicit runtime: RedisRuntime ): RedisConnector

    private implicit def instance2connector( instance: RedisInstance )( implicit runtime: RedisRuntime ): RedisConnector = redisConnectorFor( instance )
    private implicit def instance2policy( instance: RedisInstance ): RecoveryPolicy = redisRecoveryPolicy( instance.recovery )

    implicit def runtime( implicit instance: RedisInstance ) = RedisRuntime( instance = instance, recovery = instance )( actorSystem )

    // play-redis APIs
    private def asyncRedis( implicit instance: RedisInstance ) = new AsyncRedis( redis = instance )

    def syncRedisCacheApi( implicit instance: RedisInstance ): CacheApi = new SyncRedis( redis = instance )
    def asyncRedisCacheApi( instance: RedisInstance ): CacheAsyncApi = asyncRedis( instance )

    // scala api defined by Play
    def asyncCacheApi( instance: RedisInstance ): play.api.cache.AsyncCacheApi = asyncRedis( instance )
    private def defaultSyncCache( instance: RedisInstance ) = new play.api.cache.DefaultSyncCacheApi( asyncCacheApi( instance ) )
    @deprecated( message = "Use syncCacheApi or asyncCacheApi.", since = "Play 2.6.0." )
    def defaultCacheApi( instance: RedisInstance ): play.api.cache.CacheApi = defaultSyncCache( instance )
    def syncCacheApi( instance: RedisInstance ): play.api.cache.SyncCacheApi = defaultSyncCache( instance )

    // java api defined by Play
    def javaAsyncCacheApi( implicit instance: RedisInstance ): play.cache.AsyncCacheApi = new JavaRedis( asyncRedis( instance ), environment = environment )
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
