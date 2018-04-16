package play.api.cache.redis.configuration


import play.api.ConfigLoader

import com.typesafe.config.Config

/**
  * Config loader helper, provides some useful methods
  * easing config load
  *
  * @author Karel Cemus
  */
private[ configuration ] object RedisConfigLoader {

  implicit class ConfigOption( val config: Config ) extends AnyVal {
    def getOption[ T ]( path: String, f: Config => String => T ): Option[ T ] =
      if ( config hasPath path ) Some( f( config )( path ) ) else None
  }

  implicit class ConfigPath( val path: String ) extends AnyVal {
    def /( suffix: String ): String = if ( path == "" ) suffix else s"$path.$suffix"
  }

  def required( path: String ) = throw new IllegalStateException( s"Configuration key '$path' is missing." )
}

/**
  * Extended RedisConfig loader, it requires a default settings
  * to be able to actually load the configuration. This default
  * settings are used as a fallback value when the overloading
  * settings are missing
  *
  * @author Karel Cemus
  */
private[ configuration ] trait RedisConfigLoader[ T ] { outer =>

  def load( config: Config, path: String )( implicit defaults: RedisSettings ): T
}

/**
  * Extended RedisConfig loader to a produce redis instance, it requires
  * a default settings to be able to actually load the configuration and its name. This default
  * settings are used as a fallback value when the overloading
  * settings are missing
  *
  * @author Karel Cemus
  */
private[ configuration ] trait RedisConfigInstanceLoader[ T ] { outer =>

  def load( config: Config, path: String, name: String )( implicit defaults: RedisSettings ): T
}
