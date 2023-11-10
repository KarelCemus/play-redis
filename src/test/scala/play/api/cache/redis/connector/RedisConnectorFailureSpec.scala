package play.api.cache.redis.connector

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.Failure

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import redis._

class RedisConnectorFailureSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {

  import Implicits._

  import org.mockito.ArgumentMatchers._

  private val key = "key"
  private val value = "value"
  private val score = 1D

  private val simulatedEx = new RuntimeException("Simulated failure.")
  private val simulatedFailure = Failure(simulatedEx)

  private val someValue = Some(value)

  private val disconnected = Future.failed(new IllegalStateException("Simulated redis status: disconnected."))

  private def anySerializer = org.mockito.ArgumentMatchers.any[ByteStringSerializer[String]]
  private def anyDeserializer = org.mockito.ArgumentMatchers.any[ByteStringDeserializer[String]]

  "Serializer failure" should {

    "fail when serialization fails" in new MockedConnector {
      serializer.encode(any[Any]) returns simulatedFailure
      // run the test
      connector.set(key, value) must throwA[SerializationException].await
    }

    "fail when decoder fails" in new MockedConnector {
      serializer.decode(anyString)(any()) returns simulatedFailure
      commands.get[String](key) returns someValue
      // run the test
      connector.get[String](key) must throwA[SerializationException].await
    }
  }

  "Redis returning error code" should {

    "SET returning false" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.set[String](anyString, anyString, any[Some[Long]], any[Some[Long]], anyBoolean, anyBoolean)(anySerializer) returns false
      // run the test
      connector.set(key, value) must not(throwA[Throwable]).await
    }

    "EXPIRE returning false" in new MockedConnector {
      commands.expire(anyString, any[Long]) returns false
      // run the test
      connector.expire(key, 1.minute) must not(throwA[Throwable]).await
    }
  }

  "Connector failure" should {

    "failed SET" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.set(anyString, anyString, any, any, anyBoolean, anyBoolean)(anySerializer) returns disconnected
      // run the test
      connector.set(key, value) must throwA[ExecutionFailedException].await
      connector.set(key, value, 1.minute) must throwA[ExecutionFailedException].await
      connector.set(key, value, ifNotExists = true) must throwA[ExecutionFailedException].await
    }

    "failed MSET" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.mset[String](any[Map[String, String]])(anySerializer) returns disconnected
      // run the test
      connector.mSet(key -> value) must throwA[ExecutionFailedException].await
    }

    "failed MSETNX" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.msetnx[String](any[Map[String, String]])(anySerializer) returns disconnected
      // run the test
      connector.mSetIfNotExist(key -> value) must throwA[ExecutionFailedException].await
    }

    "failed EXPIRE" in new MockedConnector {
      commands.expire(anyString, anyLong) returns disconnected
      // run the test
      connector.expire(key, 1.minute) must throwA[ExecutionFailedException].await
    }

    "failed INCRBY" in new MockedConnector {
      commands.incrby(anyString, anyLong) returns disconnected
      // run the test
      connector.increment(key, 1L) must throwA[ExecutionFailedException].await
    }

    "failed LRANGE" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.lrange[String](anyString, anyLong, anyLong)(anyDeserializer) returns disconnected
      // run the test
      connector.listSlice[String](key, 0, -1) must throwA[ExecutionFailedException].await
    }

    "failed LREM" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.lrem(anyString, anyLong, anyString)(anySerializer) returns disconnected
      // run the test
      connector.listRemove(key, value, 2) must throwA[ExecutionFailedException].await
    }

    "failed LTRIM" in new MockedConnector {
      commands.ltrim(anyString, anyLong, anyLong) returns disconnected
      // run the test
      connector.listTrim(key, 1, 5) must throwA[ExecutionFailedException].await
    }

    "failed LINSERT" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.linsert[String](anyString, any[api.ListPivot], anyString, anyString)(anySerializer) returns disconnected
      // run the test
      connector.listInsert(key, "pivot", value) must throwA[ExecutionFailedException].await
    }

    "failed HINCRBY" in new MockedConnector {
      commands.hincrby(anyString, anyString, anyLong) returns disconnected
      // run the test
      connector.hashIncrement(key, "field", 1) must throwA[ExecutionFailedException].await
    }

    "failed HSET" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.hset[String](anyString, anyString, anyString)(anySerializer) returns disconnected
      // run the test
      connector.hashSet(key, "field", value) must throwA[ExecutionFailedException].await
    }

    "failed ZADD" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.zadd[String](anyString, any[(Double, String)])(anySerializer) returns disconnected
      // run the test
      connector.sortedSetAdd(key, (score, value)) must throwA[ExecutionFailedException].await
    }

    "failed ZCARD" in new MockedConnector {
      commands.zcard(anyString) returns disconnected
      // run the test
      connector.sortedSetSize(key) must throwA[ExecutionFailedException].await
    }

    "failed ZSCORE" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.zscore[String](anyString, anyString)(anySerializer) returns disconnected
      // run the test
      connector.sortedSetScore(key, value) must throwA[ExecutionFailedException].await
    }

    "failed ZREM" in new MockedConnector {
      serializer.encode(anyString) returns "encoded"
      commands.zrem[String](anyString, anyString)(anySerializer) returns disconnected
      // run the test
      connector.sortedSetRemove(key, value) must throwA[ExecutionFailedException].await
    }

    "failed ZRANGE" in new MockedConnector {
      commands.zrange[String](anyString, anyLong, anyLong)(anyDeserializer) returns disconnected
      // run the test
      connector.sortedSetRange[String](key, 1, 5) must throwA[ExecutionFailedException].await
    }

    "failed ZREVRANGE" in new MockedConnector {
      commands.zrevrange[String](anyString, anyLong, anyLong)(anyDeserializer) returns disconnected
      // run the test
      connector.sortedSetReverseRange[String](key, 1, 5) must throwA[ExecutionFailedException].await
    }
  }
}
