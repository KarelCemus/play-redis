package play.api.cache.redis.connector

import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.{RedisFuture, ScoredValue, SetArgs}
import org.scalamock.handlers.CallHandler
import org.scalamock.matchers.MatcherBase
import play.api.cache.redis._
import play.api.cache.redis.test._

import java.util.concurrent.CompletableFuture
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class RedisConnectorFailureSpec extends AsyncUnitSpec with ImplicitFutureMaterialization {

  private val score = 1d
  private val encodedValue = "encoded"
  private val disconnected = SimulatedException

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
        _ = (commands.get(_: String)).expects(cacheKey).returns(RedisFutureInTest(cacheValue))
        _ <- connector.get[String](cacheKey).assertingFailure[SerializationException]
      } yield Passed
    }
  }

  "Redis returns error code" when {

    test("SET returning false") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.setWithArgs(_: String, _: String, _: SetArgs))
              .expects(cacheKey, encodedValue, *)
              .returnsFuture("Failure")
        _ <- connector.set(cacheKey, cacheValue).assertingEqual(false)
      } yield Passed
    }

    test("EXPIRE returning false") { (_, commands, connector) =>
      for {
        _ <- (commands.expireSeconds(_: String, _: Long))
               .expects(cacheKey, 1.minute.toSeconds)
               .returnsFuture(false)
        _ <- connector.expire(cacheKey, 1.minute).assertingSuccess
      } yield Passed
    }

  }

  "Connector fails" when {

    test("failed SET") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.setWithArgs(_: String, _: String, _: SetArgs))
              .expects(cacheKey, encodedValue, Matcher.setArgs(new SetArgs()))
              .fails(disconnected)

        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.setWithArgs(_: String, _: String, _: SetArgs))
              .expects(cacheKey, encodedValue, Matcher.setArgs(new SetArgs().px(1.minute.toMillis)))
              .fails(disconnected)

        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.setWithArgs(_: String, _: String, _: SetArgs))
              .expects(cacheKey, encodedValue, Matcher.setArgs(new SetArgs().nx()))
              .fails(disconnected)

        _ <- connector.set(cacheKey, cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
        _ <- connector.set(cacheKey, cacheValue, 1.minute).assertingFailure[ExecutionFailedException, SimulatedException]
        _ <- connector.set(cacheKey, cacheValue, ifNotExists = true).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed MSET") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.mset(_: java.util.Map[String, String])).expects(Map(cacheKey -> encodedValue).asJava).fails(disconnected)
        _ <- connector.mSet(cacheKey -> cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed MSETNX") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.msetnx(_: java.util.Map[String, String])).expects(Map(cacheKey -> encodedValue).asJava).fails(disconnected)
        _ <- connector.mSetIfNotExist(cacheKey -> cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed EXPIRE") { (_, commands, connector) =>
      for {
        _ <- (commands.expireSeconds(_: String, _: Long)).expects(cacheKey, 1.minute.toSeconds).fails(disconnected)
        _ <- connector.expire(cacheKey, 1.minute).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed INCRBY") { (_, commands, connector) =>
      for {
        _ <- (commands.incrby(_: String, _: Long)).expects(cacheKey, 1L).fails(disconnected)
        _ <- connector.increment(cacheKey, 1L).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LRANGE") { (_, commands, connector) =>
      for {
        _ <- (commands.lrangeLong(_: String, _: Long, _: Long)).expects(cacheKey, 0, -1).fails(disconnected)
        _ <- connector.listSlice[String](cacheKey, 0, -1).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LREM") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.lrem(_: String, _: Long, _: String)).expects(cacheKey, 2L, encodedValue).fails(disconnected)
        _ <- connector.listRemove(cacheKey, cacheValue, 2).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LTRIM") { (_, commands, connector) =>
      for {
        _ <- (commands.ltrim(_: String, _: Long, _: Long)).expects(cacheKey, 1L, 5L).fails(disconnected)
        _ <- connector.listTrim(cacheKey, 1, 5).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed LINSERT") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode("pivot", "encodedPivot")
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.linsert(_: String, _: Boolean, _: String, _: String))
              .expects(cacheKey, true, "encodedPivot", encodedValue)
              .fails(disconnected)
        // run the test
        _ <- connector.listInsert(cacheKey, "pivot", cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed HINCRBY") { (_, commands, connector) =>
      for {
        _ <- (commands.hincrby(_: String, _: String, _: Long)).expects(cacheKey, "field", 1L).fails(disconnected)
        // run the test
        _ <- connector.hashIncrement(cacheKey, "field", 1).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed HSET") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .hsetSimple(_: String, _: String, _: String))
              .expects(cacheKey, "field", encodedValue)
              .fails(disconnected)
        _ <- connector.hashSet(cacheKey, "field", cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZADD") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.zaddMock(_: String, _: Array[ScoredValue[String]]))
              .expects(cacheKey, *)
              .fails(disconnected)
        _ <- connector.sortedSetAdd(cacheKey, (score, cacheValue)).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZCARD") { (_, commands, connector) =>
      for {
        _ <- (commands.zcard(_: String)).expects(cacheKey).fails(disconnected)
        _ <- connector.sortedSetSize(cacheKey).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZSCORE") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands
              .zscore(_: String, _: String))
              .expects(cacheKey, encodedValue)
              .fails(disconnected)
        _ <- connector.sortedSetScore(cacheKey, cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZREM") { (serializer, commands, connector) =>
      for {
        _ <- serializer.encode(cacheValue, encodedValue)
        _ = (commands.zremMock(_: String, _: Array[String]))
              .expects(cacheKey, Matcher.array(encodedValue))
              .fails(disconnected)
        _ <- connector.sortedSetRemove(cacheKey, cacheValue).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZRANGE") { (_, commands, connector) =>
      for {
        _ <- (commands.zrangeMock(_: String, _: Long, _: Long))
               .expects(cacheKey, 1, 5)
               .fails(disconnected)
        _ <- connector.sortedSetRange[String](cacheKey, 1, 5).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }

    test("failed ZREVRANGE") { (_, commands, connector) =>
      for {
        _ <- (commands.zrevrangeMock(_: String, _: Long, _: Long))
               .expects(cacheKey, 1, 5)
               .fails(disconnected)
        _ <- connector.sortedSetReverseRange[String](cacheKey, 1, 5).assertingFailure[ExecutionFailedException, SimulatedException]
      } yield Passed
    }
  }

  private def test(name: String)(f: (SerializerAssertions, RedisCommandsMock, RedisConnector) => Future[Assertion]): Unit =
    name in {
      implicit val runtime: RedisRuntime = mock[RedisRuntime]
      val serializer: PekkoSerializer = mock[PekkoSerializer]
      val mockedCommands: RedisCommandsMock = mock[RedisCommandsMock]
      val connector: RedisConnector = new RedisConnectorImpl(serializer, mockedCommands)

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

  private class RedisFutureInTest[T] extends CompletableFuture[T] with RedisFuture[T] {

    override def getError: String = null

    override def await(timeout: Long, unit: TimeUnit): Boolean = true
  }

  private object RedisFutureInTest {

    def apply[T](value: T): RedisFuture[T] = {
      val future = new RedisFutureInTest[T]
      future.complete(value)
      future
    }

    def failed[T](failure: Throwable): RedisFuture[T] = {
      val future = new RedisFutureInTest[T]
      future.completeExceptionally(failure)
      future
    }

  }

  implicit private class RichCallHandler[R](private val thiz: CallHandler[RedisFuture[R]]) {
    def returnsFuture(value: R): CallHandler[RedisFuture[R]] = thiz.returns(RedisFutureInTest(value))
    def fails(failure: Throwable): CallHandler[RedisFuture[R]] = thiz.returns(RedisFutureInTest.failed(failure))
  }

  override def convertToEqualizer[T](left: T): Equalizer[T] = super.convertToEqualizer(left)

  private object Matcher {

    def setArgs(expected: SetArgs): MatcherBase =
      new MatcherBase {
        override def canEqual(that: Any): Boolean = true

        override def equals(obj: Any): Boolean =
          obj match {
            case that: SetArgs => comparable(expected) === comparable(that)
            case _             => false
          }

        private def comparable(setArgs: SetArgs): String = {
          val args = new CommandArgs[String, String](new StringCodec())
          setArgs.build(args)
          args.toCommandString
        }

      }

    def array[T](expected: T*): MatcherBase =
      new MatcherBase {
        override def canEqual(that: Any): Boolean = true

        @SuppressWarnings(Array("org.wartremover.warts.Equals"))
        override def equals(obj: Any): Boolean = obj match {
          case that: Array[?] => that.length == expected.length && that.zip(expected).forall { case (a, b) => a == b }
          case _              => false
        }

      }

  }

}
