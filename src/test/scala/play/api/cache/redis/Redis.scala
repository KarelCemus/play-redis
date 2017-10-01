package play.api.cache.redis

import scala.concurrent.duration._
import scala.language.implicitConversions

import play.api.Application
import play.api.inject.ApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder

import akka.actor.ActorSystem
import akka.util.Timeout
import org.specs2.matcher._
import org.specs2.specification._
import redis.RedisClient

/**
  * Provides implicits and configuration for redis tests invocation
  */
trait Redis extends EmptyRedis with RedisMatcher with AfterAll {

  def injector = Redis.injector

  implicit val application: Application = injector.instanceOf[ Application ]

  implicit val system: ActorSystem = injector.instanceOf[ ActorSystem ]

  def afterAll( ) = {
    Redis.close()
  }
}

trait Synchronization {

  import play.api.cache.redis.TestHelpers._

  implicit val timeout = Timeout( 3.second )

  implicit def async2synchronizer[ T ]( future: AsynchronousResult[ T ] ): Synchronizer[ T ] = new Synchronizer[ T ]( future )
}

trait RedisMatcher extends Synchronization {

  implicit def matcher[ T ]( matcher: Matcher[ T ] ): Matcher[ AsynchronousResult[ T ] ] = new Matcher[ AsynchronousResult[ T ] ] {
    override def apply[ S <: AsynchronousResult[ T ] ]( value: Expectable[ S ] ): MatchResult[ S ] = {
      val matched = value.map( ( _: AsynchronousResult[ T ] ).sync ).applyMatcher( matcher )
      result( matched, value )
    }
  }
}

trait RedisSettings {

  def host = "localhost"

  def port = 6379

  def database = 0
}

/**
  * Provides testing redis instance
  *
  * @author Karel Cemus
  */
trait TestRedisInstance extends RedisSettings with Synchronization {

  private var _redis: RedisClient = _

  /** instance of brando */
  protected def redis( implicit application: Application, system: ActorSystem ) = synchronized {
    if ( _redis == null ) _redis = RedisClient( host = host, port = port, db = Some( database ) )
    _redis
  }
}

/**
  * Set up Redis server, empty its testing database to avoid any inference with previous tests
  *
  * @author Karel Cemus
  */
trait EmptyRedis extends BeforeAll {
  self: Redis =>

  implicit def application: Application

  implicit def system: ActorSystem

  /** before all specifications reset redis database */
  override def beforeAll( ): Unit = EmptyRedis.empty
}

object EmptyRedis extends TestRedisInstance {

  /** already executed */
  private var executed = false

  /** empty redis database at the beginning of the test suite */
  def empty( implicit application: Application, system: ActorSystem ): Unit = synchronized {
    // execute only once
    if ( !executed ) {
      redis.flushdb().sync
      executed = true
    }
  }
}

/** Plain test object to be cached */
case class SimpleObject( key: String, value: Int )

object Redis {

  import java.util.concurrent.atomic.AtomicInteger

  private val stopped = new AtomicInteger( 0 )

  val allSpecs = List(
    classOf[ configuration.RedisHostSpec ],
    classOf[ configuration.RedisInstanceSpec ],
    classOf[ configuration.RedisSettingsSpec ],
    classOf[ configuration.RedisInstanceManagerSpec ],
    classOf[ connector.RediscalaSpec ],
    classOf[ connector.RedisConnectorSpec ],
    classOf[ impl.AsynchronousCacheSpec ],
    classOf[ impl.JavaCacheSpec ],
    classOf[ impl.PlayCacheSpec ],
    classOf[ impl.RecoveryPolicySpec ],
    classOf[ impl.RedisListSpec ],
    classOf[ impl.RedisMapSpecs ],
    classOf[ impl.RedisSetSpecs ],
    classOf[ impl.RedisPrefixSpec ],
    classOf[ impl.SynchronousCacheSpec ],
    classOf[ util.ExpirationSpec ],
    classOf[ RedisComponentsSpecs ]
  ).size

  def close( ): Unit = if ( stopped.incrementAndGet() == allSpecs )
    injector.instanceOf[ ApplicationLifecycle ].stop()

  val injector = new GuiceApplicationBuilder()
    // load required bindings
    .bindings( Seq.empty: _* )
    // #19 enable Redis module
    .bindings( new RedisCacheModule )
    // produce a fake application
    .injector()
}
