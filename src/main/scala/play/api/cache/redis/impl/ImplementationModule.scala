package play.api.cache.redis.impl

import play.api.cache.redis.{CacheApi, CacheAsyncApi}
import play.api.cache.redis.exception._
import play.api.inject.Module
import play.api.{Configuration, Environment}

/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
object ImplementationModule extends Module {

  override def bindings( environment: Environment, configuration: Configuration ) = Seq(
    // default binding for Play's CacheApi to SyncCache to replace default EHCache
    bind[ play.api.cache.CacheApi ].to[ SyncRedis ],
    // enable sync module when required
    bind[ CacheApi ].to[ SyncRedis ],
    // enable async module when required
    bind[ CacheAsyncApi ].to[ AsyncRedis ],
    // java api
    bind[ play.cache.CacheApi ].to[ JavaRedis ]
  ) ++ policy( configuration )

  /** reads the configuration and provides proper recovery policy binding */
  private def policy( configuration: Configuration ) = {
    configuration.getString( "play.cache.redis.recovery" ) match {
      case Some( "log-and-fail" ) => Some( bind[ RecoveryPolicy ].to[ LogAndFailPolicy ] )
      case Some( "log-and-default" ) => Some( bind[ RecoveryPolicy ].to[ LogAndDefaultPolicy ] )
      case Some( "log-condensed-and-fail" ) => Some( bind[ RecoveryPolicy ].to[ LogCondensedAndFailPolicy ] )
      case Some( "log-condensed-and-default" ) => Some( bind[ RecoveryPolicy ].to[ LogCondensedAndDefaultPolicy ] )
      case Some( "custom" ) => None // do nothing, user provides own implementation
      case Some( _ ) => invalidConfiguration( "Invalid value in 'play.cache.redis.recovery'. Accepted values are 'log-and-fail', 'log-and-default', and 'custom'." )
      case None => invalidConfiguration( "Key 'play.cache.redis.recovery' is mandatory. Accepted values are 'log-and-fail', 'log-and-default', and 'custom'." )
    }
  }
}
