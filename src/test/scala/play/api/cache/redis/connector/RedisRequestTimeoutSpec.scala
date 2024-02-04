package play.api.cache.redis.connector

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import play.api.cache.redis.test.{AsyncUnitSpec, StoppableApplication}
import redis.RedisCommand
import redis.protocol.RedisReply

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}

class RedisRequestTimeoutSpec extends AsyncUnitSpec {

  override protected def testTimeout: FiniteDuration = 3.seconds

  "fail long running requests when connected but timeout defined" in {
    implicit val system: ActorSystem = ActorSystem("test")
    val application = StoppableApplication(system)

    application.runAsyncInApplication {
      val redisCommandMock = mock[RedisCommandTest[String]]
      (() => redisCommandMock.returning).expects().returns(Promise[String]().future)
      val redisRequest = new RedisRequestTimeoutImpl(timeout = Some(1.second))
      // run the test
      redisRequest.send[String](redisCommandMock).assertingFailure[redis.actors.NoConnectionException.type]
    }
  }

  private trait RedisCommandTest[T] extends RedisCommand[RedisReply, T] {
    def returning: Future[T]
  }

  private class RequestTimeoutBase(implicit system: ActorSystem) extends RequestTimeout {
    implicit protected val scheduler: Scheduler = system.scheduler
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    def send[T](redisCommand: RedisCommand[? <: RedisReply, T]): Future[T] =
      redisCommand.asInstanceOf[RedisCommandTest[T]].returning

  }

  private class RedisRequestTimeoutImpl(
    override val timeout: Option[FiniteDuration],
  )(implicit
    system: ActorSystem,
  ) extends RequestTimeoutBase
    with RedisRequestTimeout

}
