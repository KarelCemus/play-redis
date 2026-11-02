package play.api.cache.redis.test

import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.inject.{ApplicationLifecycle, Injector}

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * One application (actor system, lifecycle) shared by all tests of the suite,
  * stopped after the last test.
  */
trait SuiteApplication extends BeforeAndAfterAll with DefaultInjector { this: Suite =>

  private val created = new AtomicBoolean(false)

  protected lazy val injector: Injector = {
    created.set(true)
    newInjector.build()
  }

  implicit protected lazy val actorSystem: ActorSystem = injector.instanceOf[ActorSystem]

  implicit protected lazy val lifecycle: ApplicationLifecycle = injector.instanceOf[ApplicationLifecycle]

  override protected def afterAll(): Unit =
    try if (created.get) Await.result(injector.instanceOf[CoordinatedShutdown].run(TestApplication.ApplicationStop), 30.seconds)
    finally super.afterAll()

}
