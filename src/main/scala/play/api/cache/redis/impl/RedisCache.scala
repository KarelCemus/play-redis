package play.api.cache.redis.impl

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p> */
private[impl] class RedisCache[Result[_]](redis: RedisConnector, builder: Builders.ResultBuilder[Result])(implicit runtime: RedisRuntime) extends AbstractCacheApi[Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline implicit protected def implicitBuilder: Builders.ResultBuilder[Result] = builder

  override def get[T: ClassTag](key: String): Result[Option[T]] =
    key.prefixed { key =>
      redis.get[T](key).recoverWithDefault(None)
    }

  override def getAll[T: ClassTag](keys: Iterable[String]): Result[Seq[Option[T]]] =
    keys.toSeq.prefixed { keys =>
      redis.mGet[T](keys: _*).recoverWithDefault(keys.toList.map(_ => None))
    }

  override def set(key: String, value: Any, expiration: Duration): Result[Done] =
    key.prefixed { key =>
      redis.set(key, value, expiration).map(_ => (): Unit).recoverWithDone
    }

  override def setIfNotExists(key: String, value: Any, expiration: Duration): Result[Boolean] =
    key.prefixed { key =>
      redis.set(key, value, expiration, ifNotExists = true).recoverWithDefault(true)
    }

  override def setAll(keyValues: (String, Any)*): Result[Done] =
    keyValues.prefixed { keyValues =>
      redis.mSet(keyValues: _*).recoverWithDone
    }

  override def setAllIfNotExist(keyValues: (String, Any)*): Result[Boolean] =
    keyValues.prefixed { keyValues =>
      redis.mSetIfNotExist(keyValues: _*).recoverWithDefault(true)
    }

  override def append(key: String, value: String, expiration: Duration): Result[Done] =
    key.prefixed { key =>
      redis.append(key, value).flatMap { result =>
        // if the new string length is equal to the appended string, it means they should equal
        // when the finite duration is required, set it
        if (result === value.length && expiration.isFinite) redis.expire(key, expiration) else Future.successful[Unit](())
      }.recoverWithDone
    }

  override def expire(key: String, expiration: Duration): Result[Done] =
    key.prefixed { key =>
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
    case _ => (pattern: String) => redis.matching(pattern).map(_.unprefixed)
  }

  override def matching(pattern: String): Result[Seq[String]] = pattern.prefixed { pattern =>
    doMatching(pattern).recoverWithDefault(Seq.empty[String])
  }

  override def getOrElse[T: ClassTag](key: String, expiration: Duration)(orElse: => T): Result[T] =
    getOrFuture(key, expiration)(orElse.toFuture).recoverWithDefault(orElse)

  override def getOrFuture[T: ClassTag](key: String, expiration: Duration)(orElse: => Future[T]): Future[T] =
    key.prefixed { key =>
      redis.get[T](key).flatMap {
        // cache hit, return the unwrapped value
        case Some(value) => value.toFuture
        // cache miss, compute the value, store it into the cache but do not wait for the result and ignore it, directly return the value
        case None => orElse flatMap { value => runtime.invocation.invoke(redis.set(key, value, expiration), thenReturn = value) }
      }.recoverWithFuture(orElse)
    }

  override def remove(key: String): Result[Done] =
    key.prefixed { key =>
      redis.remove(key).recoverWithDone
    }

  override def remove(key1: String, key2: String, keys: String*): Result[Done] = (key1 +: key2 +: keys).prefixed { keys =>
    redis.remove(keys: _*).recoverWithDone
  }

  override def removeAll(keys: String*): Result[Done] =
    keys.prefixed { keys =>
      redis.remove(keys: _*).recoverWithDone
    }

  override def removeMatching(pattern: String): Result[Done] = pattern.prefixed { pattern =>
    redis.matching(pattern).flatMap(keys => redis.remove(keys: _*)).recoverWithDone
  }

  override def invalidate(): Result[Done] =
    redis.invalidate().recoverWithDone

  override def exists(key: String): Result[Boolean] =
    key.prefixed { key =>
      redis.exists(key).recoverWithDefault(false)
    }

  override def expiresIn(key: String): Result[Option[Duration]] =
    key.prefixed { key =>
      redis.expiresIn(key).recoverWithDefault(None)
    }

  override def increment(key: String, by: Long): Result[Long] =
    key.prefixed { key =>
      redis.increment(key, by).recoverWithDefault(by)
    }

  override def decrement(key: String, by: Long): Result[Long] =
    key.prefixed { key =>
      increment(key, -by)
    }

  override def list[T: ClassTag](key: String): RedisList[T, Result] =
    key.prefixed { key =>
      new RedisListImpl[T, Result](key, redis)
    }

  override def set[T: ClassTag](key: String): RedisSet[T, Result] =
    key.prefixed { key =>
      new RedisSetImpl[T, Result](key, redis)
    }

  override def map[T: ClassTag](key: String): RedisMap[T, Result] =
    key.prefixed { key =>
      new RedisMapImpl[T, Result](key, redis)
    }

  override def zset[T: ClassTag](key: String): RedisSortedSet[T, Result] =
    key.prefixed { key =>
      new RedisSortedSetImpl[T, Result](key, redis)
    }

  // $COVERAGE-OFF$
  override def toString = s"RedisCache(name=${runtime.name})"
  // $COVERAGE-ON$
}
