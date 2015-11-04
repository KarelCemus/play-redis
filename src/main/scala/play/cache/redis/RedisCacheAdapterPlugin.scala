package play.cache.redis

import javax.inject._

import play.api.cache.CacheApi
import play.api.{Plugin, Application, Play}
import play.cache.api.CachePlugin20

/** <p>Non-blocking simple cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
@Singleton
class RedisCacheAdapterPlugin @Inject() ( implicit app: Application, plugin: CachePlugin20 ) extends Plugin {

  /** instance of cache */
  lazy val api: CacheApi = {
    // load internal cache api
    val internal = plugin.api
    plugin.onStart()
    new RedisCacheAdapter( internal )
  }
}
