package play.api.cache.redis.impl

import org.scalamock.scalatest.AsyncMockFactoryBase
import play.api.cache.redis._

import scala.concurrent.Future

private[impl] trait RedisSetJavaMock { this: AsyncMockFactoryBase =>

  protected[impl] trait RedisSetMock extends RedisSet[String, Future] {

    final override def add(values: String*): Future[RedisSet[String, Future]] =
      addValues(values)

    def addValues(value: Seq[String]): Future[RedisSet[String, Future]]

    final override def remove(values: String*): Future[RedisSet[String, Future]] =
      removeValues(values)

    def removeValues(value: Seq[String]): Future[RedisSet[String, Future]]
  }

  implicit final protected class RedisSetOps(set: RedisSetMock) {

    def expect: RedisSetExpectation =
      new RedisSetExpectation(set)

  }

  final protected class RedisSetExpectation(set: RedisSetMock) {

    def add(value: String*): Future[Unit] =
      Future.successful {
        (set
          .addValues(_: Seq[String]))
          .expects(value)
          .returning(Future.successful(set))
          .once()
      }

    def contains(value: String, result: Boolean): Future[Unit] =
      Future.successful {
        (set
          .contains(_: String))
          .expects(value)
          .returning(Future.successful(result))
          .once()
      }

    def remove(value: String*): Future[Unit] =
      Future.successful {
        (set
          .removeValues(_: Seq[String]))
          .expects(value)
          .returning(Future.successful(set))
          .once()
      }

    def toSet(values: String*): Future[Unit] =
      Future.successful {
        (() => set.toSet)
          .expects()
          .returning(Future.successful(values.toSet))
          .once()
      }

  }

}
