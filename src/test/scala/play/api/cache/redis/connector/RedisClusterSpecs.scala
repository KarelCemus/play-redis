package play.api.cache.redis.connector

import scala.concurrent.ExecutionContext
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
class RedisClusterSpecs( implicit ee: ExecutionEnv ) extends Specification with BeforeAll with AfterAll with WithApplication {

  import Implicits._

  implicit private val lifecycle = application.injector.instanceOf[ ApplicationLifecycle ]

  implicit private val runtime = RedisRuntime( "cluster", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation )

  private val serializer = new AkkaSerializerImpl( system )

  private val clusterInstance = RedisCluster( defaultCacheName, nodes = RedisHost( localhost, defaultPort ) :: Nil, defaults )

//  todo enable cluster specs
//  private val connector: RedisConnector = new RedisConnectorProvider( clusterInstance, serializer ).get
//
//  val prefix = "cluster-test"
//
//  "Redis cluster" should {
//
//    "pong on ping" in new TestCase {
//      connector.ping() must not( throwA[ Throwable ] ).await
//    }
//  }
//
  def beforeAll( ) = {
    // initialize the connector by flushing the database
//    connector.matching( s"$prefix-*" ).flatMap( connector.remove ).await
  }

  def afterAll( ) = {
    lifecycle.stop()
  }
}
