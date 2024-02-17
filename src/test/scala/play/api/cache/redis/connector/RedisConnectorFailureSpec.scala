package play.api.cache.redis.connector

import play.api.cache.redis._
import play.api.cache.redis.test._
import redis._
import redis.api.{BEFORE, ListPivot}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class RedisConnectorFailureSpec extends AsyncUnitSpec with ImplicitFutureMaterialization {

  private val score = 1d
  private val encodedValue = "encoded"
  private val disconnected = Future.failed(SimulatedException)

  "Serializer fail" when {

    test("serialization fails") { (serializer, _, connector) =>
      for {
        _ <- serializer.failOnEncode(cacheValue)
        _ <- connector.set(cacheKey, cacheValue).assertingFailure[SerializationException]
      } yield Passed
    }

    test("decoder fails") { (serializer, commands, connector) =>
      for {
        _ <- serializer.failOnDecode(cacheValue)
        _ = (commands.get[String](_: String)(_: ByteStringDeserializer[String])).expects(cacheKey, *).returns(Some(cacheValue))
        _ <- connector.get[String](cacheKey).assertingFailure[SerializationException]
      } yield Passed
    }
  }

  "Redis returns error code" when {

    test("SET returning false") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .set[String](_: String, _: String, _: Option[Long], _: Option[Long], _: Boolean, _: Boolean)(_: ByteStringSerializer[String]))
              .expects(cacheKey, encodedValue, None, None, false, false, *)
              .returns(false)
        _ <- connector.set(cacheKey, cacheValue).assertingEqual(false)
      } yield Passed
    }

    test("EXPIRE returning false") { (_, commands, connector) =>
      for {
        _ <- (commands.expire _).expects(cacheKey, 1.minute.toSeconds).returns(false)
        _ <- connector.expire(cacheKey, 1.minute).assertingSuccess
      } yield Passed
    }

  }

  "Connector fails" when {

    test("failed SET") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .set[String](_: String, _: String, _: Option[Long], _: Option[Long], _: Boolean, _: Boolean)(_: ByteStringSerializer[String]))
              .expects(cacheKey, encodedValue, None, None, false, false, *)
              .returns(disconnected)

        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .set[String](_: String, _: String, _: Option[Long], _: Option[Long], _: Boolean, _: Boolean)(_: ByteStringSerializer[String]))
              .expects(cacheKey, encodedValue, None, Some(1.minute.toMillis), false, false, *)
              .returns(disconnected)

        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .set[String](_: String, _: String, _: Option[Long], _: Option[Long], _: Boolean, _: Boolean)(_: ByteStringSerializer[String]))
              .expects(cacheKey, encodedValue, None, None, true, false, *)
              .returns(disconnected)

        _ <- connector.set(cacheKey, cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
        _ <- connector.set(cacheKey, cacheValue, 1.minute).assertingFailure[ExecutionFailedException, SimulatedException]
        _ <- connector.set(cacheKey, cacheValue, ifNotExists = true).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed MSET") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .mset[String](_: Map[String, String])(_: ByteStringSerializer[String]))
              .expects(Map(cacheKey -> encodedValue), *)
              .returns(disconnected)
        _ <- connector.mSet(cacheKey -> cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed MSETNX") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .msetnx[String](_: Map[String, String])(_: ByteStringSerializer[String]))
              .expects(Map(cacheKey -> encodedValue), *)
              .returns(disconnected)
        _ <- connector.mSetIfNotExist(cacheKey -> cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed EXPIRE") { (_, commands, connector) =>
      for {
        _ <- (commands.expire(_: String, _: Long)).expects(cacheKey, 1.minute.toSeconds).returns(disconnected)
        _ <- connector.expire(cacheKey, 1.minute).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed INCRBY") { (_, commands, connector) =>
      for {
        _ <- (commands.incrby(_: String, _: Long)).expects(cacheKey, 1L).returns(disconnected)
        _ <- connector.increment(cacheKey, 1L).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LRANGE") { (_, commands, connector) =>
      for {
        _ <- (commands
               .lrange[String](_: String, _: Long, _: Long)(_: ByteStringDeserializer[String]))
               .expects(cacheKey, 0, -1, *)
               .returns(disconnected)
        _ <- connector.listSlice[String](cacheKey, 0, -1).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LREM") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .lrem(_: String, _: Long, _: String)(_: ByteStringSerializer[String]))
              .expects(cacheKey, 2L, encodedValue, *)
              .returns(disconnected)
        _ <- connector.listRemove(cacheKey, cacheValue, 2).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LTRIM") { (_, commands, connector) =>
      for {
        _ <- (commands.ltrim(_: String, _: Long, _: Long)).expects(cacheKey, 1L, 5L).returns(disconnected)
        _ <- connector.listTrim(cacheKey, 1, 5).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LINSERT") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode("pivot", "encodedPivot")
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .linsert[String](_: String, _: ListPivot, _: String, _: String)(_: ByteStringSerializer[String]))
              .expects(cacheKey, BEFORE, "encodedPivot", encodedValue, *)
              .returns(disconnected)
        // run the test
        _ <- connector.listInsert(cacheKey, "pivot", cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed HINCRBY") { (_, commands, connector) =>
      for {
        _ <- (commands.hincrby(_: String, _: String, _: Long)).expects(cacheKey, "field", 1L).returns(disconnected)
        // run the test
        _ <- connector.hashIncrement(cacheKey, "field", 1).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed HSET") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .hset[String](_: String, _: String, _: String)(_: ByteStringSerializer[String]))
              .expects(cacheKey, "field", encodedValue, *)
              .returns(disconnected)
        _ <- connector.hashSet(cacheKey, "field", cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZADD") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .zaddMock[String](_: String, _: Seq[(Double, String)])(_: ByteStringSerializer[String]))
              .expects(cacheKey, Seq((score, encodedValue)), *)
              .returns(disconnected)
        _ <- connector.sortedSetAdd(cacheKey, (score, cacheValue)).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZCARD") { (_, commands, connector) =>
      for {
        _ <- (commands.zcard(_: String)).expects(cacheKey).returns(disconnected)
        _ <- connector.sortedSetSize(cacheKey).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZSCORE") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .zscore[String](_: String, _: String)(_: ByteStringSerializer[String]))
              .expects(cacheKey, encodedValue, *)
              .returns(disconnected)
        _ <- connector.sortedSetScore(cacheKey, cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZREM") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .zremMock(_: String, _: Seq[String])(_: ByteStringSerializer[String]))
              .expects(cacheKey, Seq(encodedValue), *)
              .returns(disconnected)
        _ <- connector.sortedSetRemove(cacheKey, cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZRANGE") { (_, commands, connector) =>
      for {
        _ <- (commands
               .zrange[String](_: String, _: Long, _: Long)(_: ByteStringDeserializer[String]))
               .expects(cacheKey, 1, 5, *)
               .returns(disconnected)
        _ <- connector.sortedSetRange[String](cacheKey, 1, 5).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZREVRANGE") { (_, commands, connector) =>
      for {
        _ <- (commands
               .zrevrange[String](_: String, _: Long, _: Long)(_: ByteStringDeserializer[String]))
               .expects(cacheKey, 1, 5, *)
               .returns(disconnected)
        _ <- connector.sortedSetReverseRange[String](cacheKey, 1, 5).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }
  }

  private def test(name: String)(f: (SerializerAssertions, RedisCommandsMock, RedisConnector) => Future[Assertion]): Unit =
    name in {
      implicit val runtime: RedisRuntime = mock[RedisRuntime]
      val serializer: PekkoSerializer = mock[PekkoSerializer]
      val (commands: RedisCommands, mockedCommands: RedisCommandsMock) = RedisCommandsMock.mock(this)
      val connector: RedisConnector = new RedisConnectorImpl(serializer, commands)

      (() => runtime.context).expects().returns(ExecutionContext.global).anyNumberOfTimes()

      f(new SerializerAssertions(serializer), mockedCommands, connector)
    }

  private class SerializerAssertions(mock: PekkoSerializer) {

    def failOnEncode[T](value: T): Future[Unit] =
      Future.successful {
        (mock.encode(_: Any)).expects(value).returns(Failure(SimulatedException))
      }

    def encode[T](value: T, encoded: String): Future[Unit] =
      Future.successful {
        (mock.encode(_: Any)).expects(value).returns(Success(encoded))
      }

    def failOnDecode(value: String): Future[Unit] =
      Future.successful {
        (mock.decode(_: String)(_: ClassTag[String])).expects(value, *).returns(Failure(SimulatedException))
      }

  }

}
