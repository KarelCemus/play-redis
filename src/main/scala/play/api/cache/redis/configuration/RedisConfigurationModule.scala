package play.api.cache.redis.configuration

import scala.language.implicitConversions

import play.api.Configuration
import play.api.inject._

/**
  * Extracts the configuration and binds with DI for upper layers.
  *
  * @author Karel Cemus
  */
trait RedisConfigurationModule {

  def configuration: Configuration

  lazy val manager = configuration.get( "play.cache.redis" )( RedisInstanceManager )

  private[ redis ] def configurationBindings = manager.foldLeft( List.empty[ Binding[ RedisInstance ] ] ) {
    ( binding, binder ) => binder.toBinding ::: binding
  }
}


/**
  * Components for compile-time dependency injection.
  * It binds components from configuration package
  *
  * @author Karel Cemus
  */
private[ redis ] trait ConfigurationComponents {

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
