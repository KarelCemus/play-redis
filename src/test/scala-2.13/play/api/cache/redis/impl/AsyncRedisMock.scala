package play.api.cache.redis.impl

import org.scalamock.scalatest.AsyncMockFactory
import play.api.cache.redis.{AsynchronousResult, Done}

import scala.reflect.ClassTag

private trait AsyncRedisMock extends AsyncRedis {

  final override def removeAll(keys: String*): AsynchronousResult[Done] =
    removeAllKeys(keys)

  def removeAllKeys(keys: Seq[String]): AsynchronousResult[Done]

  final override def getAll[T: ClassTag](keys: Iterable[String]): AsynchronousResult[Seq[Option[T]]] =
    getAllKeys(keys)

  def getAllKeys[T](keys: Iterable[String]): AsynchronousResult[Seq[Option[T]]]
}

private object AsyncRedisMock {

  def mock(factory: AsyncMockFactory): (AsyncRedis, AsyncRedisMock) = {
    val mock = factory.mock[AsyncRedisMock](factory)
    (mock, mock)
  }

}
