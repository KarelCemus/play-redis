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
    // default binding for Play's CacheApi to SyncCache to replace default EHCache
    bind[ play.api.cache.CacheApi ].to[ SyncRedis ],
    // enable sync module when required
    bind[ CacheApi ].to[ SyncRedis ],
    // enable async module when required
    bind[ CacheAsyncApi ].to[ AsyncRedis ],
    // java api
    bind[ play.cache.CacheApi ].to[ JavaRedis ],
    bind[ play.cache.SyncCacheApi ].to[ play.cache.DefaultSyncCacheApi ],
    bind[ play.cache.AsyncCacheApi ].to[ play.cache.DefaultAsyncCacheApi ]
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

  private lazy val syncRedis: SyncRedis = new SyncRedis( redisConnector, policy )

  lazy val syncRedisCacheApi: CacheApi = syncRedis

  lazy val asyncRedisCacheApi: CacheAsyncApi = new AsyncRedis( redisConnector, policy )

  @deprecated( message = "Use syncCacheApi or asyncCacheApi.", since = "Play 2.6.0." )
  lazy val defaultCacheApi: play.api.cache.CacheApi = syncRedis

  @deprecated( message = "Use javaSyncCacheApi or javaAsyncCacheApi.", since = "Play 2.6.0." )
  lazy val javaCacheApi: play.cache.CacheApi = new JavaRedis( syncRedis, environment )

  lazy val javaSyncCacheApi: play.cache.SyncCacheApi = new play.cache.DefaultSyncCacheApi( asyncCacheApi )

  lazy val javaAsyncCacheApi: play.cache.AsyncCacheApi = new play.cache.DefaultAsyncCacheApi( asyncCacheApi )

  protected def customRedisRecoveryPolicy: RecoveryPolicy =
    shouldBeOverwritten( "In order to use custom RecoveryPolicy overwrite this method." )
}
