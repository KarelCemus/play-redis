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
class RedisInstanceManager( val all: Map[ String, RedisInstanceBinder ] ) extends Traversable[ RedisInstanceBinder ] {

  /** names of all known redis caches */
  def caches: Set[ String ] = all.keySet

  /** returns a configuration of a single named redis instance */
  def instanceOfOption( name: String ): Option[ RedisInstanceBinder ] = all get name

  /** returns a configuration of a single named redis instance */
  def instanceOf( name: String ): RedisInstanceBinder = instanceOfOption( name ) getOrElse {
    throw new IllegalArgumentException( s"There is no cache named '$name'." )
  }

  /** traverse all binders */
  def foreach[ U ]( f: RedisInstanceBinder => U ) = all.values.foreach( f )

  /** join with another manager, returns a new instance */
  def ++( that: RedisInstanceManager ): RedisInstanceManager =
    new RedisInstanceManager( this.all ++ that.all )
}

private[ configuration ] object RedisInstanceManager extends ConfigLoader[ RedisInstanceManager ] {
  import RedisConfigLoader._

  def load( config: Config, path: String ) = {
    // read default settings
    implicit val defaults = RedisSettings.load( config, path )

    // construct a manager
    new RedisInstanceManager(
      config.getObject( path / "instance" ).keySet.asScala.map {
        name => name -> RedisInstanceBinder.load( config, path / "instance" / name, name )
      }.toMap
    )
  }
}
