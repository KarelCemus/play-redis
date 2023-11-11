package play.api.cache.redis.impl

import org.scalamock.scalatest.AsyncMockFactoryBase
import play.api.cache.redis._

import scala.concurrent.Future

private[impl] trait RedisMapJavaMock { this: AsyncMockFactoryBase =>

  protected[impl] trait RedisMapMock extends RedisMap[String, Future] {

    override final def remove(field: String*): Future[RedisMap[String, Future]] =
      removeValues(field)

    def removeValues(field: Seq[String]): Future[RedisMap[String, Future]]
  }

  final protected implicit class RedisMapOps(map: RedisMapMock) {
    def expect: RedisMapExpectation =
      new RedisMapExpectation(map)
  }

  protected final class RedisMapExpectation(map: RedisMapMock) {

    def add(key: String, value: String): Future[Unit] =
      Future.successful {
        (map.add(_: String, _: String))
          .expects(key, value)
          .returning(Future.successful(map))
          .once()
      }

    def get(key: String, value: Option[String]): Future[Unit] =
      Future.successful {
        (map.get(_: String))
          .expects(key)
          .returning(Future.successful(value))
          .once()
      }

    def contains(key: String, result: Boolean): Future[Unit] =
      Future.successful {
        (map.contains(_: String))
          .expects(key)
          .returning(Future.successful(result))
          .once()
      }

    def remove(key: String*): Future[Unit] =
      Future.successful {
        (map.removeValues(_: Seq[String]))
          .expects(key)
          .returning(Future.successful(map))
          .once()
      }

    def increment(key: String, by: Long, result: Long): Future[Unit] =
      Future.successful {
        (map.increment(_: String, _: Long))
          .expects(key, by)
          .returning(Future.successful(result))
          .once()
      }

    def toMap(values: (String, String)*): Future[Unit] =
      Future.successful {
        (() => map.toMap)
          .expects()
          .returning(Future.successful(values.toMap))
          .once()
      }

    def keySet(keys: String*): Future[Unit] =
      Future.successful {
        (() => map.keySet)
          .expects()
          .returning(Future.successful(keys.toSet))
          .once()
      }

    def values(values: String*): Future[Unit] =
      Future.successful {
        (() => map.values)
          .expects()
          .returning(Future.successful(values.toSet))
          .once()
      }
  }
}