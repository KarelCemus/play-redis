package play.api.cache.redis.connector

import io.lettuce.core.SetArgs
import io.lettuce.core.api.async.RedisAsyncCommands
import play.api.Logger
import play.api.cache.redis._

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.{IterableHasAsScala, MapHasAsJava, MapHasAsScala}
import scala.jdk.FutureConverters._
import scala.reflect.ClassTag

/**
  * The connector directly connects with the REDIS instance, implements protocol commands
  * and is supposed to by used internally by another wrappers. The connector does not
  * directly implement [[play.api.cache.redis.CacheApi]] but provides fundamental functionality.
  *
  * @param serializer encodes/decodes objects into/from a string
  * @param redis      implementation of the commands
  */
private[connector] class RedisConnectorImpl(serializer: AkkaSerializer, redis: RedisAsyncCommands[String, String])(implicit runtime: RedisRuntime) extends RedisConnector {

  import ExpectedFuture._
  import runtime._

  /** logger instance */
  protected val log: Logger = Logger("play.api.cache.redis")

  def get[T: ClassTag](key: String): Future[Option[T]] = redis.get(key).asScala executing "GET" withKey key expects {
    response: String =>
      log.trace(s"Hit on key '$key'.")
      Some(decode[T](key, response))
  } recover {
    case _: Exception =>
      None
  }

  def mGet[T: ClassTag](keys: String*): Future[Seq[Option[T]]] =
    redis.mget(keys: _*).asScala executing "MGET" withKeys keys expects {
      // list is always returned
      case list =>
        list.asScala.map {
          l =>
            log.trace(s"Hit on key '${l.getKey}'.")
            Some(decode[T](l.getKey, l.getValue))
        }.toSeq
    } recover {
      case _: Exception =>
        Nil
    }

  /** decodes the object, reports an exception if fails */
  private def decode[T: ClassTag](key: String, encoded: String): T =
    serializer.decode[T](encoded).recover {
      case ex => serializationFailed(key, "Deserialization failed", ex)
    }.get

  def set(key: String, value: Any, expiration: Duration, ifNotExists: Boolean): Future[Boolean] =
    // no value to set
    if (value == null) remove(key).map(_ => true)
    // set the value
    else encode(key, value) flatMap (doSet(key, _, expiration, ifNotExists))

  /** encodes the object, reports an exception if fails */
  private def encode(key: String, value: Any): Future[String] = Future.fromTry {
    serializer.encode(value).recover {
      case ex => serializationFailed(key, "Serialization failed", ex)
    }
  }

  /** implements the advanced set operation storing already encoded value into the storage */
  private def doSet(key: String, value: String, expiration: Duration, ifNotExists: Boolean): Future[Boolean] = {
    val args = if (expiration.isFinite) SetArgs.Builder.ex(expiration.toMillis).nx() else SetArgs.Builder.nx()
    redis.set(
      key,
      value,
      args
    ).asScala.map {
      case "OK" => true
      case _    => false
    } executing "SET" withKey key andParameters s"$value${s" PX $expiration" when expiration.isFinite}${" NX" when ifNotExists}" logging {
      case true if expiration.isFinite => log.debug(s"Set on key '$key' for ${expiration.toMillis} milliseconds.")
      case true                        => log.debug(s"Set on key '$key' for infinite seconds.")
      case false                       => log.debug(s"Set on key '$key' ignored. Condition was not met.")
    }
  }

  def mSet(keyValues: (String, Any)*): Future[Unit] = mSetUsing(mSetEternally, (), keyValues: _*)

  def mSetIfNotExist(keyValues: (String, Any)*): Future[Boolean] = mSetUsing(mSetEternallyIfNotExist, true, keyValues: _*)

  /** eternally stores or removes all given values, using the given mSet implementation */
  def mSetUsing[T](mSet: Seq[(String, String)] => Future[T], default: T, keyValues: (String, Any)*): Future[T] = {
    val (toBeRemoved, toBeSet) = keyValues.partition(_.isNull)
    // remove all keys to be removed
    val toBeRemovedFuture = if (toBeRemoved.isEmpty) Future.successful(()) else remove(toBeRemoved.map(_.key): _*)
    // set all keys to be set
    val toBeSetFuture = if (toBeSet.isEmpty) Future.successful(default) else Future sequence toBeSet.map(tuple => encode(tuple.key, tuple.value).map(tuple.key -> _)) flatMap mSet
    // combine futures ignoring the result of removal
    toBeRemovedFuture.flatMap(_ => toBeSetFuture)
  }

  /** eternally stores already encoded values into the storage */
  def mSetEternally(keyValues: (String, String)*): Future[Unit] =
    redis.mset(keyValues.toMap.asJava).asScala executing "MSET" withKeys keyValues.map(_._1) asCommand keyValues.map(_.asString).mkString(" ") logging {
      case _ =>
        log.debug(s"Set on keys ${keyValues.map(_.key)} for infinite seconds.")
    }

  /** eternally stores already encoded values into the storage */
  def mSetEternallyIfNotExist(keyValues: (String, String)*): Future[Boolean] =
    redis.msetnx(keyValues.toMap.asJava).asScala.map(_.booleanValue()) executing "MSETNX" withKeys keyValues.map(_._1) asCommand keyValues.map(_.asString).mkString(" ") logging {
      case true  => log.debug(s"Set if not exists on keys ${keyValues.map(_.key) mkString " "} succeeded.")
      case false => log.debug(s"Set if not exists on keys ${keyValues.map(_.key) mkString " "} ignored. Some value already exists.")
    }

  def expire(key: String, expiration: Duration): Future[Unit] =
    redis.expire(key, expiration.toSeconds.toInt).asScala.map(_.booleanValue()) executing "EXPIRE" withKey key andParameter s"$expiration" logging {
      case true  => log.debug(s"Expiration set on key '$key'.") // expiration was set
      case false => log.debug(s"Expiration set on key '$key' failed. Key does not exist.") // Nothing was removed
    }

  def expiresIn(key: String): Future[Option[Duration]] =
    redis.pttl(key).asScala.map(_.longValue()) executing "PTTL" withKey key expects {
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

  def matching(pattern: String): Future[Seq[String]] =
    redis.keys(pattern).asScala.map(_.asScala.toSeq) executing "KEYS" withKey pattern logging {
      case keys => log.debug(s"KEYS on '$pattern' responded '${keys.mkString(", ")}'.")
    }

  // coverage is disabled as testing it would require
  // either a mock or would clear a redis while
  // the tests are in progress
  // $COVERAGE-OFF$
  def invalidate(): Future[Unit] =
    redis.flushdb().asScala executing "FLUSHDB" logging {
      case _ => log.info("Invalidated.") // cache was invalidated
    }
  // $COVERAGE-ON$

  def exists(key: String): Future[Boolean] =
    redis.exists(key).asScala.map(_.longValue()).map {
      case 1L => true
      case 0L => false
    } executing "EXISTS" withKey key logging {
      case true  => log.debug(s"Key '$key' exists.")
      case false => log.debug(s"Key '$key' doesn't exist.")
    }

  def remove(keys: String*): Future[Unit] =
    if (keys.nonEmpty) { // if any key to remove do it
      redis.del(keys: _*).asScala.map(_.longValue()) executing "DEL" withKeys keys logging {
        // Nothing was removed
        case 0L      => log.debug(s"Remove on keys ${keys.mkString("'", ",", "'")} succeeded but nothing was removed.")
        // Some entries were removed
        case removed => log.debug(s"Remove on keys ${keys.mkString("'", ",", "'")} removed $removed values.")
      }
    } else {
      Future.successful(()) // otherwise return immediately
    }

  def ping(): Future[Unit] =
    redis.ping().asScala executing "PING" logging {
      case "PONG" => ()
    }

  def increment(key: String, by: Long): Future[Long] =
    redis.incrby(key, by).asScala.map(_.toLong) executing "INCRBY" withKey key andParameter s"$by" logging {
      case value => log.debug(s"The value at key '$key' was incremented by $by to $value.")
    }

  def append(key: String, value: String): Future[Long] =
    redis.append(key, value).asScala.map(_.toLong) executing "APPEND" withKey key andParameter value logging {
      case length => log.debug(s"The value was appended to key '$key'.")
    }

  def listPrepend(key: String, values: Any*): Future[Long] =
    Future.sequence(values.map(encode(key, _))).flatMap(redis.lpush(key, _: _*).asScala.map(_.toLong)) executing "LPUSH" withKey key andParameters values logging {
      case length => log.debug(s"The $length values was prepended to key '$key'.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }

  def listAppend(key: String, values: Any*) =
    Future.sequence(values.map(encode(key, _))).flatMap(redis.rpush(key, _: _*).asScala.map(_.toLong)) executing "RPUSH" withKey key andParameters values logging {
      case length => log.debug(s"The $length values was appended to key '$key'.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }

  def listSize(key: String) =
    redis.llen(key).asScala.map(_.toLong) executing "LLEN" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  def listSetAt(key: String, position: Int, value: Any) =
    encode(key, value).flatMap(redis.lset(key, position, _).asScala) executing "LSET" withKey key andParameter value logging {
      case _ => log.debug(s"Updated value at $position in '$key' to $value.")
    } recover {
      case ExecutionFailedException(_, _, _, _) =>
        log.debug(s"Update of the value at $position in '$key' failed due to index out of range.")
        throw new IndexOutOfBoundsException("Index out of range")
    }

  def listHeadPop[T: ClassTag](key: String) =
    redis.lpop(key).asScala executing "LPOP" withKey key expects {
      encoded =>
        log.trace(s"Hit on head in key '$key'.")
        Some(decode[T](key, encoded))
    }

  def listSlice[T: ClassTag](key: String, start: Int, end: Int) =
    redis.lrange(key, start, end).asScala.map(_.asScala.toSeq) executing "LRANGE" withKey key andParameters s"$start $end" expects {
      case values =>
        log.debug(s"The range on '$key' from $start to $end included returned ${values.size} values.")
        values.map(decode[T](key, _))
    }

  def listRemove(key: String, value: Any, count: Int) =
    encode(key, value).flatMap(redis.lrem(key, count, _).asScala.map(_.toLong)) executing "LREM" withKey key andParameters s"$value $count" logging {
      case removed => log.debug(s"Removed $removed occurrences of $value in '$key'.")
    }

  def listTrim(key: String, start: Int, end: Int) =
    redis.ltrim(key, start, end).asScala executing "LTRIM" withKey key andParameter s"$start $end" logging {
      case _ => log.debug(s"Trimmed collection at '$key' to $start:$end ")
    }

  def listInsert(key: String, pivot: Any, value: Any) = for {
    pivot <- encode(key, pivot)
    value <- encode(key, value)
    result <- redis.linsert(key, true, pivot, value).asScala.map(_.longValue()) executing "LINSERT" withKey key andParameter s"$pivot $value" expects {
      case -1L | 0L =>
        log.debug(s"Insert into the list at '$key' failed. Pivot not found.")
        None
      case length =>
        log.debug(s"Inserted $value into the list at '$key'. New size is $length.")
        Some(length)
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }
  } yield result

  def setAdd(key: String, values: Any*) = {
    // encodes the value
    def toEncoded(value: Any) = encode(key, value)

    Future.sequence(values map toEncoded).flatMap(redis.sadd(key, _: _*).asScala.map(_.longValue())) executing "SADD" withKey key andParameters values expects {
      case inserted =>
        log.debug(s"Inserted $inserted elements into the set at '$key'.")
        inserted
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a set.")
        throw new IllegalArgumentException(s"Value at '$key' is not a set.")
    }
  }

  def setSize(key: String) =
    redis.scard(key).asScala.map(_.longValue()) executing "SCARD" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  def setMembers[T: ClassTag](key: String) =
    redis.smembers(key).asScala.map(_.asScala) executing "SMEMBERS" withKey key expects {
      case items =>
        log.debug(s"Returned ${items.size} items from the collection at '$key'.")
        items.map(decode[T](key, _)).toSet
    }

  def setIsMember(key: String, value: Any) =
    encode(key, value).flatMap(redis.sismember(key, _).asScala.map(_.booleanValue())) executing "SISMEMBER" withKey key andParameter value logging {
      case true  => log.debug(s"Item $value exists in the collection at '$key'.")
      case false => log.debug(s"Item $value does not exist in the collection at '$key'")
    }

  def setRemove(key: String, values: Any*) = {
    // encodes the value
    def toEncoded(value: Any) = encode(key, value)

    Future.sequence(values map toEncoded).flatMap(redis.srem(key, _: _*).asScala.map(_.longValue())) executing "SREM" withKey key andParameters values logging {
      case removed => log.debug(s"Removed $removed elements from the collection at '$key'.")
    }
  }

  def sortedSetAdd(key: String, scoreValues: (Double, Any)*) = {
    // encodes the value
    def toEncoded(scoreValue: (Double, Any)) = encode(key, scoreValue._2).map((scoreValue._1, _))

    Future.sequence(scoreValues.map(toEncoded)).flatMap(redis.zadd(key, _: _*).asScala.map(_.longValue())) executing "ZADD" withKey key andParameters scoreValues expects {
      case inserted =>
        log.debug(s"Inserted $inserted elements into the zset at '$key'.")
        inserted
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a zset.")
        throw new IllegalArgumentException(s"Value at '$key' is not a zset.")
    }
  }

  def sortedSetSize(key: String) =
    redis.zcard(key).asScala.map(_.longValue()) executing "ZCARD" withKey key logging {
      case length => log.debug(s"The zset at '$key' has $length items.")
    }

  def sortedSetScore(key: String, value: Any) = {
    encode(key, value) flatMap (redis.zscore(key, _).asScala.map(i => Some(i.doubleValue()))) executing "ZSCORE" withKey key andParameter value logging {
      score => log.debug(s"The score of item: $value is $score in the collection at '$key'.")
    }
  }

  def sortedSetRemove(key: String, values: Any*) = {
    // encodes the value
    def toEncoded(value: Any) = encode(key, value)

    Future.sequence(values map toEncoded).flatMap(redis.zrem(key, _: _*).asScala.map(_.longValue())) executing "ZREM" withKey key andParameters values logging {
      case removed => log.debug(s"Removed $removed elements from the zset at '$key'.")
    }
  }

  def sortedSetRange[T: ClassTag](key: String, start: Long, stop: Long) = {
    redis.zrange(key, start, stop).asScala.map(_.asScala) executing "ZRANGE" withKey key andParameter s"$start $stop" expects {
      case encodedSeq =>
        log.debug(s"Got range from $start to $stop in the zset at '$key'.")
        encodedSeq.map(encoded => decode[T](key, encoded)).toSeq
    }
  }

  def sortedSetReverseRange[T: ClassTag](key: String, start: Long, stop: Long) = {
    redis.zrevrange(key, start, stop).asScala.map(_.asScala) executing "ZREVRANGE" withKey key andParameter s"$start $stop" expects {
      case encodedSeq =>
        log.debug(s"Got reverse range from $start to $stop in the zset at '$key'.")
        encodedSeq.map(encoded => decode[T](key, encoded)).toSeq
    }
  }

  def hashRemove(key: String, fields: String*) =
    redis.hdel(key, fields: _*).asScala.map(_.longValue()) executing "HDEL" withKey key andParameters fields logging {
      case removed => log.debug(s"Removed $removed elements from the collection at '$key'.")
    }

  def hashIncrement(key: String, field: String, incrementBy: Long) =
    redis.hincrby(key, field, incrementBy).asScala.map(_.longValue()) executing "HINCRBY" withKey key andParameters s"$field $incrementBy" logging {
      case value => log.debug(s"Field '$field' in '$key' was incremented to $value.")
    }

  def hashExists(key: String, field: String) =
    redis.hexists(key, field).asScala.map(_.booleanValue()) executing "HEXISTS" withKey key andParameter field logging {
      case true  => log.debug(s"Item $field exists in the collection at '$key'.")
      case false => log.debug(s"Item $field does not exist in the collection at '$key'")
    }

  def hashGet[T: ClassTag](key: String, field: String) =
    redis.hget(key, field).asScala executing "HGET" withKey key andParameter field expects {
      encoded =>
        log.debug(s"Item $field exists in the collection at '$key'.")
        Some(decode[T](key, encoded))
    }

  def hashGet[T: ClassTag](key: String, fields: Seq[String]): Future[Seq[Option[T]]] =
    redis.hmget(key, fields: _*).asScala.map(_.asScala.toSeq) executing "HMGET" withKey key andParameters fields expects {
      case encoded =>
        log.debug(s"Collection at '$key' with fields '$fields' has returned ${encoded.size} items.")
        encoded.map(i => Some(decode[T](key, i.getValue)))
    }

  def hashGetAll[T: ClassTag](key: String) =
    redis.hgetall(key).asScala executing "HGETALL" withKey key expects {
      case empty if empty.isEmpty =>
        log.debug(s"Collection at '$key' is empty.")
        Map.empty
      case encoded =>
        log.debug(s"Collection at '$key' has ${encoded.size} items.")
        encoded.asScala.map { itemKey => itemKey._1 -> decode[T](itemKey._1, itemKey._2) }.toMap
    }

  def hashSize(key: String) =
    redis.hlen(key).asScala.map(_.longValue()) executing "HLEN" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  def hashKeys(key: String) =
    redis.hkeys(key).asScala.map(_.asScala) executing "HKEYS" withKey key expects {
      case keys =>
        log.debug(s"The collection at '$key' defines: ${keys mkString " "}.")
        keys.toSet
    }

  def hashSet(key: String, field: String, value: Any) =
    encode(key, value).flatMap(redis.hset(key, field, _).asScala).map(_.booleanValue) executing "HSET" withKey key andParameters s"$field $value" logging {
      case true  => log.debug(s"Item $field in the collection at '$key' was inserted.")
      case false => log.debug(s"Item $field in the collection at '$key' was updated.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a map.")
        throw new IllegalArgumentException(s"Value at '$key' is not a map.")
    }

  def hashValues[T: ClassTag](key: String) =
    redis.hvals(key).asScala.map(_.asScala) executing "HVALS" withKey key expects {
      case values =>
        log.debug(s"The collection at '$key' contains ${values.size} values.")
        values.map(decode[T](key, _)).toSet
    }

  // $COVERAGE-OFF$
  override def toString = s"RedisConnector(name=$name)"
  // $COVERAGE-ON$
}
