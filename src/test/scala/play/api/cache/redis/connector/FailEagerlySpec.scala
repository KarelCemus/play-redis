package play.api.cache.redis.connector

import akka.actor.{ActorSystem, Scheduler}
import play.api.cache.redis.test._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

class FailEagerlySpec extends AsyncUnitSpec with ImplicitFutureMaterialization {
  import FailEagerlySpec._

  test("not fail regular requests when disconnected") { failEagerly =>
    val cmd = mock[RedisCommandTest[String]]
    (() => cmd.returning).expects().returns("response")
    // run the test
    failEagerly.isConnected mustEqual false
    failEagerly.send(cmd).assertingEqual("response")
  }

  test("do fail long running requests when disconnected") { failEagerly =>
    val cmd = mock[RedisCommandTest[String]]
    (() => cmd.returning).expects().returns(Promise[String]().future)
    // run the test
    failEagerly.isConnected mustEqual false
    failEagerly.send(cmd).assertingFailure[redis.actors.NoConnectionException.type]
  }

  test("not fail long running requests when connected ") { failEagerly =>
    val cmd = mock[RedisCommandTest[String]]
    (() => cmd.returning).expects().returns(Promise[String]().future)
    failEagerly.markConnected()
    // run the test
    failEagerly.isConnected mustEqual true
    failEagerly.send(cmd).assertTimeout(200.millis)
  }

  def test(name: String)(f: FailEagerlyImpl => Future[Assertion]): Unit = {
    name in {
      val system = ActorSystem("test", classLoader = Some(getClass.getClassLoader))
      val application = StoppableApplication(system)
      application.runAsyncInApplication {
        val impl = new FailEagerlyImpl()(system)
        f(impl)
      }
    }
  }
}

object FailEagerlySpec {

  import redis.RedisCommand
  import redis.protocol.RedisReply

  trait RedisCommandTest[T] extends RedisCommand[RedisReply, T] {
    def returning: Future[T]
  }

  class FailEagerlyBase(implicit system: ActorSystem) extends RequestTimeout {
    protected implicit val scheduler: Scheduler = system.scheduler
    implicit val executionContext: ExecutionContext = system.dispatcher

    def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
      redisCommand.asInstanceOf[RedisCommandTest[T]].returning
    }
  }

  final class FailEagerlyImpl(implicit system: ActorSystem) extends FailEagerlyBase with FailEagerly {

    def connectionTimeout: Option[FiniteDuration] = Some(100.millis)

    def isConnected: Boolean = connected

    def markConnected(): Unit = connected = true

    def markDisconnected(): Unit = connected = false
  }
}
