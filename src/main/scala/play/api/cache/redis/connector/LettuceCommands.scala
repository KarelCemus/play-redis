package play.api.cache.redis.connector

import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.{KeyValue, RedisFuture, ScoredValue, SetArgs}

/**
  * The subset of the Lettuce commands the connector uses. The connector
  * depends on this subset rather than on the whole Lettuce API, so a mock of
  * it in tests stays small while Lettuce keeps adding commands.
  */
private[connector] trait LettuceCommands {

  def append(key: String, value: String): RedisFuture[java.lang.Long]

  def del(keys: String*): RedisFuture[java.lang.Long]

  def exists(keys: String*): RedisFuture[java.lang.Long]

  def expire(key: String, seconds: Long): RedisFuture[java.lang.Boolean]

  def flushdb(): RedisFuture[String]

  def get(key: String): RedisFuture[String]

  def hdel(key: String, fields: String*): RedisFuture[java.lang.Long]

  def hexists(key: String, field: String): RedisFuture[java.lang.Boolean]

  def hget(key: String, field: String): RedisFuture[String]

  def hgetall(key: String): RedisFuture[java.util.Map[String, String]]

  def hincrby(key: String, field: String, amount: Long): RedisFuture[java.lang.Long]

  def hkeys(key: String): RedisFuture[java.util.List[String]]

  def hlen(key: String): RedisFuture[java.lang.Long]

  def hmget(key: String, fields: String*): RedisFuture[java.util.List[KeyValue[String, String]]]

  def hset(key: String, field: String, value: String): RedisFuture[java.lang.Boolean]

  def hvals(key: String): RedisFuture[java.util.List[String]]

  def incrby(key: String, amount: Long): RedisFuture[java.lang.Long]

  def keys(pattern: String): RedisFuture[java.util.List[String]]

  def linsert(key: String, before: Boolean, pivot: String, value: String): RedisFuture[java.lang.Long]

  def llen(key: String): RedisFuture[java.lang.Long]

  def lpop(key: String): RedisFuture[String]

  def lpush(key: String, values: String*): RedisFuture[java.lang.Long]

  def lrange(key: String, start: Long, stop: Long): RedisFuture[java.util.List[String]]

  def lrem(key: String, count: Long, value: String): RedisFuture[java.lang.Long]

  def lset(key: String, index: Long, value: String): RedisFuture[String]

  def ltrim(key: String, start: Long, stop: Long): RedisFuture[String]

  def mget(keys: String*): RedisFuture[java.util.List[KeyValue[String, String]]]

  def mset(map: java.util.Map[String, String]): RedisFuture[String]

  def msetnx(map: java.util.Map[String, String]): RedisFuture[java.lang.Boolean]

  def ping(): RedisFuture[String]

  def pttl(key: String): RedisFuture[java.lang.Long]

  def rpush(key: String, values: String*): RedisFuture[java.lang.Long]

  def sadd(key: String, members: String*): RedisFuture[java.lang.Long]

  def scard(key: String): RedisFuture[java.lang.Long]

  def set(key: String, value: String, setArgs: SetArgs): RedisFuture[String]

  def sismember(key: String, member: String): RedisFuture[java.lang.Boolean]

  def smembers(key: String): RedisFuture[java.util.Set[String]]

  def srem(key: String, members: String*): RedisFuture[java.lang.Long]

  def zadd(key: String, scoredValues: ScoredValue[String]*): RedisFuture[java.lang.Long]

  def zcard(key: String): RedisFuture[java.lang.Long]

  def zrange(key: String, start: Long, stop: Long): RedisFuture[java.util.List[String]]

  def zrem(key: String, members: String*): RedisFuture[java.lang.Long]

  def zrevrange(key: String, start: Long, stop: Long): RedisFuture[java.util.List[String]]

  def zscore(key: String, member: String): RedisFuture[java.lang.Double]
}

private[connector] object LettuceCommands {

  def apply(redis: RedisClusterAsyncCommands[String, String]): LettuceCommands =
    new LettuceCommands {
      override def append(key: String, value: String): RedisFuture[java.lang.Long] = redis.append(key, value)
      override def del(keys: String*): RedisFuture[java.lang.Long] = redis.del(keys: _*)
      override def exists(keys: String*): RedisFuture[java.lang.Long] = redis.exists(keys: _*)
      override def expire(key: String, seconds: Long): RedisFuture[java.lang.Boolean] = redis.expire(key, seconds)
      override def flushdb(): RedisFuture[String] = redis.flushdb()
      override def get(key: String): RedisFuture[String] = redis.get(key)
      override def hdel(key: String, fields: String*): RedisFuture[java.lang.Long] = redis.hdel(key, fields: _*)
      override def hexists(key: String, field: String): RedisFuture[java.lang.Boolean] = redis.hexists(key, field)
      override def hget(key: String, field: String): RedisFuture[String] = redis.hget(key, field)
      override def hgetall(key: String): RedisFuture[java.util.Map[String, String]] = redis.hgetall(key)
      override def hincrby(key: String, field: String, amount: Long): RedisFuture[java.lang.Long] = redis.hincrby(key, field, amount)
      override def hkeys(key: String): RedisFuture[java.util.List[String]] = redis.hkeys(key)
      override def hlen(key: String): RedisFuture[java.lang.Long] = redis.hlen(key)
      override def hmget(key: String, fields: String*): RedisFuture[java.util.List[KeyValue[String, String]]] = redis.hmget(key, fields: _*)
      override def hset(key: String, field: String, value: String): RedisFuture[java.lang.Boolean] = redis.hset(key, field, value)
      override def hvals(key: String): RedisFuture[java.util.List[String]] = redis.hvals(key)
      override def incrby(key: String, amount: Long): RedisFuture[java.lang.Long] = redis.incrby(key, amount)
      override def keys(pattern: String): RedisFuture[java.util.List[String]] = redis.keys(pattern)
      override def linsert(key: String, before: Boolean, pivot: String, value: String): RedisFuture[java.lang.Long] = redis.linsert(key, before, pivot, value)
      override def llen(key: String): RedisFuture[java.lang.Long] = redis.llen(key)
      override def lpop(key: String): RedisFuture[String] = redis.lpop(key)
      override def lpush(key: String, values: String*): RedisFuture[java.lang.Long] = redis.lpush(key, values: _*)
      override def lrange(key: String, start: Long, stop: Long): RedisFuture[java.util.List[String]] = redis.lrange(key, start, stop)
      override def lrem(key: String, count: Long, value: String): RedisFuture[java.lang.Long] = redis.lrem(key, count, value)
      override def lset(key: String, index: Long, value: String): RedisFuture[String] = redis.lset(key, index, value)
      override def ltrim(key: String, start: Long, stop: Long): RedisFuture[String] = redis.ltrim(key, start, stop)
      override def mget(keys: String*): RedisFuture[java.util.List[KeyValue[String, String]]] = redis.mget(keys: _*)
      override def mset(map: java.util.Map[String, String]): RedisFuture[String] = redis.mset(map)
      override def msetnx(map: java.util.Map[String, String]): RedisFuture[java.lang.Boolean] = redis.msetnx(map)
      override def ping(): RedisFuture[String] = redis.ping()
      override def pttl(key: String): RedisFuture[java.lang.Long] = redis.pttl(key)
      override def rpush(key: String, values: String*): RedisFuture[java.lang.Long] = redis.rpush(key, values: _*)
      override def sadd(key: String, members: String*): RedisFuture[java.lang.Long] = redis.sadd(key, members: _*)
      override def scard(key: String): RedisFuture[java.lang.Long] = redis.scard(key)
      override def set(key: String, value: String, setArgs: SetArgs): RedisFuture[String] = redis.set(key, value, setArgs)
      override def sismember(key: String, member: String): RedisFuture[java.lang.Boolean] = redis.sismember(key, member)
      override def smembers(key: String): RedisFuture[java.util.Set[String]] = redis.smembers(key)
      override def srem(key: String, members: String*): RedisFuture[java.lang.Long] = redis.srem(key, members: _*)
      override def zadd(key: String, scoredValues: ScoredValue[String]*): RedisFuture[java.lang.Long] = redis.zadd(key, scoredValues: _*)
      override def zcard(key: String): RedisFuture[java.lang.Long] = redis.zcard(key)
      override def zrange(key: String, start: Long, stop: Long): RedisFuture[java.util.List[String]] = redis.zrange(key, start, stop)
      override def zrem(key: String, members: String*): RedisFuture[java.lang.Long] = redis.zrem(key, members: _*)
      override def zrevrange(key: String, start: Long, stop: Long): RedisFuture[java.util.List[String]] = redis.zrevrange(key, start, stop)
      override def zscore(key: String, member: String): RedisFuture[java.lang.Double] = redis.zscore(key, member)
    }

}
