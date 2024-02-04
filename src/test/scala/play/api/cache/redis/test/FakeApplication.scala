package play.api.cache.redis.test

import org.apache.pekko.actor.ActorSystem
import play.api.inject.Injector

trait FakeApplication extends StoppableApplication {

  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder

  private lazy val theBuilder: GuiceApplicationBuilder = builder

  protected lazy val injector: Injector = theBuilder.injector()

  protected lazy val application: Application = injector.instanceOf[Application]

  implicit protected lazy val system: ActorSystem = injector.instanceOf[ActorSystem]

  protected def builder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
}
