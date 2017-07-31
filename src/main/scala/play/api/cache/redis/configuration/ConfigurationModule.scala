package play.api.cache.redis.configuration

import play.api.Configuration
import play.api.inject._

/**
  * Extracts the configuration and binds with DI for upper layers.
  *
  * @author Karel Cemus
  */
object ConfigurationModule extends SimpleModule( ( environment, configuration ) => {

  configuration.get( "play.cache.redis" )( RedisInstanceManager ).foldLeft( List.empty[ Binding[ RedisInstance ] ] ) {
    ( binding, binder ) => binder.toBinding ::: binding
  }
} )


/**
  * Components for compile-time dependency injection.
  * It binds components from configuration package
  *
  * @author Karel Cemus
  */
private[ redis ] trait ConfigurationComponents {

  def configuration: Configuration

  /** creates a redis manager from the configuration */
  final def newRedisCacheManager = configuration.get( "play.cache.redis" )( RedisInstanceManager )

  /**
    *
    * To provide custom cache configuration override this variable,
    * define custom RedisInstanceManager and fold it with the newRedisCacheManager
    * creating the original configuration. Join managers by `++` operation.
    *
    * `
    *   override lazy val redisCacheManager = newRedisCacheManager ++ new RedisInstanceManager( Map.empty )
    * `
    */
  lazy val redisCacheManager = newRedisCacheManager
}
