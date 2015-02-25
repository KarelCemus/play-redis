package play.plugin.redis

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Success

import play.api.test._

import org.specs2.execute.{AsResult, Result}
import org.specs2.matcher._
import org.specs2.mutable.Specification
import org.specs2.specification._

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 *
 * @author Karel Cemus
 */
class CacheSpec extends Specification with AroundExample with BeforeExample {

  /** application context to perform operations in */
  protected def application = new FakeApplication( additionalPlugins = Seq(
    "play.api.libs.concurrent.AkkaPlugin",
    "play.plugin.redis.RedisCachePlugin",
    "play.plugin.redis.ExtendedRedisCachePlugin"
  ) )

  override def around[ T: AsResult ]( t: => T ): Result = {
    Helpers.running( application ) {
      // run in fake application to let cache working
      AsResult.effectively( t )
    }
  }

  implicit def inFuture[ T ]( value: Future[ T ] ): T = Await.result( value, Duration( "5s" ) )

  implicit def matcher[ T ]( matcher: Matcher[ T ] ): Matcher[ Future[ T ] ] = new Matcher[ Future[ T ] ] {
    override def apply[ S <: Future[ T ] ]( value: Expectable[ S ] ): MatchResult[ S ] = {
      val expectable: Expectable[ Future[ T ] ] = value
      val matched = expectable.map( inFuture[ T ] _ ).applyMatcher( matcher )
      result( matched, value )
    }
  }

  val invoke = this

  "Cache" should {

    sequential

    "miss on get" in {
      Cache.get[ String ]( "test-key-1" ) must beNone
    }

    "hit after set" in {
      invoke inFuture Cache.set( "test-key-2", "value" )
      Cache.get[ String ]( "test-key-2" ) must beSome[ Any ]
    }

    "miss after remove" in {

      invoke inFuture Cache.set( "test-key-3", "value" )
      Cache.get[ String ]( "test-key-3" ).isDefined must beTrue
      invoke inFuture Cache.remove( "test-key-3" )
      Cache.get[ String ]( "test-key-3" ) must beNone
    }

    "miss after timeout" in {

      // set
      invoke inFuture Cache.set( "test-key-4", "value", Some( 1 ) )
      Cache.get[ String ]( "test-key-4" ).isDefined must beTrue

      // wait until it expires
      Thread.sleep( 2000 )

      // miss
      Cache.get[ String ]( "test-key-4" ) must beNone
    }

    "miss at first getOrElse " in {

      val counter = new AtomicInteger( 0 )
      cachedValue( "test-key-5", counter ) must beSome( "value" )
      counter.get must beEqualTo( 1 )
    }

    "hit at second getOrElse" in {

      val counter = new AtomicInteger( 0 )
      for ( index <- 1 to 10 )
        cachedValue( "test-key-6", counter ) must beSome( "value" )
      counter.get must beEqualTo( 1 )
    }

    "distinct different keys" in {

      val counter = new AtomicInteger( 0 )
      cachedValue( "test-key-7A", counter ) must beSome( "value" )
      cachedValue( "test-key-7B", counter ) must beSome( "value" )
      counter.get must beEqualTo( 2 )
    }

    "perform future and store result" in {

      val counter = new AtomicInteger( 0 )
      val future = {

        // increment miss counter
        counter.incrementAndGet( )
        // return the future object
        Future.apply( "value" )
      }

      // perform test
      for ( index <- 1 to 5 ) {
        Cache.getOrElse[ String ]( "test-key-8" )( future ).map( _.toOption ) must beSome( "value" )

        // BUGFIX solution to synchronization issue. When this wasn't here,
        // the cache was synchronized a bit later and then it computed the
        // value twice, instead of just one. Adding this wait time it gives
        // a chance to cache to synchronize it
        Thread.sleep( 100 )
      }

      // verify
      counter.get must beEqualTo( 1 )
    }

    "propagate fail in future" in {

      val future = {
        Future.failed {
          new IllegalStateException( "Exception in test." )
        }
      }
      invoke inFuture Cache.getOrElse[ String ]( "test-key-9" )( future ) must throwA( new IllegalStateException( "Exception in test." ) )
    }

    "support list" in {

      // store value
      invoke inFuture Cache.set( "list", List( "A", "B", "C" ) ) must beEqualTo( Success( "OK" ) )

      // recall
      val list = invoke inFuture Cache.get[ List[ String ] ]( "list" )

      list must beSome[ List[ String ] ]
      list must beSome( List( "A", "B", "C" ) )
    }
  }

  protected def cachedValue( key: String, counter: AtomicInteger ): Future[ Option[ String ] ] =
    Cache.getOrElse[ String ]( key ) {
      // access cached value
      //      ( ) => {
      // increment miss counter
      counter.incrementAndGet( )
      // return the value to cache
      Future.successful( "value" )
      //      }
    }.map( _.toOption )

  // clear cache
  protected def before = {
    // internally initialise
    Cache.reload( )
    // invalidate cache for test
    invoke inFuture Cache.invalidate( )
  }
}
