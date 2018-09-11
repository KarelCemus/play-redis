package play.api.cache.redis.impl

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of Set API using redis-server cache implementation.</p> */
private[impl] class RedisMapImpl[Elem: ClassTag, Result[_]](key: String, redis: RedisConnector)(implicit builder: Builders.ResultBuilder[Result], runtime: RedisRuntime) extends RedisMap[Elem, Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  def add(field: String, value: Elem) = redis.hashSet(key, field, value).map(_ => This).recoverWithDefault(This)

  def get(field: String) = redis.hashGet[Elem](key, field).recoverWithDefault(None)

  def contains(field: String) = redis.hashExists(key, field).recoverWithDefault(false)

  def remove(fields: String*) = redis.hashRemove(key, fields: _*).map(_ => This).recoverWithDefault(This)

  def increment(field: String, incrementBy: Long) = redis.hashIncrement(key, field, incrementBy).recoverWithDefault(incrementBy)

  def toMap = redis.hashGetAll[Elem](key).recoverWithDefault(Map.empty)

  def keySet = redis.hashKeys(key).recoverWithDefault(Set.empty)

  def values = redis.hashValues[Elem](key).recoverWithDefault(Set.empty)

  def size = redis.hashSize(key).recoverWithDefault(0)

  def isEmpty = redis.hashSize(key).map(_ == 0).recoverWithDefault(true)

  def nonEmpty = redis.hashSize(key).map(_ > 0).recoverWithDefault(false)
}
