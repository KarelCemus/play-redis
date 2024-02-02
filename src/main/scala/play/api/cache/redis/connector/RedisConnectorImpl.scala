package play.api.cache.redis.connector

import play.api.Logger
import play.api.cache.redis._
import redis._

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
  * The connector directly connects with the REDIS instance, implements protocol commands
  * and is supposed to by used internally by another wrappers. The connector does not
  * directly implement [[play.api.cache.redis.CacheApi]] but provides fundamental functionality.
  *
  * @param serializer encodes/decodes objects into/from a string
  * @param redis      implementation of the commands
  */
private[connector] class RedisConnectorImpl(serializer: AkkaSerializer, redis: RedisCommands)(implicit runtime: RedisRuntime) extends RedisConnector {
  import ExpectedFuture._

  import runtime._

  /** logger instance */
  protected val log: Logger = Logger("play.api.cache.redis")

  override def get[T: ClassTag](key: String): Future[Option[T]] =
    redis.get[String](key) executing "GET" withKey key expects {
      case Some(response: String) =>
        log.trace(s"Hit on key '$key'.")
        Some(decode[T](key, response))
      case None =>
        log.debug(s"Miss on key '$key'.")
        None
    }

  override def mGet[T: ClassTag](keys: String*): Future[Seq[Option[T]]] =
    redis.mget[String](keys: _*) executing "MGET" withKeys keys expects {
      // list is always returned
      case list => keys.zip(list).map {
        case (key, Some(response: String)) =>
          log.trace(s"Hit on key '$key'.")
          Some(decode[T](key, response))
        case (key, None) =>
          log.debug(s"Miss on key '$key'.")
          None
      }
    }

  /** decodes the object, reports an exception if fails */
  private def decode[T: ClassTag](key: String, encoded: String): T =
    serializer.decode[T](encoded).recover {
      case ex => serializationFailed(key, "Deserialization failed", ex)
    }.get

  override def set(key: String, value: Any, expiration: Duration, ifNotExists: Boolean): Future[Boolean] =
    // no value to set
    if (Option(value).isEmpty) remove(key).map(_ => true)
    // set the value
    else encode(key, value) flatMap (doSet(key, _, expiration, ifNotExists))

  /** encodes the object, reports an exception if fails */
  private def encode(key: String, value: Any): Future[String] = Future.fromTry {
    serializer.encode(value).recover {
      case ex => serializationFailed(key, "Serialization failed", ex)
    }
  }

  /** implements the advanced set operation storing already encoded value into the storage */
  private def doSet(key: String, value: String, expiration: Duration, ifNotExists: Boolean): Future[Boolean] =
    redis.set[String](
      key,
      value,
      pxMilliseconds = if (expiration.isFinite) Some(expiration.toMillis) else None,
      NX = ifNotExists
    ) executing "SET" withKey key andParameters s"$value${s" PX $expiration" when expiration.isFinite}${" NX" when ifNotExists}" logging {
      case true if expiration.isFinite => log.debug(s"Set on key '$key' for ${expiration.toMillis} milliseconds.")
      case true                        => log.debug(s"Set on key '$key' for infinite seconds.")
      case false                       => log.debug(s"Set on key '$key' ignored. Condition was not met.")
    }

  override def mSet(keyValues: (String, Any)*): Future[Unit] = mSetUsing(mSetEternally, (), keyValues: _*)

  override def mSetIfNotExist(keyValues: (String, Any)*): Future[Boolean] = mSetUsing(mSetEternallyIfNotExist, true, keyValues: _*)

  /** eternally stores or removes all given values, using the given mSet implementation */
  private def mSetUsing[T](mSet: Seq[(String, String)] => Future[T], default: T, keyValues: (String, Any)*): Future[T] = {
    val (toBeRemoved, toBeSet) = keyValues.partition(_.isNull)
    // remove all keys to be removed
    val toBeRemovedFuture = if (toBeRemoved.isEmpty) Future.successful(()) else remove(toBeRemoved.map(_.key): _*)
    // set all keys to be set
    val toBeSetFuture = if (toBeSet.isEmpty) Future.successful(default) else Future sequence toBeSet.map(tuple => encode(tuple.key, tuple.value).map(tuple.key -> _)) flatMap mSet
    // combine futures ignoring the result of removal
    toBeRemovedFuture.flatMap(_ => toBeSetFuture)
  }

  /** eternally stores already encoded values into the storage */
  private def mSetEternally(keyValues: (String, String)*): Future[Unit] =
    redis.mset(keyValues.toMap) executing "MSET" withKeys keyValues.map(_._1) asCommand keyValues.map(_.asString).mkString(" ") logging {
      case _ => log.debug(s"Set on keys ${keyValues.map(_.key)} for infinite seconds.")
    }

  /** eternally stores already encoded values into the storage */
  private def mSetEternallyIfNotExist(keyValues: (String, String)*): Future[Boolean] =
    redis.msetnx(keyValues.toMap) executing "MSETNX" withKeys keyValues.map(_._1) asCommand keyValues.map(_.asString).mkString(" ") logging {
      case true  => log.debug(s"Set if not exists on keys ${keyValues.map(_.key) mkString " "} succeeded.")
      case false => log.debug(s"Set if not exists on keys ${keyValues.map(_.key) mkString " "} ignored. Some value already exists.")
    }

  override def expire(key: String, expiration: Duration): Future[Unit] =
    redis.expire(key, expiration.toSeconds) executing "EXPIRE" withKey key andParameter s"$expiration" logging {
      case true  => log.debug(s"Expiration set on key '$key'.") // expiration was set
      case false => log.debug(s"Expiration set on key '$key' failed. Key does not exist.") // Nothing was removed
    }

  override def expiresIn(key: String): Future[Option[Duration]] =
    redis.pttl(key) executing "PTTL" withKey key expects {
      case -2 =>
        log.debug(s"PTTL on key '$key' returns -2, it does not exist.")
        None
      case -1 =>
        log.debug(s"PTTL on key '$key' returns -1, it has no associated expiration.")
        Some(Duration.Inf)
      case expiration =>
        log.debug(s"PTTL on key '$key' returns $expiration milliseconds.")
        Some(Duration(expiration, TimeUnit.MILLISECONDS))
    }

  override def matching(pattern: String): Future[Seq[String]] =
    redis.keys(pattern) executing "KEYS" withKey pattern logging {
      case keys => log.debug(s"KEYS on '$pattern' responded '${keys.mkString(", ")}'.")
    }

  // coverage is disabled as testing it would require
  // either a mock or would clear a redis while
  // the tests are in progress
  // $COVERAGE-OFF$
  override def invalidate(): Future[Unit] =
    redis.flushdb() executing "FLUSHDB" logging {
      case _ => log.info("Invalidated.") // cache was invalidated
    }
  // $COVERAGE-ON$

  override def exists(key: String): Future[Boolean] =
    redis.exists(key) executing "EXISTS" withKey key logging {
      case true  => log.debug(s"Key '$key' exists.")
      case false => log.debug(s"Key '$key' doesn't exist.")
    }

  override def remove(keys: String*): Future[Unit] =
    if (keys.nonEmpty) { // if any key to remove do it
      redis.del(keys: _*) executing "DEL" withKeys keys logging {
        // Nothing was removed
        case 0L      => log.debug(s"Remove on keys ${keys.mkString("'", ",", "'")} succeeded but nothing was removed.")
        // Some entries were removed
        case removed => log.debug(s"Remove on keys ${keys.mkString("'", ",", "'")} removed $removed values.")
      }
    } else {
      Future.successful(()) // otherwise return immediately
    }

  override def ping(): Future[Unit] =
    redis.ping() executing "PING" logging {
      case "PONG" => ()
    }

  override def increment(key: String, by: Long): Future[Long] =
    redis.incrby(key, by) executing "INCRBY" withKey key andParameter s"$by" logging {
      case value => log.debug(s"The value at key '$key' was incremented by $by to $value.")
    }

  override def append(key: String, value: String): Future[Long] =
    redis.append(key, value) executing "APPEND" withKey key andParameter value logging {
      case _ => log.debug(s"The value was appended to key '$key'.")
    }

  override def listPrepend(key: String, values: Any*): Future[Long] =
    Future.sequence(values.map(encode(key, _))).flatMap(redis.lpush(key, _: _*)) executing "LPUSH" withKey key andParameters values logging {
      case length => log.debug(s"The $length values was prepended to key '$key'.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list to be prepended.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }

  override def listAppend(key: String, values: Any*): Future[Long] =
    Future.sequence(values.map(encode(key, _))).flatMap(redis.rpush(key, _: _*)) executing "RPUSH" withKey key andParameters values logging {
      case length => log.debug(s"The $length values was appended to key '$key'.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list to be appended.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }

  override def listSize(key: String): Future[Long] =
    redis.llen(key) executing "LLEN" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  override def listSetAt(key: String, position: Long, value: Any): Future[Unit] =
    encode(key, value).flatMap(redis.lset(key, position, _)) executing "LSET" withKey key andParameter value logging {
      case _ => log.debug(s"Updated value at $position in '$key' to $value.")
    } map (_ => ()) recover {
      case ExecutionFailedException(_, _, _, actors.ReplyErrorException("ERR index out of range")) =>
        log.debug(s"Update of the value at $position in '$key' failed due to index out of range.")
        throw new IndexOutOfBoundsException("Index out of range")
    }

  override def listHeadPop[T: ClassTag](key: String): Future[Option[T]] =
    redis.lpop[String](key) executing "LPOP" withKey key expects {
      case Some(encoded) =>
        log.trace(s"Hit on head in key '$key'.")
        Some(decode[T](key, encoded))
      case None =>
        log.trace(s"Miss on head in key '$key'.")
        None
    }

  override def listSlice[T: ClassTag](key: String, start: Long, end: Long): Future[Seq[T]] =
    redis.lrange[String](key, start, end) executing "LRANGE" withKey key andParameters s"$start $end" expects {
      case values =>
        log.debug(s"The range on '$key' from $start to $end included returned ${values.size} values.")
        values.map(decode[T](key, _))
    }

  override def listRemove(key: String, value: Any, count: Long): Future[Long] =
    encode(key, value).flatMap(redis.lrem(key, count, _)) executing "LREM" withKey key andParameters s"$value $count" logging {
      case removed => log.debug(s"Removed $removed occurrences of $value in '$key'.")
    }

  override def listTrim(key: String, start: Long, end: Long): Future[Unit] =
    redis.ltrim(key, start, end) executing "LTRIM" withKey key andParameter s"$start $end" logging {
      case _ => log.debug(s"Trimmed collection at '$key' to $start:$end ")
    }

  override def listInsert(key: String, pivot: Any, value: Any): Future[Option[Long]] = for {
    pivot <- encode(key, pivot)
    value <- encode(key, value)
    result <- redis.linsert(key, api.BEFORE, pivot, value) executing "LINSERT" withKey key andParameter s"$pivot $value" expects {
      case -1L | 0L =>
        log.debug(s"Insert into the list at '$key' failed. Pivot not found.")
        None
      case length =>
        log.debug(s"Inserted $value into the list at '$key'. New size is $length.")
        Some(length)
    } recover[Option[Long]] {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }
  } yield result

  override def setAdd(key: String, values: Any*): Future[Long] = {
    // encodes the value
    def toEncoded(value: Any) = encode(key, value)
    Future.sequence(values map toEncoded).flatMap(redis.sadd(key, _: _*)) executing "SADD" withKey key andParameters values expects {
      case inserted =>
        log.debug(s"Inserted $inserted elements into the set at '$key'.")
        inserted
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a set.")
        throw new IllegalArgumentException(s"Value at '$key' is not a set.")
    }
  }

  override def setSize(key: String): Future[Long] =
    redis.scard(key) executing "SCARD" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  override def setMembers[T: ClassTag](key: String): Future[Set[T]] =
    redis.smembers[String](key) executing "SMEMBERS" withKey key expects {
      case items =>
        log.debug(s"Returned ${items.size} items from the collection at '$key'.")
        items.map(decode[T](key, _)).toSet
    }

  override def setIsMember(key: String, value: Any): Future[Boolean] =
    encode(key, value).flatMap(redis.sismember(key, _)) executing "SISMEMBER" withKey key andParameter value logging {
      case true  => log.debug(s"Item $value exists in the collection at '$key'.")
      case false => log.debug(s"Item $value does not exist in the collection at '$key'")
    }

  override def setRemove(key: String, values: Any*): Future[Long] = {
    // encodes the value
    def toEncoded(value: Any): Future[String] = encode(key, value)

    Future.sequence(values map toEncoded).flatMap(redis.srem(key, _: _*)) executing "SREM" withKey key andParameters values logging {
      case removed => log.debug(s"Removed $removed elements from the collection at '$key'.")
    }
  }

  override def sortedSetAdd(key: String, scoreValues: (Double, Any)*): Future[Long] = {
    // encodes the value
    def toEncoded(scoreValue: (Double, Any)) = encode(key, scoreValue._2).map((scoreValue._1, _))

    Future.sequence(scoreValues.map(toEncoded)).flatMap(redis.zadd(key, _: _*)) executing "ZADD" withKey key andParameters scoreValues expects {
      case inserted =>
        log.debug(s"Inserted $inserted elements into the zset at '$key'.")
        inserted
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a zset.")
        throw new IllegalArgumentException(s"Value at '$key' is not a zset.")
    }
  }

  override def sortedSetSize(key: String): Future[Long] =
    redis.zcard(key) executing "ZCARD" withKey key logging {
      case length => log.debug(s"The zset at '$key' has $length items.")
    }

  override def sortedSetScore(key: String, value: Any): Future[Option[Double]] = {
    encode(key, value) flatMap (redis.zscore(key, _)) executing "ZSCORE" withKey key andParameter value logging {
      case Some(score) => log.debug(s"The score of item: $value is $score in the collection at '$key'.")
      case None        => log.debug(s"Item $value does not exist in the collection at '$key'")
    }
  }

  override def sortedSetRemove(key: String, values: Any*): Future[Long] = {
    // encodes the value
    def toEncoded(value: Any) = encode(key, value)

    Future.sequence(values map toEncoded).flatMap(redis.zrem(key, _: _*)) executing "ZREM" withKey key andParameters values logging {
      case removed => log.debug(s"Removed $removed elements from the zset at '$key'.")
    }
  }

  override def sortedSetRange[T: ClassTag](key: String, start: Long, stop: Long): Future[Seq[T]] = {
    redis.zrange[String](key, start, stop) executing "ZRANGE" withKey key andParameter s"$start $stop" expects {
      case encodedSeq =>
        log.debug(s"Got range from $start to $stop in the zset at '$key'.")
        encodedSeq.map(encoded => decode[T](key, encoded))
    }
  }

  override def sortedSetReverseRange[T: ClassTag](key: String, start: Long, stop: Long): Future[Seq[T]] = {
    redis.zrevrange[String](key, start, stop) executing "ZREVRANGE" withKey key andParameter s"$start $stop" expects {
      case encodedSeq =>
        log.debug(s"Got reverse range from $start to $stop in the zset at '$key'.")
        encodedSeq.map(encoded => decode[T](key, encoded))
    }
  }

  override def hashRemove(key: String, fields: String*): Future[Long] =
    redis.hdel(key, fields: _*) executing "HDEL" withKey key andParameters fields logging {
      case removed => log.debug(s"Removed $removed elements from the collection at '$key'.")
    }

  override def hashIncrement(key: String, field: String, incrementBy: Long): Future[Long] =
    redis.hincrby(key, field, incrementBy) executing "HINCRBY" withKey key andParameters s"$field $incrementBy" logging {
      case value => log.debug(s"Field '$field' in '$key' was incremented to $value.")
    }

  override def hashExists(key: String, field: String): Future[Boolean] =
    redis.hexists(key, field) executing "HEXISTS" withKey key andParameter field logging {
      case true  => log.debug(s"Item $field exists in the collection at '$key'.")
      case false => log.debug(s"Item $field does not exist in the collection at '$key'")
    }

  override def hashGet[T: ClassTag](key: String, field: String): Future[Option[T]] =
    redis.hget[String](key, field) executing "HGET" withKey key andParameter field expects {
      case Some(encoded) =>
        log.debug(s"Item $field exists in the collection at '$key'.")
        Some(decode[T](key, encoded))
      case None =>
        log.debug(s"Item $field is not in the collection at '$key'.")
        None
    }

  override def hashGet[T: ClassTag](key: String, fields: Seq[String]): Future[Seq[Option[T]]] =
    redis.hmget[String](key, fields: _*) executing "HMGET" withKey key andParameters fields expects {
      case encoded =>
        log.debug(s"Collection at '$key' with fields '$fields' has returned ${encoded.size} items.")
        encoded.map(_.map(decode[T](key, _)))
    }

  override def hashGetAll[T: ClassTag](key: String): Future[Map[String, T]] =
    redis.hgetall[String](key) executing "HGETALL" withKey key expects {
      case empty if empty.isEmpty =>
        log.debug(s"Collection at '$key' is empty.")
        Map.empty[String, T]
      case encoded =>
        log.debug(s"Collection at '$key' has ${encoded.size} items.")
        encoded.map { case (itemKey, value) => itemKey -> decode[T](itemKey, value) }
    }

  override def hashSize(key: String): Future[Long] =
    redis.hlen(key) executing "HLEN" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  override def hashKeys(key: String): Future[Set[String]] =
    redis.hkeys(key) executing "HKEYS" withKey key expects {
      case keys =>
        log.debug(s"The collection at '$key' defines: ${keys mkString " "}.")
        keys.toSet
    }

  override def hashSet(key: String, field: String, value: Any): Future[Boolean] =
    encode(key, value).flatMap(redis.hset(key, field, _)) executing "HSET" withKey key andParameters s"$field $value" logging {
      case true  => log.debug(s"Item $field in the collection at '$key' was inserted.")
      case false => log.debug(s"Item $field in the collection at '$key' was updated.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a map.")
        throw new IllegalArgumentException(s"Value at '$key' is not a map.")
    }

  override def hashValues[T: ClassTag](key: String): Future[Set[T]] =
    redis.hvals[String](key) executing "HVALS" withKey key expects {
      case values =>
        log.debug(s"The collection at '$key' contains ${values.size} values.")
        values.map(decode[T](key, _)).toSet
    }

  // $COVERAGE-OFF$
  override def toString: String = s"RedisConnector(name=$name)"
  // $COVERAGE-ON$
}
