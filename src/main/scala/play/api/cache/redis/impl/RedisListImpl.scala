package play.api.cache.redis.impl

import scala.language.implicitConversions
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of List API using redis-server cache implementation.</p> */
private[impl] class RedisListImpl[Elem: ClassTag, Result[_]](key: String, redis: RedisConnector)(implicit builder: Builders.ResultBuilder[Result], runtime: RedisRuntime) extends RedisList[Elem, Result] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  override def prepend(element: Elem): Result[This] = prependAll(element)

  override def append(element: Elem): Result[This] = appendAll(element)

  override def +:(element: Elem): Result[This] = prependAll(element)

  override def :+(element: Elem): Result[This] = appendAll(element)

  override def ++:(elements: Iterable[Elem]): Result[This] = prependAll(elements.toSeq: _*)

  override def :++(elements: Iterable[Elem]): Result[This] = appendAll(elements.toSeq: _*)

  private def prependAll(elements: Elem*): Result[This] =
    redis.listPrepend(key, elements: _*).map(_ => This).recoverWithDefault(This)

  private def appendAll(elements: Elem*): Result[This] =
    redis.listAppend(key, elements: _*).map(_ => This).recoverWithDefault(This)

  override def apply(index: Int): Result[Elem] = redis.listSlice[Elem](key, index, index).map {
    _.headOption getOrElse (throw new NoSuchElementException(s"Element at index $index is missing."))
  }.recoverWithDefault {
    throw new NoSuchElementException(s"Element at index $index is missing.")
  }

  override def get(index: Int): Result[Option[Elem]] =
    redis.listSlice[Elem](key, index, index).map(_.headOption).recoverWithDefault(None)

  override def headPop: Result[Option[Elem]] = redis.listHeadPop[Elem](key).recoverWithDefault(None)

  override def size: Result[Long] = redis.listSize(key).recoverWithDefault(0)

  override def insertBefore(pivot: Elem, element: Elem): Result[Option[Long]] =
    redis.listInsert(key, pivot, element).recoverWithDefault(None)

  override def set(position: Int, element: Elem): Result[This] =
    redis.listSetAt(key, position, element).map(_ => This).recoverWithDefault(This)

  override def isEmpty: Result[Boolean] =
    redis.listSize(key).map(_ === 0).recoverWithDefault(true)

  override def nonEmpty: Result[Boolean] =
    redis.listSize(key).map(_ > 0).recoverWithDefault(false)

  override def view: RedisListView = ListView

  private object ListView extends RedisListView {
    override def slice(start: Int, end: Int): Result[Seq[Elem]] = redis.listSlice[Elem](key, start, end).recoverWithDefault(Seq.empty)
  }

  override def modify: RedisListModification = ListModifier

  private object ListModifier extends RedisListModification {

    override def collection: This = This

    override  def clear(): Result[RedisListModification] =
      redis.remove(key).map {
        _ => this: RedisListModification
      }.recoverWithDefault(this)

    override def slice(start: Int, end: Int): Result[RedisListModification] =
      redis.listTrim(key, start, end).map {
        _ => this: RedisListModification
      }.recoverWithDefault(this)
  }

  override def remove(element: Elem, count: Int): Result[This] =
    redis.listRemove(key, element, count).map(_ => This).recoverWithDefault(This)

  override def removeAt(position: Int): Result[This] =
    redis.listSetAt(key, position, "play-redis:DELETED").flatMap {
      _ => redis.listRemove(key, "play-redis:DELETED", count = 0)
    }.map(_ => This).recoverWithDefault(This)
}
