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

  override def bindings( environment: Environment, config: play.api.Configuration ) = {
    // play-redis consists of several layers and sub-modules, each defining it's own bindings
    val module = new connector.RedisConnectorModule with configuration.RedisConfigurationModule with impl.ImplementationModule {
      def configuration = config
    }
    module.connectorBindings ++ module.configurationBindings ++ module.implementationBindings
  }
}

/**
  * <p>Redis cache components for use with compile time dependency injection.</p>
  *
  * @author Karel Cemus
  */
trait RedisCacheComponents
  extends configuration.ConfigurationComponents
  with connector.RedisConnectorComponents
  with impl.ImplementationComponents
