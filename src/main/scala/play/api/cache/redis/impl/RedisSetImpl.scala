package play.api.cache.redis.impl

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of Set API using redis-server cache implementation.</p> */
private[impl] class RedisSetImpl[Elem: ClassTag, Result[_]](key: String, redis: RedisConnector)(implicit builder: Builders.ResultBuilder[Result], runtime: RedisRuntime) extends RedisSet[Elem, Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  def add(elements: Elem*) = {
    redis.setAdd(key, elements: _*).map(_ => This).recoverWithDefault(This)
  }

  def contains(element: Elem) = {
    redis.setIsMember(key, element).recoverWithDefault(false)
  }

  def remove(element: Elem*) = {
    redis.setRemove(key, element: _*).map(_ => This).recoverWithDefault(This)
  }

  def toSet = {
    redis.setMembers[Elem](key).recoverWithDefault(Set.empty)
  }

  def size = {
    redis.setSize(key).recoverWithDefault(0)
  }

  def isEmpty = {
    redis.setSize(key).map(_ == 0).recoverWithDefault(true)
  }

  def nonEmpty = {
    redis.setSize(key).map(_ > 0).recoverWithDefault(false)
  }
}
