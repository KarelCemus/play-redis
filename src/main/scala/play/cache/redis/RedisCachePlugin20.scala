package play.cache.redis

import play.api._
import play.cache.api.{CachePlugin, CachePlugin20}

/** <p>Non-blocking advanced cache plugin implementation. Implementation provides simple access to Redis cache.</p> */
class RedisCachePlugin20( implicit app: Application ) extends CachePlugin20 {

  /** instance of cache */
  lazy val api: RedisCache20 = {
    // load internal cache api
    val internal = Play.current.plugin[ CachePlugin ] match {
      case Some( plugin ) => plugin.api
      case None => throw new Exception( "There is no cache plugin registered. Make sure at least one play.cache.redis.CachePlugin implementation is enabled." )
    }
    // create advanced wrapper
    new RedisCache20( internal )
  }
}