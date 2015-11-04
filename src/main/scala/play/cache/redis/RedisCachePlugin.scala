package play.cache.redis

import javax.inject._

import play.api.Application
import play.cache.api.CachePlugin

/** <p>Non-blocking simple cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
@Singleton
class RedisCachePlugin @Inject() ( implicit r: RedisCache, app: Application ) extends CachePlugin {

  /** instance of cache */
  lazy val api: RedisCache = r

  override def onStart( ): Unit = {} //api.start( )

  override def onStop( ): Unit = {} //api.stop( )
}
