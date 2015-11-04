package play.cache.redis

import javax.inject._

import play.api._
import play.cache.api.{CachePlugin, CachePlugin20}

/** <p>Non-blocking advanced cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
@Singleton
class RedisCachePlugin20 @Inject() ( implicit app: Application, plugin: CachePlugin ) extends CachePlugin20 {

  /** instance of cache */
  lazy val api: RedisCache20 = {
    // load internal cache api
    val internal = plugin.api
    // create advanced wrapper
    new RedisCache20( internal )
  }

  override def onStart( ): Unit = {
    // on plugin start reload the cache to look up the newest plugin version
    // this is a minor hack because on application recompilation the plugins
    // are restarted but singletons such as AsyncCache are not somehow reinitialized
  }

  plugin.onStart()
//  AsyncCache.reload()
}