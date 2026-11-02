package play.api.cache.redis.connector

import play.api.cache.redis.configuration.{RedisHost, RedisMasterSlaves, RedisSettings}
import play.api.cache.redis.impl.{LazyInvocation, RedisRuntime}
import play.api.cache.redis.test._
import play.api.cache.redis.{LogAndFailPolicy, RedisConnector}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class RedisMasterSlavesSpec extends IntegrationSpec with RedisMasterSlaveContainer with SuiteApplication {

  override protected def testTimeout: FiniteDuration = 60.seconds

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

  implicit private lazy val runtime: RedisRuntime =
    RedisRuntime("master-slaves", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation)

  private lazy val connector: RedisConnector = {
    val instance = RedisMasterSlaves(
      name = "master-slaves",
      master = RedisHost(host, masterPort),
      slaves = List(RedisHost(host, slavePort)),
      settings = RedisSettings.load(
        config = Helpers.configuration.default.underlying,
        path = "play.cache.redis",
      ),
    )
    new RedisConnectorProvider(instance, new PekkoSerializerImpl(actorSystem)).get
  }

  def test(name: String)(f: RedisConnector => Future[Assertion]): Unit =
    name in {
      for {
        // initialize the connector by flushing the database
        keys <- connector.matching("*")
        _    <- Future.sequence(keys.map(connector.remove(_)))
        _    <- f(connector)
      } yield Passed
    }

}
