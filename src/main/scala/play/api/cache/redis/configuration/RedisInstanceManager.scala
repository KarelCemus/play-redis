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
class RedisInstanceManager( config: Config, path: String )( implicit defaults: RedisSettings ) extends Traversable[ RedisInstanceBinder ] {
  import RedisConfigLoader._

  /** names of all known redis caches */
  def caches: Set[ String ] = config.getObject( path / "instance" ).keySet.asScala.toSet

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceBinder ] =
    if ( config hasPath ( path / "instance" / name ) ) Some( RedisInstanceBinder.load( config, path / "instance" / name, name ) ) else None

  /** returns a configuration of a single named redis instance */
  def instanceOf( name: String ): RedisInstanceBinder = instanceOfOption( name ) getOrElse {
    throw new IllegalArgumentException( s"There is no cache named '$name'." )
  }

  /** traverse all binders */
  def foreach[ U ]( f: RedisInstanceBinder => U ) = caches.view.flatMap( instanceOfOption ).foreach( f )
}

private[ configuration ] object RedisInstanceManager extends ConfigLoader[ RedisInstanceManager ] {

  def load( config: Config, path: String ) = {
    // read default settings
    implicit val defaults = RedisSettings.load( config, path )
    // construct a manager
    new RedisInstanceManager( config, path )
  }
}
