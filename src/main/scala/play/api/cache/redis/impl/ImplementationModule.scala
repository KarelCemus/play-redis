package play.api.cache.redis.impl

import play.api.cache.redis.{CacheApi, CacheAsyncApi}
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
    bind[ play.cache.CacheApi ].to[ JavaRedis ],
    // recovery policy
    bind[ RecoveryPolicy ].to[ LogAndFailPolicy ]
  )
}
