package play.api.cache.redis.impl

import scala.language.implicitConversions
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of Set API using redis-server cache implementation.</p> */
private[impl] class RedisMapImpl[Elem: ClassTag, Result[_]](key: String, redis: RedisConnector)(implicit builder: Builders.ResultBuilder[Result], runtime: RedisRuntime) extends RedisMap[Elem, Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  override def add(field: String, value: Elem): Result[This] =
    redis.hashSet(key, field, value).map(_ => This).recoverWithDefault(This)

  override def get(field: String): Result[Option[Elem]] =
    redis.hashGet[Elem](key, field).recoverWithDefault(None)

  override def getFields(fields: Iterable[String]): Result[Seq[Option[Elem]]] =
    redis.hashGet[Elem](key, fields.toSeq).recoverWithDefault(Seq.fill(fields.size)(None))

  override def contains(field: String): Result[Boolean] =
    redis.hashExists(key, field).recoverWithDefault(false)

  override def remove(fields: String*): Result[This] =
    redis.hashRemove(key, fields: _*).map(_ => This).recoverWithDefault(This)

  override def increment(field: String, incrementBy: Long): Result[Long] =
    redis.hashIncrement(key, field, incrementBy).recoverWithDefault(incrementBy)

  override def toMap: Result[Map[String, Elem]] =
    redis.hashGetAll[Elem](key).recoverWithDefault(Map.empty)

  override def keySet: Result[Set[String]] =
    redis.hashKeys(key).recoverWithDefault(Set.empty)

  override def values: Result[Set[Elem]] =
    redis.hashValues[Elem](key).recoverWithDefault(Set.empty)

  override def size: Result[Long] =
    redis.hashSize(key).recoverWithDefault(0)

  override def isEmpty: Result[Boolean] =
    redis.hashSize(key).map(_ === 0).recoverWithDefault(true)

  override def nonEmpty: Result[Boolean] =
    redis.hashSize(key).map(_ > 0).recoverWithDefault(false)
}
