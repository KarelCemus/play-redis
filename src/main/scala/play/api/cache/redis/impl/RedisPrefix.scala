package play.api.cache.redis.impl

/**
  * Each instance can apply its own prefix, e.g., to use multiple instances
  * with the same redis database.
  */
sealed trait RedisPrefix extends Any {
  @inline def prefixed(key: String): String
  @inline def unprefixed(key: String): String
  @inline def prefixed(key: Seq[String]): Seq[String]
  @inline def unprefixed(key: Seq[String]): Seq[String]
}

final class RedisPrefixImpl(val prefix: String) extends AnyVal with RedisPrefix {
  @inline override def prefixed(key: String): String = s"$prefix:$key"
  @inline override def unprefixed(key: String): String = key.drop(prefix.length + 1)
  @inline override def prefixed(keys: Seq[String]): Seq[String] = keys.map(prefixed)
  @inline override def unprefixed(keys: Seq[String]): Seq[String] = keys.map(unprefixed)
}

object RedisEmptyPrefix extends RedisPrefix {
  @inline override def prefixed(key: String): String = key
  @inline override def unprefixed(key: String): String = key
  @inline override def prefixed(keys: Seq[String]): Seq[String] = keys
  @inline override def unprefixed(keys: Seq[String]): Seq[String] = keys
}
