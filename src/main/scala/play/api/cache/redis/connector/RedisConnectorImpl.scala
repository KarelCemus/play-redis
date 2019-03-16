package play.api.cache.redis.connector

import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.Logger
import play.api.cache.redis._

import redis._

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
  protected val log = Logger("play.api.cache.redis")

  def get[T: ClassTag](key: String): Future[Option[T]] =
    redis.get[String](key) executing "GET" withKey key expects {
      case Some(response: String) =>
        log.trace(s"Hit on key '$key'.")
        Some(decode[T](key, response))
      case None =>
        log.debug(s"Miss on key '$key'.")
        None
    }

  def mGet[T: ClassTag](keys: String*): Future[Seq[Option[T]]] =
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
  private def doSet(key: String, value: String, expiration: Duration, ifNotExists: Boolean): Future[Boolean] =
    redis.set(
      key,
      value,
      exSeconds = if (expiration.isFinite) Some(expiration.toSeconds) else None,
      NX = ifNotExists
    ) executing "SET" withKey key andParameters s"$value${s" EX $expiration" when expiration.isFinite}${" NX" when ifNotExists}" logging {
      case true if expiration.isFinite => log.debug(s"Set on key '$key' for ${expiration.toSeconds} seconds.")
      case true                        => log.debug(s"Set on key '$key' for infinite seconds.")
      case false                       => log.debug(s"Set on key '$key' ignored. Condition was not met.")
    }

  def mSet(keyValues: (String, Any)*): Future[Unit] = mSetUsing(mSetEternally, (), keyValues: _*)

  def mSetIfNotExist(keyValues: (String, Any)*): Future[Boolean] = mSetUsing(mSetEternallyIfNotExist, true, keyValues: _*)

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

  def expire(key: String, expiration: Duration): Future[Unit] =
    redis.expire(key, expiration.toSeconds.toInt) executing "EXPIRE" withKey key andParameter s"$expiration" logging {
      case true  => log.debug(s"Expiration set on key '$key'.") // expiration was set
      case false => log.debug(s"Expiration set on key '$key' failed. Key does not exist.") // Nothing was removed
    }

  def expiresIn(key: String): Future[Option[Duration]] =
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

  def matching(pattern: String): Future[Seq[String]] =
    redis.keys(pattern) executing "KEYS" withKey pattern logging {
      case keys => log.debug(s"KEYS on '$pattern' responded '${keys.mkString(", ")}'.")
    }

  // coverage is disabled as testing it would require
  // either a mock or would clear a redis while
  // the tests are in progress
  // $COVERAGE-OFF$
  def invalidate(): Future[Unit] =
    redis.flushdb() executing "FLUSHDB" logging {
      case _ => log.info("Invalidated.") // cache was invalidated
    }
  // $COVERAGE-ON$

  def exists(key: String): Future[Boolean] =
    redis.exists(key) executing "EXISTS" withKey key logging {
      case true  => log.debug(s"Key '$key' exists.")
      case false => log.debug(s"Key '$key' doesn't exist.")
    }

  def remove(keys: String*): Future[Unit] =
    if (keys.nonEmpty) { // if any key to remove do it
      redis.del(keys: _*) executing "DEL" withKeys keys logging {
        // Nothing was removed
        case 0L      => log.debug(s"Remove on keys ${keys.mkString("'", ",", "'")} succeeded but nothing was removed.")
        // Some entries were removed
        case removed => log.debug(s"Remove on keys ${keys.mkString("'", ",", "'")} removed $removed values.")
      }
    } else {
      Future.successful(Unit) // otherwise return immediately
    }

  def ping(): Future[Unit] =
    redis.ping() executing "PING" logging {
      case "PONG" => Unit
    }

  def increment(key: String, by: Long): Future[Long] =
    redis.incrby(key, by) executing "INCRBY" withKey key andParameter s"$by" logging {
      case value => log.debug(s"The value at key '$key' was incremented by $by to $value.")
    }

  def append(key: String, value: String): Future[Long] =
    redis.append(key, value) executing "APPEND" withKey key andParameter value logging {
      case length => log.debug(s"The value was appended to key '$key'.")
    }

  def listPrepend(key: String, values: Any*): Future[Long] =
    Future.sequence(values.map(encode(key, _))).flatMap(redis.lpush(key, _: _*)) executing "LPUSH" withKey key andParameters values logging {
      case length => log.debug(s"The $length values was prepended to key '$key'.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }

  def listAppend(key: String, values: Any*) =
    Future.sequence(values.map(encode(key, _))).flatMap(redis.rpush(key, _: _*)) executing "RPUSH" withKey key andParameters values logging {
      case length => log.debug(s"The $length values was appended to key '$key'.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a list.")
        throw new IllegalArgumentException(s"Value at '$key' is not a list.")
    }

  def listSize(key: String) =
    redis.llen(key) executing "LLEN" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  def listSetAt(key: String, position: Int, value: Any) =
    encode(key, value).flatMap(redis.lset(key, position, _)) executing "LSET" withKey key andParameter value logging {
      case _ => log.debug(s"Updated value at $position in '$key' to $value.")
    } recover {
      case ExecutionFailedException(_, _, _, actors.ReplyErrorException("ERR index out of range")) =>
        log.debug(s"Update of the value at $position in '$key' failed due to index out of range.")
        throw new IndexOutOfBoundsException("Index out of range")
    }

  def listHeadPop[T: ClassTag](key: String) =
    redis.lpop[String](key) executing "LPOP" withKey key expects {
      case Some(encoded) =>
        log.trace(s"Hit on head in key '$key'.")
        Some(decode[T](key, encoded))
      case None =>
        log.trace(s"Miss on head in key '$key'.")
        None
    }

  def listSlice[T: ClassTag](key: String, start: Int, end: Int) =
    redis.lrange[String](key, start, end) executing "LRANGE" withKey key andParameters s"$start $end" expects {
      case values =>
        log.debug(s"The range on '$key' from $start to $end included returned ${values.size} values.")
        values.map(decode[T](key, _))
    }

  def listRemove(key: String, value: Any, count: Int) =
    encode(key, value).flatMap(redis.lrem(key, count, _)) executing "LREM" withKey key andParameters s"$value $count" logging {
      case removed => log.debug(s"Removed $removed occurrences of $value in '$key'.")
    }

  def listTrim(key: String, start: Int, end: Int) =
    redis.ltrim(key, start, end) executing "LTRIM" withKey key andParameter s"$start $end" logging {
      case _ => log.debug(s"Trimmed collection at '$key' to $start:$end ")
    }

  def listInsert(key: String, pivot: Any, value: Any) = for {
    pivot <- encode(key, pivot)
    value <- encode(key, value)
    result <- redis.linsert(key, api.BEFORE, pivot, value) executing "LINSERT" withKey key andParameter s"$pivot $value" expects {
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

  def setSize(key: String) =
    redis.scard(key) executing "SCARD" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  def setMembers[T: ClassTag](key: String) =
    redis.smembers[String](key) executing "SMEMBERS" withKey key expects {
      case items =>
        log.debug(s"Returned ${items.size} items from the collection at '$key'.")
        items.map(decode[T](key, _)).toSet
    }

  def setIsMember(key: String, value: Any) =
    encode(key, value).flatMap(redis.sismember(key, _)) executing "SISMEMBER" withKey key andParameter value logging {
      case true  => log.debug(s"Item $value exists in the collection at '$key'.")
      case false => log.debug(s"Item $value does not exist in the collection at '$key'")
    }

  def setRemove(key: String, values: Any*) = {
    // encodes the value
    def toEncoded(value: Any) = encode(key, value)

    Future.sequence(values map toEncoded).flatMap(redis.srem(key, _: _*)) executing "SREM" withKey key andParameters values logging {
      case removed => log.debug(s"Removed $removed elements from the collection at '$key'.")
    }
  }

  def hashRemove(key: String, fields: String*) =
    redis.hdel(key, fields: _*) executing "HDEL" withKey key andParameters fields logging {
      case removed => log.debug(s"Removed $removed elements from the collection at '$key'.")
    }

  def hashIncrement(key: String, field: String, incrementBy: Long) =
    redis.hincrby(key, field, incrementBy) executing "HINCRBY" withKey key andParameters s"$field $incrementBy" logging {
      case value => log.debug(s"Field '$field' in '$key' was incremented to $value.")
    }

  def hashExists(key: String, field: String) =
    redis.hexists(key, field) executing "HEXISTS" withKey key andParameter field logging {
      case true  => log.debug(s"Item $field exists in the collection at '$key'.")
      case false => log.debug(s"Item $field does not exist in the collection at '$key'")
    }

  def hashGet[T: ClassTag](key: String, field: String) =
    redis.hget[String](key, field) executing "HGET" withKey key andParameter field expects {
      case Some(encoded) =>
        log.debug(s"Item $field exists in the collection at '$key'.")
        Some(decode[T](key, encoded))
      case None =>
        log.debug(s"Item $field is not in the collection at '$key'.")
        None
    }

  def hashGetAll[T: ClassTag](key: String) =
    redis.hgetall[String](key) executing "HGETALL" withKey key expects {
      case empty if empty.isEmpty =>
        log.debug(s"Collection at '$key' is empty.")
        Map.empty
      case encoded =>
        log.debug(s"Collection at '$key' has ${encoded.size} items.")
        encoded.mapValues(decode[T](key, _))
    }

  def hashSize(key: String) =
    redis.hlen(key) executing "HLEN" withKey key logging {
      case length => log.debug(s"The collection at '$key' has $length items.")
    }

  def hashKeys(key: String) =
    redis.hkeys(key) executing "HKEYS" withKey key expects {
      case keys =>
        log.debug(s"The collection at '$key' defines: ${keys mkString " "}.")
        keys.toSet
    }

  def hashSet(key: String, field: String, value: Any) =
    encode(key, value).flatMap(redis.hset(key, field, _)) executing "HSET" withKey key andParameters s"$field $value" logging {
      case true  => log.debug(s"Item $field in the collection at '$key' was inserted.")
      case false => log.debug(s"Item $field in the collection at '$key' was updated.")
    } recover {
      case ExecutionFailedException(_, _, _, ex) if ex.getMessage startsWith "WRONGTYPE" =>
        log.warn(s"Value at '$key' is not a map.")
        throw new IllegalArgumentException(s"Value at '$key' is not a map.")
    }

  def hashValues[T: ClassTag](key: String) =
    redis.hvals[String](key) executing "HVALS" withKey key expects {
      case values =>
        log.debug(s"The collection at '$key' contains ${values.size} values.")
        values.map(decode[T](key, _)).toSet
    }

  // $COVERAGE-OFF$
  override def toString = s"RedisConnector(name=$name)"
  // $COVERAGE-ON$
}
