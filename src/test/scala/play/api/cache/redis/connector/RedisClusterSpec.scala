package play.api.cache.redis.connector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import play.api.cache.redis._
import play.api.cache.redis.configuration._
import play.api.cache.redis.impl._
import play.api.inject.ApplicationLifecycle

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.{AfterAll, BeforeAll}

/**
  * <p>Specification of the low level connector implementing basic commands</p>
  */
class RedisClusterSpec( implicit ee: ExecutionEnv ) extends Specification with BeforeAll with AfterAll with WithApplication {

  import Implicits._

  implicit private val lifecycle = application.injector.instanceOf[ ApplicationLifecycle ]

  implicit private val runtime = RedisRuntime( "cluster", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation )

  private val serializer = new AkkaSerializerImpl( system )

  private val clusterInstance = RedisCluster( defaultCacheName, nodes = RedisHost( localhost, 7000 ) :: RedisHost( localhost, 7001 ) :: RedisHost( localhostIp, 7002 ) :: RedisHost( localhostIp, 7003 ) :: Nil, defaults )

  private val connector: RedisConnector = new RedisConnectorProvider( clusterInstance, serializer ).get

  val prefix = "cluster-test"

  "Redis cluster" should {

    "pong on ping" in new TestCase {
      connector.ping() must not( throwA[ Throwable ] ).await
    }

    "miss on get" in new TestCase {
      connector.get[ String ]( s"$prefix-$idx" ) must beNone.await
    }

    "hit after set" in new TestCase {
      connector.set( s"$prefix-$idx", "value" ) must beTrue.await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome[ Any ].await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome( "value" ).await
    }

    "ignore set if not exists when already defined" in new TestCase {
      connector.set( s"$prefix-if-not-exists-when-exists", "previous" ) must beTrue.await
      connector.set( s"$prefix-if-not-exists-when-exists", "value", ifNotExists = true ) must beFalse.await
      connector.get[ String ]( s"$prefix-if-not-exists-when-exists" ) must beSome( "previous" ).await
    }

    "perform set if not exists when undefined" in new TestCase {
      connector.get[ String ]( s"$prefix-if-not-exists" ) must beNone.await
      connector.set( s"$prefix-if-not-exists", "value", ifNotExists = true ) must beTrue.await
      connector.get[ String ]( s"$prefix-if-not-exists" ) must beSome( "value" ).await
      connector.set( s"$prefix-if-not-exists", "other", ifNotExists = true ) must beFalse.await
      connector.get[ String ]( s"$prefix-if-not-exists" ) must beSome( "value" ).await
    }

    "perform set if not exists with expiration" in new TestCase {
      connector.get[ String ]( s"$prefix-if-not-exists-with-expiration" ) must beNone.await
      connector.set( s"$prefix-if-not-exists-with-expiration", "value", 2.seconds, ifNotExists = true ) must beTrue.await
      connector.get[ String ]( s"$prefix-if-not-exists-with-expiration" ) must beSome( "value" ).await
      // wait until the first duration expires
      Future.after( 3 ) must not( throwA[ Throwable ] ).awaitFor( 4.seconds )
      connector.get[ String ]( s"$prefix-if-not-exists-with-expiration" ) must beNone.await
    }
  }

  def beforeAll( ) = {
    // initialize the connector by flushing the database
    connector.matching( s"$prefix-*" ).flatMap( connector.remove ).await
  }

  def afterAll( ) = {
    lifecycle.stop()
  }
}
