package play.api.cache.redis.connector

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import play.api.cache.redis.LogAndFailPolicy
import play.api.cache.redis.impl._
import play.api.inject.guice.GuiceApplicationBuilder

import org.specs2.execute.{AsResult, Result}
import org.specs2.mock.Mockito
import org.specs2.specification.{Around, Scope}
import redis.RedisCommands

/**
  * @author Karel Cemus
  */
abstract class MockedConnector extends Around with Scope with WithRuntime with WithApplication {
  import MockitoImplicits._

  protected val serializer = mock[ AkkaSerializer ]

  protected val commands = mock[ RedisCommands ]

  protected val connector: RedisConnector = new RedisConnectorImpl( serializer, commands )

  def around[ T: AsResult ]( t: => T ): Result = {
    AsResult.effectively( t )
  }
}

trait WithRuntime {

  implicit protected val runtime: RedisRuntime = RedisRuntime( "connector", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation )
}

trait WithApplication {

  protected val application = GuiceApplicationBuilder().build()

  implicit protected val system = application.actorSystem
}

object MockitoImplicits extends Mockito
