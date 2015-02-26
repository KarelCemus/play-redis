package play.cache.redis

import play.api.Application
import play.cache.api.CachePlugin

/** <p>Non-blocking simple cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
class RedisCachePlugin( implicit app: Application ) extends CachePlugin {

  /** instance of cache */
  lazy val api: RedisCache = new RedisCache

  override def onStart( ): Unit = api.start( )

  override def onStop( ): Unit = api.stop( )
}
