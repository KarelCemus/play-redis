package play.api.cache.redis.impl

import org.scalamock.scalatest.AsyncMockFactoryBase
import play.api.cache.redis._

import scala.concurrent.Future

private[impl] trait RedisListJavaMock { this: AsyncMockFactoryBase =>

  protected[impl] trait RedisListMock extends RedisList[String, Future]

  implicit final protected class RedisListOps(list: RedisListMock) {

    def expect: RedisListExpectation =
      new RedisListExpectation(list)

  }

  final protected class RedisListExpectation(list: RedisListMock) {

    def apply(index: Long, value: Option[String]): Future[Unit] =
      Future.successful {
        (list
          .apply(_: Long))
          .expects(index)
          .returning(
            value.fold[Future[String]](
              Future.failed(new NoSuchElementException()),
            )(
              Future.successful,
            ),
          )
          .once()
      }

    def get(index: Long, value: Option[String]): Future[Unit] =
      Future.successful {
        (list
          .get(_: Long))
          .expects(index)
          .returning(Future.successful(value))
          .once()
      }

    def prepend(value: String): Future[Unit] =
      Future.successful {
        (list
          .prepend(_: String))
          .expects(value)
          .returning(Future.successful(list))
          .once()
      }

    def append(value: String): Future[Unit] =
      Future.successful {
        (list
          .append(_: String))
          .expects(value)
          .returning(Future.successful(list))
          .once()
      }

    def headPop(value: Option[String]): Future[Unit] =
      Future.successful {
        (() => list.headPop)
          .expects()
          .returning(Future.successful(value))
          .once()
      }

    def insertBefore(pivot: String, value: String, newSize: Option[Long]): Future[Unit] =
      Future.successful {
        (list
          .insertBefore(_: String, _: String))
          .expects(pivot, value)
          .returning(Future.successful(newSize))
          .once()
      }

    def set(index: Long, value: String): Future[Unit] =
      Future.successful {
        (list
          .set(_: Long, _: String))
          .expects(index, value)
          .returning(Future.successful(list))
          .once()
      }

    def remove(value: String, count: Long = 1): Future[Unit] =
      Future.successful {
        (list
          .remove(_: String, _: Long))
          .expects(value, count)
          .returning(Future.successful(list))
          .once()
      }

    def removeAt(index: Long): Future[Unit] =
      Future.successful {
        (list
          .removeAt(_: Long))
          .expects(index)
          .returning(Future.successful(list))
          .once()
      }

    def view: RedisListViewExpectation = new RedisListViewExpectation(list)

    def modify: RedisListModificationExpectation = new RedisListModificationExpectation(list)
  }

  final protected class RedisListViewExpectation(list: RedisListMock) {

    def slice(from: Long, to: Long, value: List[String]): Future[Unit] =
      Future.successful {
        (list
          .view
          .slice(_: Long, _: Long))
          .expects(from, to)
          .returning(Future.successful(value))
          .once()
      }

  }

  final protected class RedisListModificationExpectation(list: RedisListMock) {

    def clear(): Future[Unit] =
      Future.successful {
        (() => list.modify.clear())
          .expects()
          .returning(Future.successful(list.modify))
          .once()
      }

    def slice(from: Long, to: Long): Future[Unit] =
      Future.successful {
        (list
          .modify
          .slice(_: Long, _: Long))
          .expects(from, to)
          .returning(Future.successful(list.modify))
          .once()
      }

  }

}
