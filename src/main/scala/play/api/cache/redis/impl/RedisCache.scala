package play.api.cache.redis.impl

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag
import play.api.cache.redis._

import scala.collection.{AbstractSeq, Iterable}

/** <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p> */
private[impl] class RedisCache[Result[_]](redis: RedisConnector, builder: Builders.ResultBuilder[Result])(implicit runtime: RedisRuntime) extends AbstractCacheApi[Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline implicit protected def implicitBuilder: Builders.ResultBuilder[Result] = builder

  def get[T: ClassTag](key: String) = key.prefixed { key =>
    redis.get[T](key).recoverWithDefault(None)
  }

  def getAll[T: ClassTag](keys: String*): Result[Seq[Option[T]]] = keys.prefixed { keys =>
    redis.mGet[T](keys: _*).recoverWithDefault(keys.toList.map(_ => None))
  }

  def set(key: String, value: Any, expiration: Duration) = key.prefixed { key =>
    redis.set(key, value, expiration).map(_ => (): Unit).recoverWithDone
  }

  def setIfNotExists(key: String, value: Any, expiration: Duration) = key.prefixed { key =>
    redis.set(key, value, expiration, ifNotExists = true).recoverWithDefault(true)
  }

  def setAll(keyValues: (String, Any)*): Result[Done] = keyValues.prefixed { keyValues =>
    redis.mSet(keyValues: _*).recoverWithDone
  }

  def setAllIfNotExist(keyValues: (String, Any)*): Result[Boolean] = keyValues.prefixed { keyValues =>
    redis.mSetIfNotExist(keyValues: _*).recoverWithDefault(true)
  }

  def append(key: String, value: String, expiration: Duration): Result[Done] = key.prefixed { key =>
    redis.append(key, value).flatMap { result =>
      // if the new string length is equal to the appended string, it means they should equal
      // when the finite duration is required, set it
      if (result == value.length && expiration.isFinite()) redis.expire(key, expiration) else Future.successful[Unit](Unit)
    }.recoverWithDone
  }

  def expire(key: String, expiration: Duration) = key.prefixed { key =>
    redis.expire(key, expiration).recoverWithDone
  }

  /**
    * cached implementation of the matching function
    *
    * - when a prefix is empty, it simply delegates the invocation to the connector
    * - when a prefix is defined, it unprefixes the keys when returned
    */
  private val doMatching = runtime.prefix match {
    case RedisEmptyPrefix => (pattern: String) => redis.matching(pattern)
    case prefix => (pattern: String) => redis.matching(pattern).map(_.unprefixed)
  }

  def matching(pattern: String) = pattern.prefixed { pattern =>
    doMatching(pattern).recoverWithDefault(Seq.empty[String])
  }

  def getOrElse[T: ClassTag](key: String, expiration: Duration)(orElse: => T) = key.prefixed { key =>
    getOrFuture(key, expiration)(orElse.toFuture).recoverWithDefault(orElse)
  }

  def getOrFuture[T: ClassTag](key: String, expiration: Duration)(orElse: => Future[T]): Future[T] = key.prefixed { key =>
    redis.get[T](key).flatMap {
      // cache hit, return the unwrapped value
      case Some(value: T) => value.toFuture
      // cache miss, compute the value, store it into the cache but do not wait for the result and ignore it, directly return the value
      case None           => orElse flatMap { value => runtime.invocation.invoke(redis.set(key, value, expiration), thenReturn = value) }
    }.recoverWithFuture(orElse)
  }

  def remove(key: String) = key.prefixed { key =>
    redis.remove(key).recoverWithDone
  }

  def remove(key1: String, key2: String, keys: String*) = (key1 +: key2 +: keys).prefixed { keys =>
    redis.remove(keys: _*).recoverWithDone
  }

  def removeAll(keys: String*): Result[Done] = keys.prefixed { keys =>
    redis.remove(keys: _*).recoverWithDone
  }

  def removeMatching(pattern: String): Result[Done] = pattern.prefixed { pattern =>
    redis.matching(pattern).flatMap(keys => redis.remove(keys: _*)).recoverWithDone
  }

  def invalidate() =
    redis.invalidate().recoverWithDone

  def exists(key: String) = key.prefixed { key =>
    redis.exists(key).recoverWithDefault(false)
  }

  def increment(key: String, by: Long) = key.prefixed { key =>
    redis.increment(key, by).recoverWithDefault(by)
  }

  def decrement(key: String, by: Long) = key.prefixed { key =>
    increment(key, -by)
  }

  def list[T: ClassTag](key: String): RedisList[T, Result] = key.prefixed { key =>
    new RedisListImpl(key, redis)
  }

  def set[T: ClassTag](key: String): RedisSet[T, Result] = key.prefixed { key =>
    new RedisSetImpl(key, redis)
  }

  def map[T: ClassTag](key: String): RedisMap[T, Result] = key.prefixed { key =>
    new RedisMapImpl(key, redis)
  }

  // $COVERAGE-OFF$
  override def toString = s"RedisCache(name=${runtime.name})"

  // $COVERAGE-ON$
}
