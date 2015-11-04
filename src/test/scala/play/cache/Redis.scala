package play.cache

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

/**
 * Provides implicits and configuration for redis tests invocation
 */
trait Redis extends EmptyRedis with RedisAsker with RedisMatcher {

  /** application context to perform operations in */
  protected implicit def application: Application = new GuiceApplicationBuilder( ).bindings( binding: _* ).build( )

  /** binding to be used inside this test */
  protected def binding: Seq[ GuiceableModule ] = Seq.empty
}

trait Implicits {

  /** waits for future responses and returns them synchronously */
  protected implicit class Synchronizer[ T ]( future: Future[ T ] ) {
    def sync = Await.result( future, 1.second )
  }

}

trait RedisAsker extends Implicits {

  implicit class RichRedis( redis: ActorRef )( implicit timeout: Timeout ) {
    def ?( request: Request ) =
      new AskableActorRef( redis ).ask( request ).sync.asInstanceOf[ Option[ Any ] ]

    def execute( command: String, params: String* ) =
      this ? Request( command, params: _* )
  }

}

trait RedisMatcher extends Implicits {

  implicit def matcher[ T ]( matcher: Matcher[ T ] ): Matcher[ Future[ T ] ] = new Matcher[ Future[ T ] ] {
    override def apply[ S <: Future[ T ] ]( value: Expectable[ S ] ): MatchResult[ S ] = {
      val matched = value.map( ( _: Future[ T ] ).sync ).applyMatcher( matcher )
      result( matched, value )
    }
  }
}

/**
 * Provides testing redis instance
 *
 * @author Karel Cemus
 */
trait RedisInstance extends RedisAsker {

  private var _redis: RichRedis = null

  /** timeout of cache requests */
  protected implicit val timeout = Timeout( 1.second )

  /** instance of brando */
  protected def redis( implicit application: Application ) = synchronized {
    if ( _redis == null ) _redis = Akka.system.actorOf( brando.Redis( "localhost", 6379, database = 1 ) )
    _redis
  }
}
