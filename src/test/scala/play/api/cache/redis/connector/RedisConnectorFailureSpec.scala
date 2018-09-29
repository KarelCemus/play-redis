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

  val key = "key"
  val value = "value"

  val simulatedEx = new RuntimeException("Simulated failure.")
  val simulatedFailure = Failure(simulatedEx)

  val someValue = Some(value)

  val disconnected = Future.failed(new IllegalStateException("Simulated redis status: disconnected."))

  def anySerializer = org.mockito.ArgumentMatchers.any[ByteStringSerializer[String]]
  def anyDeserializer = org.mockito.ArgumentMatchers.any[ByteStringDeserializer[String]]

  "Serializer failure" should {

    "fail when serialization fails" in new MockedConnector {
      serializer.encode(any[Any]) returns simulatedFailure
      // run the test
      connector.set(key, value) must throwA[SerializationException].await
    }

    "fail when decoder fails" in new MockedConnector {
      serializer.decode(anyString)(any[ClassTag[_]]) returns simulatedFailure
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
  }
}
