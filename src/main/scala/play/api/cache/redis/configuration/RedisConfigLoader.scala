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
      config match {
        case c if c.hasPath(path) => Some(f( c )( path ))
        case _ => None
      }
  }

  implicit class ConfigPath( val path: String ) extends AnyVal {

    private def eagerDecision(): Function1[String, String] =
      path.isEmpty match {
        case true => (suffix: String) => suffix
        case _ => (suffix: String) => path + "." + suffix
      }

    private def delegate: Function1[String, String] = eagerDecision()

    def /( suffix: String ): String = delegate(suffix)
  }

  implicit class FallbackValue[ T ]( val value: T ) extends AnyVal {
    def asFallback = ( _: String ) => value
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

  implicit final def loader( implicit defaults: RedisSettings ) = new ConfigLoader[ T ] {
    def load( config: Config, path: String ) = outer.load( config, path )
  }

  def load( config: Config, path: String )( implicit defaults: RedisSettings ): T
}

/**
  * Extended RedisConfig loader to a produce redis instance, it requires
  * a default settings to be able to actually load the configuration and its
  * name. This default settings are used as a fallback value when th
  * overloading settings are missing
  *
  * @author Karel Cemus
  */
private[ configuration ] trait RedisConfigInstanceLoader[ T ] { outer =>

  final def loader( name: String )( implicit defaults: RedisSettings ) = new ConfigLoader[ T ] {
    def load( config: Config, path: String ) = outer.load( config, path = path, name = name )
  }

  def load( config: Config, path: String, name: String )( implicit defaults: RedisSettings ): T
}
