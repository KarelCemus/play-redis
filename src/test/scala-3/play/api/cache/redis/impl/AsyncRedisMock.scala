package play.api.cache.redis.impl

import akka.Done
import org.scalamock.scalatest.AsyncMockFactory
import play.api.cache.redis.*

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

// this is implemented due to a bug in ScalaMock 6.0.0-M1 and reported as https://github.com/paulbutcher/ScalaMock/issues/503
private trait AsyncRedisMock {

  def get[A: ClassTag](key: String): Future[Option[A]]

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A]

  def removeAll(): Future[Done]

  def getAllKeys[T](keys: Iterable[String]): Future[Seq[Option[T]]]

  def getOrElse[T: ClassTag](key: String, expiration: Duration)(orElse: => T): Future[T]

  def getOrFuture[T: ClassTag](key: String, expiration: Duration)(orElse: => Future[T]): Future[T]

  def exists(key: String): Future[Boolean]

  def matching(pattern: String): Future[Seq[String]]

  def set(key: String, value: Any, expiration: Duration): Future[Done]

  def setIfNotExists(key: String, value: Any, expiration: Duration): Future[Boolean]

  def setAll(keyValues: (String, Any)*): Future[Done]

  def setAllIfNotExist(keyValues: (String, Any)*): Future[Boolean]

  def append(key: String, value: String, expiration: Duration): Future[Done]

  def expire(key: String, expiration: Duration): Future[Done]

  def expiresIn(key: String): Future[Option[Duration]]

  def remove(key: String): Future[Done]

  def remove(key1: String, key2: String, keys: String*): Future[Done]

  def removeAllKeys(keys: Seq[String]): Future[Done]

  def removeMatching(pattern: String): Future[Done]

  def invalidate(): Future[Done]

  def increment(key: String, by: Long = 1): Future[Long]

  def decrement(key: String, by: Long = 1): Future[Long]

  def list[T: ClassTag](key: String): RedisList[T, Future]

  def set[T: ClassTag](key: String): RedisSet[T, Future]

  def map[T: ClassTag](key: String): RedisMap[T, Future]

  def zset[T: ClassTag](key: String): RedisSortedSet[T, Future]
}

private object AsyncRedisMock {

  def mock(factory: AsyncMockFactory)(implicit ec: ExecutionContext): (AsyncRedis, AsyncRedisMock) = {
    val mock = factory.mock[AsyncRedisMock](factory)
    (new AsyncRedisAdapter(mock), mock)
  }

}

private class AsyncRedisAdapter(inner: AsyncRedisMock) extends AsyncRedis {

  override def get[A: ClassTag](key: String): Future[Option[A]] =
    inner.get(key)

  override def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] =
    inner.getOrElseUpdate(key, expiration)(orElse)

  override def removeAll(): Future[Done] =
    inner.removeAll()

  override def getAll[T: ClassTag](keys: Iterable[String]): Future[Seq[Option[T]]] =
    inner.getAllKeys(keys)

  override def getOrElse[T: ClassTag](key: String, expiration: Duration)(orElse: => T): Future[T] =
    inner.getOrElse(key, expiration)(orElse)

  override def getOrFuture[T: ClassTag](key: String, expiration: Duration)(orElse: => Future[T]): Future[T] =
    inner.getOrFuture(key, expiration)(orElse)

  override def exists(key: String): Future[Boolean] =
    inner.exists(key)

  override def matching(pattern: String): Future[Seq[String]] =
    inner.matching(pattern)

  override def set(key: String, value: Any, expiration: Duration): Future[Done] =
    inner.set(key, value, expiration)

  override def setIfNotExists(key: String, value: Any, expiration: Duration): Future[Boolean] =
    inner.setIfNotExists(key, value, expiration)

  override def setAll(keyValues: (String, Any)*): Future[Done] =
    inner.setAll(keyValues: _*)

  override def setAllIfNotExist(keyValues: (String, Any)*): Future[Boolean] =
    inner.setAllIfNotExist(keyValues: _*)

  override def append(key: String, value: String, expiration: Duration): Future[Done] =
    inner.append(key, value, expiration)

  override def expire(key: String, expiration: Duration): Future[Done] =
    inner.expire(key, expiration)

  override def expiresIn(key: String): Future[Option[Duration]] =
    inner.expiresIn(key)

  override def remove(key: String): Future[Done] =
    inner.remove(key)

  override def remove(key1: String, key2: String, keys: String*): Future[Done] =
    inner.remove(key1, key2, keys: _*)

  override def removeAll(keys: String*): Future[Done] =
    inner.removeAllKeys(keys)

  override def removeMatching(pattern: String): Future[Done] =
    inner.removeMatching(pattern)

  override def invalidate(): Future[Done] =
    inner.invalidate()

  override def increment(key: String, by: Long = 1): Future[Long] =
    inner.increment(key, by)

  override def decrement(key: String, by: Long = 1): Future[Long] =
    inner.decrement(key, by)

  override def list[T: ClassTag](key: String): RedisList[T, Future] =
    inner.list(key)

  override def set[T: ClassTag](key: String): RedisSet[T, Future] =
    inner.set(key)

  override def map[T: ClassTag](key: String): RedisMap[T, Future] =
    inner.map(key)

  override def zset[T: ClassTag](key: String): RedisSortedSet[T, Future] =
    inner.zset(key)

}
