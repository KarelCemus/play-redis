package play.api.cache.redis.configuration

import scala.collection.JavaConverters._

import play.api.ConfigLoader

import com.typesafe.config.Config

/**
  * Cache manager maintains a list of the redis caches in the application.
  * It also provides a configuration of the instance based on the name
  * of the cache.
  *
  * This object should be used only during the configuration phase to
  * simplify binding creation and application configuration. While
  * the application is running, there should be no need to use this
  * manager.
  *
  * @author Karel Cemus
  */
trait RedisInstanceManager extends Traversable[ RedisInstanceProvider ] {

  /** names of all known redis caches */
  def caches: Set[ String ]

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceProvider ]

  /** returns a configuration of a single named redis instance */
  def instanceOf( name: String ): RedisInstanceProvider = instanceOfOption( name ) getOrElse {
    throw new IllegalArgumentException( s"There is no cache named '$name'." )
  }

  /** returns the default instance */
  def defaultInstance: RedisInstanceProvider

  /** traverse all binders */
  def foreach[ U ]( f: RedisInstanceProvider => U ) = caches.view.flatMap( instanceOfOption ).foreach( f )
}

private[ redis ] object RedisInstanceManager extends ConfigLoader[ RedisInstanceManager ] {
  import RedisConfigLoader._

  def load( config: Config, path: String ) = {
    // read default settings
    implicit val defaults = RedisSettings.load( config, path )
    // check if the list of instances is defined or whether to use
    // a fallback definition directly under the configuration root
    val hasInstances = config.hasPath( path / "instances" )

    // construct a manager
    config.hasPath( path / "instances" ) match {
      case true => new RedisInstanceManagerImpl( config, path )
      case false => new RedisInstanceManagerFallback( config, path )
    }
  }
}

/**
  * Redis manager reading 'play.cache.redis.instances' tree for cache definitions.
  */
class RedisInstanceManagerImpl( config: Config, path: String )( implicit defaults: RedisSettings ) extends RedisInstanceManager {
  import RedisConfigLoader._

  /** names of all known redis caches */
  def caches: Set[ String ] = config.getObject( path / "instances" ).keySet.asScala.toSet

  def defaultCacheName = config.getString( path / "default-cache" )

  def defaultInstance = instanceOfOption( defaultCacheName ) getOrElse {
    throw new IllegalArgumentException( s"Default cache '$defaultCacheName' is not defined." )
  }

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceProvider ] =
    if ( config hasPath ( path / "instances" / name ) ) Some( RedisInstanceProvider.load( config, path / "instances" / name, name ) ) else None
}

/**
  * Redis manager reading 'play.cache.redis' root for a single fallback default cache.
  */
class RedisInstanceManagerFallback( config: Config, path: String )( implicit defaults: RedisSettings ) extends RedisInstanceManager {
  import RedisConfigLoader._

  private val name = config.getString( path / "default-cache" )

  /** names of all known redis caches */
  def caches: Set[ String ] = Set( name )

  def defaultInstance = RedisInstanceProvider.load( config, path, name )

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceProvider ] =
    if ( name == this.name ) Some( defaultInstance ) else None
}
