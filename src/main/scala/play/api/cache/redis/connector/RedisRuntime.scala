package play.api.cache.redis.connector

import scala.concurrent.ExecutionContext

/**
  * @author Karel Cemus
  */
private[ redis ] trait RedisRuntime {
  def name: String
  implicit def context: ExecutionContext
}
