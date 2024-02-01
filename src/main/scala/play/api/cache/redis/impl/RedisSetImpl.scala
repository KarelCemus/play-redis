package play.api.cache.redis.impl

import play.api.cache.redis._

import scala.language.implicitConversions
import scala.reflect.ClassTag

/** <p>Implementation of Set API using redis-server cache implementation.</p> */
private[impl] class RedisSetImpl[Elem: ClassTag, Result[_]](key: String, redis: RedisConnector)(implicit builder: Builders.ResultBuilder[Result], runtime: RedisRuntime) extends RedisSet[Elem, Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  override def add(elements: Elem*): Result[RedisSet[Elem, Result]] = {
    redis.setAdd(key, elements: _*).map(_ => This).recoverWithDefault(This)
  }

  override def contains(element: Elem): Result[Boolean] = {
    redis.setIsMember(key, element).recoverWithDefault(false)
  }

  override def remove(element: Elem*): Result[RedisSet[Elem, Result]] = {
    redis.setRemove(key, element: _*).map(_ => This).recoverWithDefault(This)
  }

  override def toSet: Result[Set[Elem]] = {
    redis.setMembers[Elem](key).recoverWithDefault(Set.empty)
  }

  override def size: Result[Long] = {
    redis.setSize(key).recoverWithDefault(0)
  }

  override def isEmpty: Result[Boolean] = {
    redis.setSize(key).map(_ === 0).recoverWithDefault(true)
  }

  override def nonEmpty: Result[Boolean] = {
    redis.setSize(key).map(_ > 0).recoverWithDefault(false)
  }
}
