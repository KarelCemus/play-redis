package play.api.cache.redis.connector

import org.apache.pekko.actor.ActorSystem
import play.api.cache.redis._
import play.api.cache.redis.configuration._
import play.api.cache.redis.impl._
import play.api.cache.redis.test._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class RedisClusterSpec extends IntegrationSpec with RedisClusterContainer {

  override protected def testTimeout: FiniteDuration = 60.seconds

  test("pong on ping") { connector =>
    connector.ping().assertingSuccess
  }

  test("miss on get") { connector =>
    connector.get[String]("miss-on-get").assertingEqual(None)
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
      _ <- connector.set("if-not-exists-with-expiration", "value", 500.millis, ifNotExists = true).assertingEqual(true)
      _ <- connector.get[String]("if-not-exists-with-expiration").assertingEqual(Some("value"))
      // wait until the first duration expires
      _ <- Future.after(700.millis, ())
      _ <- connector.get[String]("if-not-exists-with-expiration").assertingEqual(None)
    } yield Passed
  }

  def test(name: String)(f: RedisConnector => Future[Assertion]): Unit =
    name in {

      lazy val clusterInstance = RedisCluster(
        name = "play",
        nodes = 0
          .until(redisMaster)
          .map { i =>
            RedisHost(container.containerIpAddress, container.mappedPort(initialPort + i))
          }
          .toList,
        settings = RedisSettings.load(
          config = Helpers.configuration.default.underlying,
          path = "play.cache.redis",
        ),
      )

      def runTest: Future[Assertion] = {
        implicit val system: ActorSystem = ActorSystem("test", classLoader = Some(getClass.getClassLoader))
        implicit val runtime: RedisRuntime = RedisRuntime("cluster", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation)
        implicit val application: StoppableApplication = StoppableApplication(system)
        val serializer = new PekkoSerializerImpl(system)

        application.runAsyncInApplication {
          for {
            connector <- Future(new RedisConnectorProvider(clusterInstance, serializer).get)
            // initialize the connector by flushing the database
            keys      <- connector.matching("*")
            _         <- Future.sequence(keys.map(connector.remove(_)))
            // run the test
            _         <- f(connector)
          } yield Passed
        }
      }

      @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
      def makeAttempt(id: Int): Future[Assertion] =
        runTest.recoverWith {
          case cause: Throwable if id <= 1 =>
            log.error(s"RedisClusterSpec: test '$name', attempt $id failed, will retry", cause)
            Future.waitFor(1.second).flatMap(_ => makeAttempt(id + 1))
        }

      makeAttempt(1)
    }

}
