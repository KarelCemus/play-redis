package play.cache

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import play.api.Play
import play.api.test.{FakeApplication, PlayRunners}

import org.specs2.matcher._
import org.specs2.mutable.Specification
import org.specs2.specification.{Fragments, Step}

/**
 * Takes care of redis connection and play application
 */
trait RedisCacheSupport {
  self: Specification =>

  protected val invoke = this

  /** application context to perform operations in */
  protected def application = new FakeApplication( additionalPlugins = Seq(
    "play.api.libs.concurrent.AkkaPlugin",
    "play.cache.redis.RedisCachePlugin",
    "play.cache.redis.RedisCachePlugin20",
    "play.cache.redis.RedisCacheAdapterPlugin"
  ) )

  def start( ): Unit = PlayRunners.mutex.synchronized {
    // start play application
    if ( Running.counter.incrementAndGet( ) == 1 ) {
      Play.start( application )
      // reload cache in case the play application was stopped
      play.cache.Cache.reload()
      // invalidate redis cache for test
      invoke inFuture play.cache.Cache.invalidate( )
    }
  }

  def stop( ): Unit = PlayRunners.mutex.synchronized {
    // stop play application
    if ( Running.counter.decrementAndGet( ) == 0 ) Play.stop( )
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
