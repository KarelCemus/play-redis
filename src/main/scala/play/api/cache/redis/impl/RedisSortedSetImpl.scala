package play.api.cache.redis.impl

import play.api.cache.redis._

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

/** <p>Implementation of Set API using redis-server cache implementation.</p> */
private[impl] class RedisSortedSetImpl[Elem: ClassTag, Result[_]](
  key: String,
  redis: RedisConnector
)(implicit
  builder: Builders.ResultBuilder[Result],
  runtime: RedisRuntime
) extends RedisSortedSet[Elem, Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  override def add(scoreValues: (Double, Elem)*): Result[RedisSortedSet[Elem, Result]] =
    redis.sortedSetAdd(key, scoreValues: _*).map(_ => This).recoverWithDefault(This)

  override def contains(element: Elem): Result[Boolean] =
    redis.sortedSetScore(key, element).map(_.isDefined).recoverWithDefault(false)

  override def remove(element: Elem*): Result[RedisSortedSet[Elem, Result]] = {
    redis.sortedSetRemove(key, element: _*).map(_ => This).recoverWithDefault(This)
  }

  override def range(start: Long, stop: Long, isReverse: Boolean = false): Result[Seq[Elem]] = {
    if (isReverse) {
      redis.sortedSetReverseRange[Elem](key, start, stop).recoverWithDefault(Seq.empty)
    } else {
      redis.sortedSetRange[Elem](key, start, stop).recoverWithDefault(Seq.empty)
    }
  }

  override def size: Result[Long] =
    redis.sortedSetSize(key).recoverWithDefault(0)

  override def isEmpty: Result[Boolean] =
    builder.map(size)(_ == 0)

  override def nonEmpty: Result[Boolean] =
    builder.map(isEmpty)(x => !x)
}
