package play.api.cache.redis

import scala.language.higherKinds

/**
  * @author Karel Cemus
  */
private[ redis ] trait RedisCollection[ Collection ] {

  type This = this.type
}
