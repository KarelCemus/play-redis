package play.cache

import java.util.concurrent.TimeUnit

import play.api.libs.concurrent.Akka

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.Timeout
import brando._
import org.specs2.mutable.Specification

/**
 * <p>Test of brando to be sure that it works etc.</p>
 */
class BrandoSpec extends Specification with RedisCacheSupport {

  sequential

  "Brando" should {

    /** instance of brando */
    val redis = Akka.system.actorOf( Redis( "localhost", 6379, database = 1 ) )

    /** timeout of cache requests */
    implicit val timeout = Timeout( 1000, TimeUnit.MILLISECONDS )

    "ping" in {
      redis ? Request( "PING" ) expects Value must beEqualTo( Pong )
    }

    "set value" in {
      redis ? Request( "SET", "some-key", "this-value" ) expects Value must beEqualTo( Ok )
    }

    "get stored value" in {
      redis ? Request( "GET", "some-key" ) expects String must beEqualTo( "this-value" )
    }

    "get non-existing value" in {
      redis ? Request( "GET", "non-existing" ) expects Option must beNone
    }

    "determine whether it contains already stored key" in {
      redis ? Request( "EXISTS", "some-key" ) expects Value must beEqualTo( 1L )
    }

    "determine whether it contains non-existent key" in {
      redis ? Request( "EXISTS", "non-existing" ) expects Value must beEqualTo( 0L )
    }

    "delete stored value" in {
      redis ? Request( "DEL", "some-key" ) expects Value must beEqualTo( 1L )
    }

    "delete already deleted value" in {
      redis ? Request( "DEL", "some-key" ) expects Value must beEqualTo( 0L )
    }

    "delete non-existing value" in {
      redis ? Request( "DEL", "non-existing" ) expects Value must beEqualTo( 0L )
    }

    "determine whether it contains deleted key" in {
      redis ? Request( "EXISTS", "some-key" ) expects Value must beEqualTo( 0L )
    }

    "flush current database" in {
      redis ? Request( "FLUSHDB" ) expects Value must beEqualTo( Ok )
    }

    "determine whether it contains key after invalidation" in {
      redis ? Request( "EXISTS", "model" ) expects Value must beEqualTo( 0L )
    }
  }

  sealed trait Expectation
  trait StringExpectation extends Expectation
  trait ValueExpectation extends Expectation
  trait OptionExpectation extends Expectation

  object String extends StringExpectation
  object Value extends ValueExpectation
  object Option extends OptionExpectation

  implicit class ExpectationTransformer( value: Option[ Any ] ) {
    def expects( expectation: StringExpectation ) = Response.AsString.unapply( value ).orNull
    def expects( expectation: ValueExpectation ) = value.orNull
    def expects( expectation: OptionExpectation ) = value
  }

  implicit class BrandoRequest( brando: ActorRef )( implicit timeout: Timeout ) {
    def ?( request: Request ) = new AskableActorRef( brando ).ask( request ).sync.asInstanceOf[ Option[ Any ] ]
  }
}
