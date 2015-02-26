package play.cache.redis

import play.api.cache.{CacheAPI, CachePlugin}
import play.api.{Application, Play}
import play.cache.api.CachePlugin20

/** <p>Non-blocking simple cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
class RedisCacheAdapterPlugin( implicit app: Application ) extends CachePlugin {

  /** instance of cache */
  lazy val api: CacheAPI = {
    // load internal cache api
    val internal = Play.current.plugin[ CachePlugin20 ] match {
      case Some( plugin ) => plugin.api
      case None => throw new Exception( "There is no cache plugin registered. Make sure at least one play.cache.redis.CachePlugin20 implementation is enabled." )
    }

    new RedisCacheAdapter( internal )
  }
}
