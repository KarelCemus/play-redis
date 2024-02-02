package play.api.cache.redis.impl

import org.scalamock.scalatest.AsyncMockFactoryBase
import play.api.cache.redis._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.{Failure, Try}

private[impl] trait RedisConnectorMock { this: AsyncMockFactoryBase =>

  protected[impl] trait RedisConnectorMock extends RedisConnector {

    final override def remove(keys: String*): Future[Unit] =
      removeValues(keys)

    def removeValues(keys: Seq[String]): Future[Unit]

    final override def mGet[T: ClassTag](keys: String*): Future[Seq[Option[T]]] =
      mGetKeys[T](keys)

    def mGetKeys[T: ClassTag](keys: Seq[String]): Future[Seq[Option[T]]]

    final override def mSet(keyValues: (String, Any)*): Future[Unit] =
      mSetValues(keyValues)

    def mSetValues(keyValues: Seq[(String, Any)]): Future[Unit]

    final override def mSetIfNotExist(keyValues: (String, Any)*): Future[Boolean] =
      mSetIfNotExistValues(keyValues)

    def mSetIfNotExistValues(keyValues: Seq[(String, Any)]): Future[Boolean]

    final override def listPrepend(key: String, value: Any*): Future[Long] =
      listPrependValues(key, value)

    def listPrependValues(key: String, values: Seq[Any]): Future[Long]

    final override def listAppend(key: String, value: Any*): Future[Long] =
      listAppendValues(key, value)

    def listAppendValues(key: String, values: Seq[Any]): Future[Long]

    final override def setAdd(key: String, value: Any*): Future[Long] =
      setAddValues(key, value)

    def setAddValues(key: String, values: Seq[Any]): Future[Long]

    final override def setRemove(key: String, value: Any*): Future[Long] =
      setRemoveValues(key, value)

    def setRemoveValues(key: String, values: Seq[Any]): Future[Long]

    final override def sortedSetAdd(key: String, scoreValues: (Double, Any)*): Future[Long] =
      sortedSetAddValues(key, scoreValues)

    def sortedSetAddValues(key: String, values: Seq[(Double, Any)]): Future[Long]

    final override def sortedSetRemove(key: String, value: Any*): Future[Long] =
      sortedSetRemoveValues(key, value)

    def sortedSetRemoveValues(key: String, values: Seq[Any]): Future[Long]

    final override def hashGet[T: ClassTag](key: String, field: String): Future[Option[T]] =
      hashGetField[T](key, field)

    def hashGetField[T: ClassTag](key: String, field: String): Future[Option[T]]

    final override def hashGet[T: ClassTag](key: String, fields: Seq[String]): Future[Seq[Option[T]]] =
      hashGetFields[T](key, fields)

    def hashGetFields[T: ClassTag](key: String, fields: Seq[String]): Future[Seq[Option[T]]]

    final override def hashRemove(key: String, field: String*): Future[Long] =
      hashRemoveValues(key, field)

    def hashRemoveValues(key: String, fields: Seq[String]): Future[Long]

    final override def hashGetAll[T: ClassTag](key: String): Future[Map[String, T]] =
      hashGetAllValues[T](key)

    def hashGetAllValues[T: ClassTag](key: String): Future[Map[String, T]]
  }

  implicit final protected class RedisConnectorExpectationOps(connector: RedisConnectorMock) {

    def expect: RedisConnectorExpectation =
      new RedisConnectorExpectation(connector)

  }

  final protected class RedisConnectorExpectation(connector: RedisConnectorMock) {

    def get[T: ClassTag](key: String, result: Try[Option[T]]): Future[Unit] =
      Future.successful {
        (connector
          .get(_: String)(_: ClassTag[?]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(Future.fromTry(result))
          .once()
      }

    def get[T: ClassTag](key: String, result: Option[T]): Future[Unit] =
      get(key, Try(result))

    def get[T: ClassTag](key: String, result: Throwable): Future[Unit] =
      get[T](key, Failure(result))

    def mGet[T: ClassTag](keys: Seq[String], result: Future[Seq[Option[T]]]): Future[Unit] =
      Future.successful {
        (connector
          .mGetKeys(_: Seq[String])(_: ClassTag[?]))
          .expects(keys, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def set[T](key: String, value: T, duration: Duration = Duration.Inf, setIfNotExists: Boolean = false, result: Future[Boolean]): Future[Unit] =
      Future.successful {
        (connector
          .set(_: String, _: Any, _: Duration, _: Boolean))
          .expects(key, if (Option(value).isEmpty) * else value, duration, setIfNotExists)
          .returning(result)
          .once()
      }

    def mSet(keyValues: Seq[(String, Any)], result: Future[Unit] = Future.unit): Future[Unit] =
      Future.successful {
        (connector
          .mSetValues(_: Seq[(String, Any)]))
          .expects(keyValues)
          .returning(result)
          .once()
      }

    def mSetIfNotExist(keyValues: Seq[(String, Any)], result: Future[Boolean]): Future[Unit] =
      Future.successful {
        (connector
          .mSetIfNotExistValues(_: Seq[(String, Any)]))
          .expects(keyValues)
          .returning(result)
          .once()
      }

    def expire(key: String, duration: Duration, result: Future[Unit] = Future.unit): Future[Unit] =
      Future.successful {
        (connector
          .expire(_: String, _: Duration))
          .expects(key, duration)
          .returning(result)
          .once()
      }

    def expiresIn(key: String, result: Future[Option[Duration]]): Future[Unit] =
      Future.successful {
        (connector
          .expiresIn(_: String))
          .expects(key)
          .returning(result)
          .once()
      }

    def remove(keys: String*): Future[Unit] =
      remove(keys, Future.unit)

    def remove(keys: Seq[String], result: Future[Unit]): Future[Unit] =
      Future.successful {
        (connector
          .removeValues(_: Seq[String]))
          .expects(keys)
          .returning(result)
          .once()
      }

    def invalidate(result: Future[Unit] = Future.unit): Future[Unit] =
      Future.successful {
        (() => connector.invalidate())
          .expects()
          .returning(result)
          .once()
      }

    def exists(key: String, result: Future[Boolean]): Future[Unit] =
      Future.successful {
        (connector
          .exists(_: String))
          .expects(key)
          .returning(result)
          .once()
      }

    def increment(key: String, by: Long, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .increment(_: String, _: Long))
          .expects(key, by)
          .returning(result)
          .once()
      }

    def append(key: String, value: String, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .append(_: String, _: String))
          .expects(key, value)
          .returning(result)
          .once()
      }

    def matching(pattern: String, result: Future[Seq[String]]): Future[Unit] =
      Future.successful {
        (connector
          .matching(_: String))
          .expects(pattern)
          .returning(result)
          .once()
      }

    def listPrepend(key: String, values: Seq[String], result: Future[Long] = Future.successful(5L)): Future[Unit] =
      Future.successful {
        (connector
          .listPrependValues(_: String, _: Seq[Any]))
          .expects(key, values)
          .returning(result)
          .once()
      }

    def listAppend[T](key: String, values: Seq[T], result: Future[Long] = Future.successful(5L)): Future[Unit] =
      Future.successful {
        (connector
          .listAppendValues(_: String, _: Seq[Any]))
          .expects(key, values)
          .returning(result)
          .once()
      }

    def listSlice[T: ClassTag](key: String, start: Long, end: Long, result: Future[Seq[T]]): Future[Unit] =
      Future.successful {
        (connector
          .listSlice(_: String, _: Long, _: Long)(_: ClassTag[?]))
          .expects(key, start, end, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def listHeadPop[T: ClassTag](key: String, result: Future[Option[T]]): Future[Unit] =
      Future.successful {
        (connector
          .listHeadPop(_: String)(_: ClassTag[?]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def listSize(key: String, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .listSize(_: String))
          .expects(key)
          .returning(result)
          .once()
      }

    def listInsert(key: String, pivot: String, value: String, result: Future[Option[Long]]): Future[Unit] =
      Future.successful {
        (connector
          .listInsert(_: String, _: String, _: Any))
          .expects(key, pivot, value)
          .returning(result)
          .once()
      }

    def listSetAt(key: String, index: Long, value: String, result: Future[Unit]): Future[Unit] =
      Future.successful {
        (connector
          .listSetAt(_: String, _: Long, _: Any))
          .expects(key, index, value)
          .returning(result)
          .once()
      }

    def listRemove(key: String, value: String, count: Long, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .listRemove(_: String, _: Any, _: Long))
          .expects(key, value, count)
          .returning(result)
          .once()
      }

    def listRemoveAt(key: String, index: Long, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .listSetAt(_: String, _: Long, _: Any))
          .expects(key, index, "play-redis:DELETED")
          .returning(Future.unit)
          .once()
        (connector
          .listRemove(_: String, _: Any, _: Long))
          .expects(key, "play-redis:DELETED", 0)
          .returning(result)
          .once()
      }

    def listTrim(key: String, start: Long, end: Long, result: Future[Unit] = Future.unit): Future[Unit] =
      Future.successful {
        (connector
          .listTrim(_: String, _: Long, _: Long))
          .expects(key, start, end)
          .returning(result)
          .once()
      }

    def setAdd(key: String, values: Seq[String], result: Future[Long] = Future.successful(5L)): Future[Unit] =
      Future.successful {
        (connector
          .setAddValues(_: String, _: Seq[Any]))
          .expects(key, values)
          .returning(result)
          .once()
      }

    def setIsMember(key: String, value: String, result: Future[Boolean]): Future[Unit] =
      Future.successful {
        (connector
          .setIsMember(_: String, _: Any))
          .expects(key, value)
          .returning(result)
          .once()
      }

    def setRemove(key: String, values: Seq[String], result: Future[Long] = Future.successful(1L)): Future[Unit] =
      Future.successful {
        (connector
          .setRemoveValues(_: String, _: Seq[Any]))
          .expects(key, values)
          .returning(result)
          .once()
      }

    def setMembers[T: ClassTag](key: String, result: Future[Set[Any]]): Future[Unit] =
      Future.successful {
        (connector
          .setMembers(_: String)(_: ClassTag[?]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def setSize(key: String, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .setSize(_: String))
          .expects(key)
          .returning(result)
          .once()
      }

    def sortedSetAdd(key: String, values: Seq[(Double, String)], result: Future[Long] = Future.successful(1L)): Future[Unit] =
      Future.successful {
        (connector
          .sortedSetAddValues(_: String, _: Seq[(Double, Any)]))
          .expects(key, values)
          .returning(result)
          .once()
      }

    def sortedSetScore(key: String, value: String, result: Future[Option[Double]]): Future[Unit] =
      Future.successful {
        (connector
          .sortedSetScore(_: String, _: Any))
          .expects(key, value)
          .returning(result)
          .once()
      }

    def sortedSetRemove(key: String, values: Seq[String], result: Future[Long] = Future.successful(1L)): Future[Unit] =
      Future.successful {
        (connector
          .sortedSetRemoveValues(_: String, _: Seq[Any]))
          .expects(key, values)
          .returning(result)
          .once()
      }

    def sortedSetRange[T: ClassTag](key: String, start: Long, end: Long, result: Future[Seq[String]]): Future[Unit] =
      Future.successful {
        (connector
          .sortedSetRange(_: String, _: Long, _: Long)(_: ClassTag[?]))
          .expects(key, start, end, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def sortedSetReverseRange[T: ClassTag](key: String, start: Long, end: Long, result: Future[Seq[String]]): Future[Unit] =
      Future.successful {
        (connector
          .sortedSetReverseRange(_: String, _: Long, _: Long)(_: ClassTag[?]))
          .expects(key, start, end, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def sortedSetSize(key: String, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .sortedSetSize(_: String))
          .expects(key)
          .returning(result)
          .once()
      }

    def hashSet(key: String, field: String, value: String, result: Future[Boolean]): Future[Unit] =
      Future.successful {
        (connector
          .hashSet(_: String, _: String, _: Any))
          .expects(key, field, value)
          .returning(result)
          .once()
      }

    def hashGet[T: ClassTag](key: String, field: String, result: Future[Option[T]]): Future[Unit] =
      Future.successful {
        (connector
          .hashGetField(_: String, _: String)(_: ClassTag[?]))
          .expects(key, field, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def hashGet[T: ClassTag](key: String, fields: Seq[String], result: Future[Seq[Option[T]]]): Future[Unit] =
      Future.successful {
        (connector
          .hashGetFields(_: String, _: Seq[String])(_: ClassTag[?]))
          .expects(key, fields, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def hashExists(key: String, field: String, result: Future[Boolean]): Future[Unit] =
      Future.successful {
        (connector
          .hashExists(_: String, _: String))
          .expects(key, field)
          .returning(result)
          .once()
      }

    def hashRemove(key: String, fields: Seq[String], result: Future[Long] = Future.successful(1L)): Future[Unit] =
      Future.successful {
        (connector
          .hashRemoveValues(_: String, _: Seq[String]))
          .expects(key, fields)
          .returning(result)
          .once()
      }

    def hashIncrement(key: String, field: String, by: Long, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .hashIncrement(_: String, _: String, _: Long))
          .expects(key, field, by)
          .returning(result)
          .once()
      }

    def hashGetAll[T: ClassTag](key: String, result: Future[Map[String, T]]): Future[Unit] =
      Future.successful {
        (connector
          .hashGetAllValues(_: String)(_: ClassTag[?]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def hashKeys(key: String, result: Future[Set[String]]): Future[Unit] =
      Future.successful {
        (connector
          .hashKeys(_: String))
          .expects(key)
          .returning(result)
          .once()
      }

    def hashValues[T: ClassTag](key: String, result: Future[Set[T]]): Future[Unit] =
      Future.successful {
        (connector
          .hashValues[T](_: String)(_: ClassTag[T]))
          .expects(key, implicitly[ClassTag[T]])
          .returning(result)
          .once()
      }

    def hashSize(key: String, result: Future[Long]): Future[Unit] =
      Future.successful {
        (connector
          .hashSize(_: String))
          .expects(key)
          .returning(result)
          .once()
      }

  }

}
