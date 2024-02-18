package play.api.cache.redis.connector

import org.scalamock.scalatest.AsyncMockFactory
import redis.*
import redis.api.ListPivot
import redis.protocol.RedisReply

import scala.concurrent.{ExecutionContext, Future}

// this is implemented due to a bug in ScalaMock 6.0.0-M1 and reported as https://github.com/paulbutcher/ScalaMock/issues/503
private trait RedisCommandsMock {

  def get[R: ByteStringDeserializer](key: String): Future[Option[R]]

  def set[V: ByteStringSerializer](key: String, value: V, exSeconds: Option[Long], pxMilliseconds: Option[Long], NX: Boolean, XX: Boolean): Future[Boolean]

  def expire(key: String, seconds: Long): Future[Boolean]

  def mset[V: ByteStringSerializer](keysValues: Map[String, V]): Future[Boolean]

  def msetnx[V: ByteStringSerializer](keysValues: Map[String, V]): Future[Boolean]

  def incrby(key: String, increment: Long): Future[Long]

  def lrange[R: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[R]]

  def lrem[V: ByteStringSerializer](key: String, count: Long, value: V): Future[Long]

  def ltrim(key: String, start: Long, stop: Long): Future[Boolean]

  def linsert[V: ByteStringSerializer](key: String, beforeAfter: ListPivot, pivot: String, value: V): Future[Long]

  def hincrby(key: String, field: String, increment: Long): Future[Long]

  def hset[V: ByteStringSerializer](key: String, field: String, value: V): Future[Boolean]

  def zcard(key: String): Future[Long]

  def zscore[V: ByteStringSerializer](key: String, member: V): Future[Option[Double]]

  def zrange[V: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[V]]

  def zrevrange[V: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[V]]

  def zaddMock[V: ByteStringSerializer](key: String, scoreMembers: Seq[(Double, V)]): Future[Long]

  def zremMock[V: ByteStringSerializer](key: String, members: Seq[V]): Future[Long]
}

private object RedisCommandsMock {

  def mock(factory: AsyncMockFactory)(implicit ec: ExecutionContext): (RedisCommands, RedisCommandsMock) = {
    val mock = factory.mock[RedisCommandsMock](factory)
    (new RedisCommandsAdapter(mock), mock)
  }

}

private class RedisCommandsAdapter(inner: RedisCommandsMock)(implicit override val executionContext: ExecutionContext) extends RedisCommands {

  override def send[T](redisCommand: RedisCommand[? <: RedisReply, T]): Future[T] =
    throw new IllegalStateException(s"Uncaught call to mock: $redisCommand")

  final override def get[R: ByteStringDeserializer](key: String): Future[Option[R]] =
    inner.get(key)

  final override def set[V: ByteStringSerializer](key: String, value: V, exSeconds: Option[Long], pxMilliseconds: Option[Long], NX: Boolean, XX: Boolean): Future[Boolean] =
    inner.set(key, value, exSeconds, pxMilliseconds, NX, XX)

  final override def expire(key: String, seconds: Long): Future[Boolean] =
    inner.expire(key, seconds)

  final override def mset[V: ByteStringSerializer](keysValues: Map[String, V]): Future[Boolean] =
    inner.mset(keysValues)

  final override def msetnx[V: ByteStringSerializer](keysValues: Map[String, V]): Future[Boolean] =
    inner.msetnx(keysValues)

  final override def incrby(key: String, increment: Long): Future[Long] =
    inner.incrby(key, increment)

  final override def lrange[R: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[R]] =
    inner.lrange(key, start, stop)

  final override def lrem[V: ByteStringSerializer](key: String, count: Long, value: V): Future[Long] =
    inner.lrem(key, count, value)

  final override def ltrim(key: String, start: Long, stop: Long): Future[Boolean] =
    inner.ltrim(key, start, stop)

  final override def linsert[V: ByteStringSerializer](key: String, beforeAfter: ListPivot, pivot: String, value: V): Future[Long] =
    inner.linsert(key, beforeAfter, pivot, value)

  final override def hincrby(key: String, field: String, increment: Long): Future[Long] =
    inner.hincrby(key, field, increment)

  final override def hset[V: ByteStringSerializer](key: String, field: String, value: V): Future[Boolean] =
    inner.hset(key, field, value)

  final override def zcard(key: String): Future[Long] =
    inner.zcard(key)

  final override def zscore[V: ByteStringSerializer](key: String, member: V): Future[Option[Double]] =
    inner.zscore(key, member)

  final override def zrange[V: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[V]] =
    inner.zrange(key, start, stop)

  final override def zrevrange[V: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[V]] =
    inner.zrevrange(key, start, stop)

  final override def zadd[V: ByteStringSerializer](key: String, scoreMembers: (Double, V)*): Future[Long] =
    inner.zaddMock(key, scoreMembers)

  final override def zrem[V: ByteStringSerializer](key: String, members: V*): Future[Long] =
    inner.zremMock(key, members)

}
