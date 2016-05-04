package play.api.cache.redis

import javax.inject.Singleton

import play.api.Environment
import play.api.inject.Module

/** Play framework module implementing play.api.cache.CacheApi for redis-server key/value storage. For more details
  * see README.
  *
  * @author Karel Cemus
  */
@Singleton
class RedisCacheModule extends Module {

  /** play-redis consists of several layers and sub-modules, each defining it's own bindings */
  private def layers = Seq(
    // the lowest layer, the connector
    connector.RedisConnectorModule,
    // the implementation
    impl.ImplementationModule,
    // the configuration provider
    configuration.ConfigurationModule
  )

  override def bindings( environment: Environment, configuration: play.api.Configuration ) = Seq(
    // extracts the configuration
    bind[ ConnectionSettings ].toProvider[ ConnectionSettingsProvider ]

  ) ++ layers.flatMap( _.bindings( environment, configuration ) )
}
