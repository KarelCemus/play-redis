package play.api.cache.redis.test

import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import org.scalatest.Assertion
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait StoppableApplication extends ApplicationLifecycle {

  private var hooks: List[() => Future[?]] = Nil

  protected def system: ActorSystem

  def shutdownAsync(): Future[Done] =
    CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)

  def runAsyncInApplication(block: => Future[Assertion])(implicit ec: ExecutionContext): Future[Assertion] =
    block
      .map(Success(_))
      .recover(Failure(_))
      .flatMap(result => Future.sequence(hooks.map(_.apply())).map(_ => result))
      .flatMap(result => shutdownAsync().map(_ => result))
      .flatMap(result => system.terminate().map(_ => result))
      .flatMap(Future.fromTry)

  final def runInApplication(block: => Assertion)(implicit ec: ExecutionContext): Future[Assertion] =
    runAsyncInApplication(Future(block))

  final override def addStopHook(hook: () => Future[?]): Unit =
    hooks = hook :: hooks

  final override def stop(): Future[?] = Future.unit

}

object StoppableApplication {

  def apply(actorSystem: ActorSystem): StoppableApplication =
    new StoppableApplication {
      override protected def system: ActorSystem = actorSystem
    }

}
