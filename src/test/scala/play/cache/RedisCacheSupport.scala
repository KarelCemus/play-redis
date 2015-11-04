package play.cache

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
//import scala.language.implicitConversions

import play.api.cache.{CacheApi=>Api}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Play}
import play.api.test.{FakeApplication, PlayRunners}
import play.cache.redis._

import org.specs2.matcher._
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments
import org.specs2.specification.Step

/**
 * Takes care of redis connection and play application
 */
trait RedisCacheSupport {
  self: Specification =>

  protected val invoke = this

  /** application context to perform operations in */
  protected implicit def application: Application = FakeApplication()

  def start( ): Unit = PlayRunners.mutex.synchronized {
    // start play application
//    if ( Running.counter.incrementAndGet( ) == 1 ) {
//      println("-------- starting ------")
//      Play.start( application )
      // reload cache in case the play application was stopped
//      play.cache.AsyncCache.reload( )
      // invalidate redis cache for test
//      application.injector.instanceOf[ CacheAPI20 ].invalidate()
//      invoke inFuture play.cache.AsyncCache.invalidate( )
//    }
  }

  def stop( ): Unit = PlayRunners.mutex.synchronized {
    // stop play application
//    if ( Running.counter.decrementAndGet( ) == 0 ) Play.stop( application )
  }

  override def map( fs: => Fragments ): Fragments = Step( start( ) ) ^ fs ^ Step( stop( ) )

  implicit def inFuture[ T ]( value: Future[ T ] ): T = Await.result( value, Duration( "5s" ) )

  implicit def matcher[ T ]( matcher: Matcher[ T ] ): Matcher[ Future[ T ] ] = new Matcher[ Future[ T ] ] {
    override def apply[ S <: Future[ T ] ]( value: Expectable[ S ] ): MatchResult[ S ] = {
      val expectable: Expectable[ Future[ T ] ] = value
      val matched = expectable.map( inFuture[ T ] _ ).applyMatcher( matcher )
      result( matched, value )
    }
  }
}

object Running {
  val counter = new AtomicInteger( 0 )
}
