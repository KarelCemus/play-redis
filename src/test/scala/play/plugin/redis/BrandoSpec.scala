package play.plugin.redis

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import play.api.libs.concurrent.Akka
import play.api.test.FakeApplication

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.Timeout
import brando._
import org.specs2.mutable.Specification

/**
 * <p>Test of brando to be sure that it works etc.</p>
 *
 * @author Karel Cemus
 */
class BrandoSpec extends Specification {

  sequential

  "Brando" should {

    /** instance of brando */
    val redis = Akka.system( FakeApplication( ) ).actorOf( Brando( "localhost", 6379, database = Some( 1 ) ) )

    /** timeout of cache requests */
    implicit val timeout = Timeout( 1000, TimeUnit.MILLISECONDS )

    "ping" in {
      redis ?> Request( "PING" ) must beEqualTo( Pong )
    }

    "set value" in {
      redis ?> Request( "SET", "some-key", "this-value" ) must beEqualTo( Ok )
    }

    "get stored value" in {
      redis ?>> Request( "GET", "some-key" ) must beEqualTo( "this-value" )
    }

    "get non-existing value" in {
      redis ? Request( "GET", "non-existing" ) must beNone
    }

    "determine whether it contains already stored key" in {
      redis ?> Request( "EXISTS", "some-key" ) must beEqualTo( 1L )
    }

    "determine whether it contains non-existent key" in {
      redis ?> Request( "EXISTS", "non-existing" ) must beEqualTo( 0L )
    }

    "delete stored value" in {
      redis ?> Request( "DEL", "some-key" ) must beEqualTo( 1L )
    }

    "delete already deleted value" in {
      redis ?> Request( "DEL", "some-key" ) must beEqualTo( 0L )
    }

    "delete non-existing value" in {
      redis ?> Request( "DEL", "non-existing" ) must beEqualTo( 0L )
    }

    "determine whether it contains deleted key" in {
      redis ?> Request( "EXISTS", "some-key" ) must beEqualTo( 0L )
    }

    "flush current database" in {
      redis ?> Request( "FLUSHDB" ) must beEqualTo( Ok )
    }

    "determine whether it contains key after invalidation" in {
      redis ?> Request( "EXISTS", "model" ) must beEqualTo( 0L )
    }
  }

  implicit class BrandoRequest( brando: ActorRef ) {

    def ?( request: Request )( implicit timeout: Timeout ) = {
      val response = new AskableActorRef( brando ).ask( request ).map( _.asInstanceOf[ Option[ Any ] ] )
      Await.result( response, Duration( "5s" ) )
    }

    def ?>( request: Request )( implicit timeout: Timeout ) = ?( request ).getOrElse( None )

    def ?>>( request: Request )( implicit timeout: Timeout ) = Response.AsString.unapply( ?( request ) ).getOrElse( None )
  }

}
