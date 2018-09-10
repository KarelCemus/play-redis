package play.api.cache.redis.impl

/**
  * Each instance can apply its own prefix, e.g., to use multiple instances
  * with the same redis database.
  */
sealed trait RedisPrefix extends Any {
  @inline def prefixed(key: String): String
  @inline def prefixed(key: Seq[String]): Seq[String]
}

class RedisPrefixImpl(val prefix: String) extends AnyVal with RedisPrefix {
  @inline def prefixed(key: String) = s"$prefix:$key"
  @inline def prefixed(keys: Seq[String]) = keys.map(prefixed)
}

object RedisEmptyPrefix extends RedisPrefix {
  @inline def prefixed(key: String) = key
  @inline def prefixed(keys: Seq[String]) = keys
}
