package play.api.cache.redis.test

import org.apache.pekko.actor.CoordinatedShutdown
import org.scalatest.Assertion
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object TestApplication {

  def run(injector: Injector)(block: => Assertion)(implicit ec: ExecutionContext): Future[Assertion] =
    runAsync(injector)(Future(block))

  def runAsync(injector: Injector)(block: => Future[Assertion])(implicit ec: ExecutionContext): Future[Assertion] =
    block
      .map(Success(_))
      .recover(Failure(_))
      .flatMap(result => injector.instanceOf[CoordinatedShutdown].run(ApplicationStop).map(_ => result))
      .flatMap(Future.fromTry)

  case object ApplicationStop extends CoordinatedShutdown.Reason
}
