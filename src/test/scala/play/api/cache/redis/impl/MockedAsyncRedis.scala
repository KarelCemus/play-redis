package play.api.cache.redis.impl

import org.scalamock.scalatest.AsyncMockFactoryBase
import play.api.cache.redis._
import play.api.cache.redis.test._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

private[impl] trait MockedAsyncRedis { this: AsyncMockFactoryBase =>

  implicit final protected class AsyncRedisOps(async: AsyncRedisMock) {

    def expect: AsyncRedisExpectation =
      new AsyncRedisExpectation(async)

  }

  final protected class AsyncRedisExpectation(async: AsyncRedisMock) extends ImplicitOptionMaterialization {

    private def classTagKey(key: String): String = s"classTag::$key"

    private def classTagValue: Any => String = {
      case null                                => "null"
      case v if v.getClass =~= classOf[String] => "java.lang.String"
      case other                               => throw new IllegalArgumentException(s"Unexpected value for classTag: ${other.getClass.getSimpleName}")
    }

    def getClassTag(key: String, value: Option[String]): Future[Unit] =
      get(classTagKey(key), value)

    def get[T: ClassTag](key: String, value: Option[T]): Future[Unit] =
      Future.successful {
        (async
          .get[T](_: String)(_: ClassTag[T]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(Future.successful(value))
          .once()
      }

    def getAllKeys[T](keys: Iterable[String], values: Seq[Option[T]]): Future[Unit] =
      Future.successful {
        (async
          .getAllKeys[T](_: Iterable[String]))
          .expects(keys)
          .returning(Future.successful(values))
          .once()
      }

    def setValue[T](key: String, value: T, duration: Duration): Future[Unit] =
      Future.successful {
        (async
          .set(_: String, _: Any, _: Duration))
          .expects(key, if (Option(value).isEmpty) * else value, duration)
          .returning(Future.successful(Done))
          .once()
      }

    def setClassTag[T](key: String, value: T, duration: Duration): Future[Unit] =
      setValue(classTagKey(key), value, duration)

    def set[T](key: String, value: T, duration: Duration): Future[Unit] =
      for {
        _ <- setValue(key, value, duration)
        _ <- setClassTag(key, classTagValue(value), duration)
      } yield ()

    def setValueIfNotExists[T](key: String, value: T, duration: Duration, exists: Boolean): Future[Unit] =
      Future.successful {
        (async
          .setIfNotExists(_: String, _: Any, _: Duration))
          .expects(key, if (Option(value).isEmpty) * else value, duration)
          .returning(Future.successful(exists))
          .once()
      }

    def setClassTagIfNotExists[T](key: String, value: T, duration: Duration, exists: Boolean): Future[Unit] =
      setValueIfNotExists(classTagKey(key), classTagValue(value), duration, exists)

    def setIfNotExists[T](key: String, value: T, duration: Duration, exists: Boolean): Future[Unit] =
      for {
        _ <- setValueIfNotExists(key, value, duration, exists)
        _ <- setClassTagIfNotExists(key, value, duration, exists)
      } yield ()

    def setAll(values: (String, Any)*): Future[Unit] =
      Future.successful {
        val valuesWithClassTags = values.flatMap { case (k, v) =>
          Seq((k, v), (classTagKey(k), classTagValue(v)))
        }
        (async.setAll _)
          .expects(valuesWithClassTags)
          .returning(Future.successful(Done))
          .once()
      }

    def setAllIfNotExist(values: Seq[(String, Any)], exists: Boolean): Future[Unit] =
      Future.successful {
        val valuesWithClassTags = values.flatMap { case (k, v) =>
          Seq((k, v), (classTagKey(k), classTagValue(v)))
        }
        (async.setAllIfNotExist _)
          .expects(valuesWithClassTags)
          .returning(Future.successful(exists))
          .once()
      }

    def expire(key: String, duration: Duration): Future[Unit] =
      Future.successful {
        (async
          .expire(_: String, _: Duration))
          .expects(classTagKey(key), duration)
          .returning(Future.successful(Done))
          .once()
        (async
          .expire(_: String, _: Duration))
          .expects(key, duration)
          .returning(Future.successful(Done))
          .once()
      }

    def expiresIn(key: String, duration: Option[Duration]): Future[Unit] =
      Future.successful {
        (async
          .expiresIn(_: String))
          .expects(key)
          .returning(Future.successful(duration))
          .once()
      }

    def matching(pattern: String, keys: Seq[String]): Future[Unit] =
      Future.successful {
        (async
          .matching(_: String))
          .expects(pattern)
          .returning(Future.successful(keys))
          .once()
      }

    def removeMatching(pattern: String): Future[Unit] = {
      def removePattern(patternToRemove: String): Unit =
        (async
          .removeMatching(_: String))
          .expects(patternToRemove)
          .returning(Future.successful(Done))
          .once()

      Future.successful {
        removePattern(pattern)
        removePattern(classTagKey(pattern))
      }
    }

    def exists(key: String, exists: Boolean): Future[Unit] =
      Future.successful {
        (async
          .exists(_: String))
          .expects(key)
          .returning(Future.successful(exists))
          .once()
      }

    def increment(key: String, by: Long, result: Long): Future[Unit] =
      Future.successful {
        (async
          .increment(_: String, _: Long))
          .expects(key, by)
          .returning(Future.successful(result))
          .once()
      }

    def decrement(key: String, by: Long, result: Long): Future[Unit] =
      Future.successful {
        (async
          .decrement(_: String, _: Long))
          .expects(key, by)
          .returning(Future.successful(result))
          .once()
      }

    def remove(key: String): Future[Unit] =
      Future.successful {
        (async
          .remove(_: String))
          .expects(classTagKey(key))
          .returning(Future.successful(Done))
          .once()
        (async
          .remove(_: String))
          .expects(key)
          .returning(Future.successful(Done))
          .once()
      }

    def removeAll(keys: String*): Future[Unit] =
      Future.successful {
        val keysWithClassTags = keys.flatMap { key =>
          Seq(key, classTagKey(key))
        }
        (async.removeAllKeys _)
          .expects(keysWithClassTags)
          .returning(Future.successful(Done))
          .once()
      }

    def append(key: String, value: String, expiration: Duration): Future[Unit] =
      Future.successful {
        (async
          .append(_: String, _: String, _: Duration))
          .expects(key, value, expiration)
          .returning(Future.successful(Done))
          .once()
      }

    def invalidate(): Future[Unit] =
      Future.successful {
        (() => async.invalidate())
          .expects()
          .returning(Future.successful(Done))
          .once()
      }

    def list[T: ClassTag](key: String, mock: RedisList[T, AsynchronousResult]): Future[Unit] =
      Future.successful {
        (async
          .list[T](_: String)(_: ClassTag[T]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(mock)
          .once()
      }

    def set[T: ClassTag](key: String, mock: RedisSet[T, AsynchronousResult]): Future[Unit] =
      Future.successful {
        (async
          .set[T](_: String)(_: ClassTag[T]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(mock)
          .once()
      }

    def map[T: ClassTag](key: String, mock: RedisMap[T, AsynchronousResult]): Future[Unit] =
      Future.successful {
        (async
          .map[T](_: String)(_: ClassTag[T]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(mock)
          .once()
      }

  }

}
