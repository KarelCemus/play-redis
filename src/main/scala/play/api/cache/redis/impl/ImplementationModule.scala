package play.api.cache.redis.impl

import play.api.cache.redis.exception._
import play.api.cache.redis.{CacheApi, CacheAsyncApi}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

/**
  * Dispatches policy configuration and declares methods
  * to initialize proper policy. This is reusable implementation
  * encapsulating all supported values in configuration and
  * to ensure consistency if the keys are added or changed.
  *
  * @author Karel Cemus
  */
private[ impl ] trait PolicyResolver[ T ] {

  /** reads the configuration and provides proper recovery policy binding */
  def resolve( configuration: Configuration ) = {
    configuration.get[ String ]( "play.cache.redis.recovery" ) match {
      case "log-and-fail" => logAndFail
      case "log-and-default" => logAndDefault
      case "log-condensed-and-fail" => logCondensedAndFail
      case "log-condensed-and-default" => logCondensedAndDefault
      case "custom" => custom
      case _ => invalidConfiguration( "Invalid value in 'play.cache.redis.recovery'. Accepted values are 'log-and-fail', 'log-and-default', and 'custom'." )
    }
  }

  /** creates the policy based on the PolicyResolver implementation */
  protected def logAndFail: T

  /** creates the policy based on the PolicyResolver implementation */
  protected def logAndDefault: T

  /** creates the policy based on the PolicyResolver implementation */
  protected def logCondensedAndFail: T

  /** creates the policy based on the PolicyResolver implementation */
  protected def logCondensedAndDefault: T

  /** creates the policy based on the PolicyResolver implementation */
  protected def custom: T
}

/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
object ImplementationModule extends Module {

  private object RedisRecoveryPolicyResolver extends PolicyResolver[ Option[ Binding[ RecoveryPolicy ] ] ] {
    protected def logAndFail = Some( bind[ RecoveryPolicy ].to[ LogAndFailPolicy ] )
    protected def logAndDefault = Some( bind[ RecoveryPolicy ].to[ LogAndDefaultPolicy ] )
    protected def logCondensedAndFail = Some( bind[ RecoveryPolicy ].to[ LogCondensedAndFailPolicy ] )
    protected def logCondensedAndDefault = Some( bind[ RecoveryPolicy ].to[ LogCondensedAndDefaultPolicy ] )
    protected def custom = None // do nothing, user provides own implementation binding
  }

  override def bindings( environment: Environment, configuration: Configuration ) = Seq(
    // play-redis APIs
    bind[ CacheApi ].to[ SyncRedis ],
    bind[ CacheAsyncApi ].to[ AsyncRedis ],
    // scala api defined by Play
    bind[ play.api.cache.CacheApi ].to[ play.api.cache.DefaultSyncCacheApi ],
    bind[ play.api.cache.SyncCacheApi ].to[ play.api.cache.DefaultSyncCacheApi ],
    bind[ play.api.cache.AsyncCacheApi ].to[ AsyncRedis ],
    // java api defined by Play
    bind[ play.cache.CacheApi ].to[ play.cache.DefaultSyncCacheApi ],
    bind[ play.cache.SyncCacheApi ].to[ play.cache.DefaultSyncCacheApi ],
    bind[ play.cache.AsyncCacheApi ].to[ JavaRedis ]

  ) ++ RedisRecoveryPolicyResolver.resolve( configuration )
}

/**
  * Components for compile-time dependency injection.
  * It binds components from impl package
  *
  * @author Karel Cemus
  */
private[ redis ] trait ImplementationComponents {

  import play.api.cache.redis._

  def configuration: Configuration

  def environment: Environment

  def redisConnector: RedisConnector

  private object RecoveryPolicyResolver extends PolicyResolver[ RecoveryPolicy ] {
    protected def logAndFail = new LogAndFailPolicy
    protected def logAndDefault = new LogAndDefaultPolicy
    protected def logCondensedAndFail = new LogCondensedAndFailPolicy
    protected def logCondensedAndDefault = new LogCondensedAndDefaultPolicy
    protected def custom = customRedisRecoveryPolicy // enable escape and overriding
  }

  private lazy val policy: RecoveryPolicy = RecoveryPolicyResolver.resolve( configuration )

  // play-redis APIs
  private lazy val asyncRedis = new AsyncRedis( redisConnector, policy )
  lazy val syncRedisCacheApi: CacheApi = new SyncRedis( redisConnector, policy )
  lazy val asyncRedisCacheApi: CacheAsyncApi = asyncRedis

  // scala api defined by Play
  lazy val asyncCacheApi: play.api.cache.AsyncCacheApi = asyncRedis
  private lazy val defaultSyncCache = new play.api.cache.DefaultSyncCacheApi( asyncCacheApi )
  @deprecated( message = "Use syncCacheApi or asyncCacheApi.", since = "Play 2.6.0." )
  lazy val defaultCacheApi: play.api.cache.CacheApi = defaultSyncCache
  lazy val syncCacheApi: play.api.cache.SyncCacheApi = defaultSyncCache

  // java api defined by Play
  lazy val javaAsyncCacheApi: play.cache.AsyncCacheApi = new JavaRedis( asyncRedis, environment, redisConnector )
  private lazy val javaDefaultSyncCache = new play.cache.DefaultSyncCacheApi( javaAsyncCacheApi )
  @deprecated( message = "Use javaSyncCacheApi or javaAsyncCacheApi.", since = "Play 2.6.0." )
  lazy val javaCacheApi: play.cache.CacheApi = javaDefaultSyncCache
  lazy val javaSyncCacheApi: play.cache.SyncCacheApi = javaDefaultSyncCache

  protected def customRedisRecoveryPolicy: RecoveryPolicy =
    shouldBeOverwritten( "In order to use custom RecoveryPolicy overwrite this method." )
}
