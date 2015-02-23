package play.plugin.redis

import play.api._

trait CachePlugin extends Plugin {

  /** Implementation of the the Cache plugin provided by this plugin. */
  def api: CacheAPI
}

trait ExtendedCachePlugin extends Plugin {

  /** Implementation of the the Cache plugin provided by this plugin. */
  def api: ExtendedCacheAPI
}

/** <p>Non-blocking simple cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
class RedisCachePlugin( implicit app: Application ) extends CachePlugin {

  /** instance of cache */
  lazy val api: RedisCache = new RedisCache

  override def onStart( ): Unit = api.start( )

  override def onStop( ): Unit = api.stop( )
}

/** <p>Non-blocking advanced cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
class ExtendedRedisCachePlugin( implicit app: Application ) extends ExtendedCachePlugin {

  /** instance of cache */
  lazy val api: ExtendedRedisCache = {
    // load internal cache api
    val internal = Play.current.plugin[ CachePlugin ] match {
      case Some( plugin ) => plugin.api
      case None => throw new Exception( "There is no cache plugin registered. Make sure at least one play.plugin.redis.CachePlugin implementation is enabled." )
    }
    // create advanced wrapper
    new ExtendedRedisCache( internal )
  }
}


