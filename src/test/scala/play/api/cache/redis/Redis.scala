package play.api.cache.redis

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.implicitConversions

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import akka.actor.ActorSystem
import akka.util.Timeout
import org.specs2.matcher._
import org.specs2.specification.BeforeAll
import scredis.Client

/**
  * Provides implicits and configuration for redis tests invocation
  */
trait Redis extends EmptyRedis with RedisMatcher {

  def injector = Redis.injector

  implicit val application = injector.instanceOf[ Application ]

  implicit val system = injector.instanceOf[ ActorSystem ]
}

trait Synchronization {

  protected implicit val timeout = Timeout( 3.second )

  /** waits for future responses and returns them synchronously */
  protected implicit class Synchronizer[ T ]( future: AsynchronousResult[ T ] ) {
    def sync = Await.result( future, timeout.duration )
  }

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

  def database = 1
}

/**
  * Provides testing redis instance
  *
  * @author Karel Cemus
  */
trait RedisInstance extends RedisSettings with Synchronization {

  private var _redis: Client = _

  /** instance of brando */
  protected def redis( implicit application: Application, system: ActorSystem ) = synchronized {
    if ( _redis == null ) _redis = Client( host = host, port = port, database = database )
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

  /** before all specifications reset redis database */
  override def beforeAll( ): Unit = EmptyRedis.empty
}

object EmptyRedis extends RedisInstance {

  /** already executed */
  private var executed = false

  /** empty redis database at the beginning of the test suite */
  def empty( implicit application: Application, system: ActorSystem ): Unit = synchronized {
    // execute only once
    if ( !executed ) {
      redis.flushDB( ).sync
      executed = true
    }
  }
}

/** Plain test object to be cached */
case class SimpleObject( key: String, value: Int )

object Redis {

  val injector = new GuiceApplicationBuilder( )
    // load required bindings
    .bindings( Seq.empty: _* )
    // #19: disable default EhCache module which is enabled by default
    .disable( classOf[ play.api.cache.EhCacheModule ] )
    // #19 enable Redis module
    .bindings( new RedisCacheModule )
    // produce a fake application
    .injector( )

}
