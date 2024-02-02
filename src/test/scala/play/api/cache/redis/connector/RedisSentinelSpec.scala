package play.api.cache.redis.connector

import akka.actor.ActorSystem
import org.scalatest.Ignore
import play.api.cache.redis._
import play.api.cache.redis.configuration._
import play.api.cache.redis.impl._
import play.api.cache.redis.test._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Ignore
class RedisSentinelSpec extends IntegrationSpec with RedisSentinelContainer {

  test("pong on ping") { connector =>
    for {
      _ <- connector.ping().assertingSuccess
    } yield Passed
  }

  test("miss on get") { connector =>
    for {
      _ <- connector.get[String]("miss-on-get").assertingEqual(None)
    } yield Passed
  }

  test("hit after set") { connector =>
    for {
      _ <- connector.set("hit-after-set", "value").assertingEqual(true)
      _ <- connector.get[String]("hit-after-set").assertingEqual(Some("value"))
    } yield Passed
  }

  test("ignore set if not exists when already defined") { connector =>
    for {
      _ <- connector.set("if-not-exists-when-exists", "previous").assertingEqual(true)
      _ <- connector.set("if-not-exists-when-exists", "value", ifNotExists = true).assertingEqual(false)
      _ <- connector.get[String]("if-not-exists-when-exists").assertingEqual(Some("previous"))
    } yield Passed
  }

  test("perform set if not exists when undefined") { connector =>
    for {
      _ <- connector.get[String]("if-not-exists").assertingEqual(None)
      _ <- connector.set("if-not-exists", "value", ifNotExists = true).assertingEqual(true)
      _ <- connector.get[String]("if-not-exists").assertingEqual(Some("value"))
      _ <- connector.set("if-not-exists", "other", ifNotExists = true).assertingEqual(false)
      _ <- connector.get[String]("if-not-exists").assertingEqual(Some("value"))
    } yield Passed
  }

  test("perform set if not exists with expiration") { connector =>
    for {
      _ <- connector.get[String]("if-not-exists-with-expiration").assertingEqual(None)
      _ <- connector.set("if-not-exists-with-expiration", "value", 300.millis, ifNotExists = true).assertingEqual(true)
      _ <- connector.get[String]("if-not-exists-with-expiration").assertingEqual(Some("value"))
      // wait until the first duration expires
      _ <- Future.after(700.millis, ())
      _ <- connector.get[String]("if-not-exists-with-expiration").assertingEqual(None)
    } yield Passed
  }

  def test(name: String)(f: RedisConnector => Future[Assertion]): Unit =
    name in {
      implicit val system: ActorSystem = ActorSystem("test", classLoader = Some(getClass.getClassLoader))
      implicit val runtime: RedisRuntime = RedisRuntime("sentinel", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation)
      implicit val application: StoppableApplication = StoppableApplication(system)
      val serializer = new AkkaSerializerImpl(system)

      lazy val sentinelInstance = RedisSentinel(
        name = "sentinel",
        masterGroup = master,
        sentinels = 0
          .until(nodes)
          .map { i =>
            RedisHost(container.containerIpAddress, container.mappedPort(sentinelPort + i))
          }
          .toList,
        settings = RedisSettings.load(
          config = Helpers.configuration.default.underlying,
          path = "play.cache.redis",
        ),
      )

      application.runAsyncInApplication {
        val connector: RedisConnector = new RedisConnectorProvider(sentinelInstance, serializer).get
        for {
          // initialize the connector by flushing the database
          keys <- connector.matching("*")
          _    <- Future.sequence(keys.map(connector.remove(_)))
          // run the test
          _    <- f(connector)
        } yield Passed
      }
    }

}
