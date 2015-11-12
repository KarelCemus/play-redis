package play.api.cache.redis

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions

import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.concurrent.Akka

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.Timeout
import brando.Request
import org.specs2.matcher._
import org.specs2.specification.BeforeAll

/**
 * Provides implicits and configuration for redis tests invocation
 */
trait Redis extends EmptyRedis with RedisAsker with RedisMatcher {

  /** application context to perform operations in */
  protected implicit def application: Application = new GuiceApplicationBuilder( )
    // load required bindings
    .bindings( binding: _* )
    // #19: disable default EhCache module which is enabled by default
    .disable( classOf[ play.api.cache.EhCacheModule ] )
    // #19 enable Redis module
    .bindings( new RedisCacheModule )
    // produce a fake application
    .build( )

  /** binding to be used inside this test */
  protected def binding: Seq[ GuiceableModule ] = Seq.empty
}

trait Synchronization {

  /** waits for future responses and returns them synchronously */
  protected implicit class Synchronizer[ T ]( future: Future[ T ] ) {
    def sync = Await.result( future, 1.second )
  }
}

trait RedisAsker extends Synchronization {

  implicit class RichRedis( redis: ActorRef )( implicit timeout: Timeout ) {
    def ?( request: Request ) =
      new AskableActorRef( redis ).ask( request ).sync.asInstanceOf[ Option[ Any ] ]

    def execute( command: String, params: String* ) =
      this ? Request( command, params: _* )
  }

}

trait RedisMatcher extends Synchronization {

  implicit def matcher[ T ]( matcher: Matcher[ T ] ): Matcher[ Future[ T ] ] = new Matcher[ Future[ T ] ] {
    override def apply[ S <: Future[ T ] ]( value: Expectable[ S ] ): MatchResult[ S ] = {
      val matched = value.map( ( _: Future[ T ] ).sync ).applyMatcher( matcher )
      result( matched, value )
    }
  }
}

trait RedisSettings {

  /** timeout of cache requests */
  protected implicit val timeout = Timeout( 1.second )

  def host = "localhost"

  def port = 6379

  def database = 1
}

/**
 * Provides testing redis instance
 *
 * @author Karel Cemus
 */
trait RedisInstance extends RedisAsker with RedisSettings {

  private var _redis: RichRedis = null

  /** instance of brando */
  protected def redis( implicit application: Application ) = synchronized {
    if ( _redis == null ) _redis = Akka.system.actorOf( brando.Brando( host = host, port = port, database = Some( database ) ) )
    _redis
  }
}

/**
 * Set up Redis server, empty its testing database to avoid any inference with previous tests
 *
 * @author Karel Cemus
 */
trait EmptyRedis extends BeforeAll { self: Redis =>

  /** before all specifications reset redis database */
  override def beforeAll( ): Unit = EmptyRedis.empty
}

object EmptyRedis extends RedisInstance {

  /** already executed */
  private var executed = false

  /** empty redis database at the beginning of the test suite */
  def empty( implicit application: Application ): Unit = synchronized {
    // execute only once
    if ( !executed ) {
      redis execute "FLUSHDB"
      executed = true
    }
  }
}

/** Plain test object to be cached */
case class SimpleObject( key: String, value: Int )
