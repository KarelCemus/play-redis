package play.api.cache.redis.impl

import org.scalamock.scalatest.AsyncMockFactoryBase
import play.api.cache.redis._

import scala.concurrent.Future

private[impl] trait RedisListJavaMock { this: AsyncMockFactoryBase =>

  protected[impl] trait RedisListMock extends RedisList[String, Future]

  final protected implicit class RedisListOps(list: RedisListMock) {
    def expect: RedisListExpectation =
      new RedisListExpectation(list)
  }

  protected final class RedisListExpectation(list: RedisListMock) {

    def apply(index: Int, value: Option[String]): Future[Unit] =
      Future.successful {
        (list.apply(_: Int))
          .expects(index)
          .returning(
            value.fold[Future[String]](
              Future.failed(new NoSuchElementException())
            )(
              Future.successful
            )
          )
          .once()
      }

    def get(index: Int, value: Option[String]): Future[Unit] =
      Future.successful {
        (list.get(_: Int))
          .expects(index)
          .returning(Future.successful(value))
          .once()
      }

    def prepend(value: String): Future[Unit] =
      Future.successful {
        (list.prepend(_: String))
          .expects(value)
          .returning(Future.successful(list))
          .once()
      }

    def append(value: String): Future[Unit] =
      Future.successful {
        (list.append(_: String))
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
        (list.insertBefore(_: String, _: String))
          .expects(pivot, value)
          .returning(Future.successful(newSize))
          .once()
      }

    def set(index: Int, value: String): Future[Unit] =
      Future.successful {
        (list.set(_: Int, _: String))
          .expects(index, value)
          .returning(Future.successful(list))
          .once()
      }

    def remove(value: String, count: Int = 1): Future[Unit] =
      Future.successful {
        (list.remove(_: String, _: Int))
          .expects(value, count)
          .returning(Future.successful(list))
          .once()
      }

    def removeAt(index: Int): Future[Unit] =
      Future.successful {
        (list.removeAt(_: Int))
          .expects(index)
          .returning(Future.successful(list))
          .once()
      }

    def view: RedisListViewExpectation = new RedisListViewExpectation(list)

    def modify: RedisListModificationExpectation = new RedisListModificationExpectation(list)
  }

  protected final class RedisListViewExpectation(list: RedisListMock) {

    def slice(from: Int, to: Int, value: List[String]): Future[Unit] =
      Future.successful {
        (list.view.slice(_: Int, _: Int))
          .expects(from, to)
          .returning(Future.successful(value))
          .once()
      }
  }

  protected final class RedisListModificationExpectation(list: RedisListMock) {

    def clear(): Future[Unit] =
      Future.successful {
        (() => list.modify.clear())
          .expects()
          .returning(Future.successful(list.modify))
          .once()
      }

    def slice(from: Int, to: Int): Future[Unit] =
      Future.successful {
        (list.modify.slice(_: Int, _: Int))
          .expects(from, to)
          .returning(Future.successful(list.modify))
          .once()
      }
  }
}