package play.cache

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions

import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}

import play.api.Application

import org.specs2.matcher._

/**
 * Provides implicits and configuration for redis tests invocation
 */
trait RedisCacheSupport {

  /** application context to perform operations in */
  protected implicit def application: Application = new GuiceApplicationBuilder( ).bindings( binding: _* ).build( )

  /** binding to be used inside this test */
  protected def binding: Seq[ GuiceableModule ] = Seq.empty

  implicit def matcher[ T ]( matcher: Matcher[ T ] ): Matcher[ Future[ T ] ] = new Matcher[ Future[ T ] ] {
    override def apply[ S <: Future[ T ] ]( value: Expectable[ S ] ): MatchResult[ S ] = {
      val matched = value.map( ( _: Future[ T ] ).sync ).applyMatcher( matcher )
      result( matched, value )
    }
  }

  /** waits for future responses and returns them synchronously */
  implicit class Synchronizer[ T ]( future: Future[ T ] ) {
    def sync = Await.result( future, Duration( "5s" ) )
  }
}
