package play.api.cache.redis.impl

import play.api.cache.redis._

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

/** <p>Implementation of Set API using redis-server cache implementation.</p> */
private[impl] class RedisSortedSetImpl[Elem: ClassTag, Result[_]](key: String, redis: RedisConnector)(implicit builder: Builders.ResultBuilder[Result], runtime: RedisRuntime) extends RedisSortedSet[Elem, Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  def add(scoreValues: (Double, Elem)*) = {
    redis.zsetAdd(key, scoreValues: _*).map(_ => This).recoverWithDefault(This)
  }

  def contains(element: Elem) = {
    redis.zscore(key, element).map(_.isDefined).recoverWithDefault(false)
  }

  def remove(element: Elem*) = {
    redis.zsetRemove(key, element: _*).map(_ => This).recoverWithDefault(This)
  }

  def range(start: Long, stop: Long, isReverse: Boolean = false) = {
    if (isReverse) {
      redis.zrevRange[Elem](key, start, stop).recoverWithDefault(Seq.empty)
    } else {
      redis.zrange[Elem](key, start, stop).recoverWithDefault(Seq.empty)
    }
  }

  def size = {
    redis.zsetSize(key).recoverWithDefault(0)
  }

  def isEmpty = {
    redis.zsetSize(key).map(_ == 0).recoverWithDefault(true)
  }

  def nonEmpty = {
    redis.zsetSize(key).map(_ > 0).recoverWithDefault(false)
  }
}
