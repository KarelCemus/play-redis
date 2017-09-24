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
trait RedisInstanceManager extends Traversable[ RedisInstanceBinder ] {

  /** names of all known redis caches */
  def caches: Set[ String ]

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceBinder ]

  /** returns a configuration of a single named redis instance */
  def instanceOf( name: String ): RedisInstanceBinder = instanceOfOption( name ) getOrElse {
    throw new IllegalArgumentException( s"There is no cache named '$name'." )
  }

  /** traverse all binders */
  def foreach[ U ]( f: RedisInstanceBinder => U ) = caches.view.flatMap( instanceOfOption ).foreach( f )
}

private[ configuration ] object RedisInstanceManager extends ConfigLoader[ RedisInstanceManager ] {
  import RedisConfigLoader._

  def load( config: Config, path: String ) = {
    // read default settings
    implicit val defaults = RedisSettings.load( config, path )
    // check if the list of instances is defined or whether to use
    // a fallback definition directly under the configuration root
    val hasInstances = config.hasPath( path / "instances" )
    // construct a manager
    if ( hasInstances ) new RedisInstanceManagerImpl( config, path ) else new RedisInstanceManagerFallback( config, path )
  }
}

/**
  * Redis manager reading 'play.cache.redis.instances' tree for cache definitions.
  */
class RedisInstanceManagerImpl( config: Config, path: String )( implicit defaults: RedisSettings ) extends RedisInstanceManager {
  import RedisConfigLoader._

  /** names of all known redis caches */
  def caches: Set[ String ] = config.getObject( path / "instances" ).keySet.asScala.toSet

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceBinder ] =
    if ( config hasPath ( path / "instances" / name ) ) Some( RedisInstanceBinder.load( config, path / "instances" / name, name ) ) else None
}

/**
  * Redis manager reading 'play.cache.redis' root for a single fallback default cache.
  */
class RedisInstanceManagerFallback( config: Config, path: String )( implicit defaults: RedisSettings ) extends RedisInstanceManager {
  import RedisConfigLoader._

  private val name = config.getString( path / "default-cache" )

  /** names of all known redis caches */
  def caches: Set[ String ] = Set( name )

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceBinder ] =
    if ( name == this.name ) Some( RedisInstanceBinder.load( config, path, name ) ) else None
}
