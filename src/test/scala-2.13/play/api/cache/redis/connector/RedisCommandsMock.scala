package play.api.cache.redis.connector

import org.scalamock.scalatest.AsyncMockFactory
import redis.{ByteStringSerializer, RedisCommands}

import scala.concurrent.Future

private trait RedisCommandsMock extends RedisCommands {

  final override def zadd[V: ByteStringSerializer](key: String, scoreMembers: (Double, V)*): Future[Long] =
    zaddMock(key, scoreMembers)

  def zaddMock[V: ByteStringSerializer](key: String, scoreMembers: Seq[(Double, V)]): Future[Long]

  final override def zrem[V: ByteStringSerializer](key: String, members: V*): Future[Long] =
    zremMock(key, members)

  def zremMock[V: ByteStringSerializer](key: String, members: Seq[V]): Future[Long]
}

private object RedisCommandsMock {

  def mock(factory: AsyncMockFactory): (RedisCommands, RedisCommandsMock) = {
    val mock = factory.mock[RedisCommandsMock](factory)
    (mock, mock)
  }

}
