package play.cache

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Future
import scala.util.Success

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class CacheSpec extends Specification with RedisCacheSupport {

  "Cache" should {

    "miss on get" in {
      AsyncCache.get[ String ]( "async-test-1" ) must beNone
    }

    "hit after set" in {
      invoke inFuture AsyncCache.set( "async-test-2", "value" )
      AsyncCache.get[ String ]( "async-test-2" ) must beSome[ Any ]
      AsyncCache.get[ String ]( "async-test-2" ) must beSome( "value" )
    }

    "miss after remove" in {
      invoke inFuture AsyncCache.set( "async-test-3", "value" )
      AsyncCache.get[ String ]( "async-test-3" ).isDefined must beTrue
      invoke inFuture AsyncCache.remove( "async-test-3" )
      AsyncCache.get[ String ]( "async-test-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      invoke inFuture AsyncCache.set( "async-test-4", "value", Some( 1 ) )
      AsyncCache.get[ String ]( "async-test-4" ).isDefined must beTrue
      // wait until it expires
      Thread.sleep( 2000 )
      // miss
      AsyncCache.get[ String ]( "async-test-4" ) must beNone
    }

    "miss at first getOrElse " in {
      val counter = new AtomicInteger( 0 )
      cachedValue( "async-test-5", counter ) must beSome( "value" )
      counter.get must beEqualTo( 1 )
    }

    "hit at second getOrElse" in {
      val counter = new AtomicInteger( 0 )
      for ( index <- 1 to 10 )
        cachedValue( "async-test-6", counter ) must beSome( "value" )
      counter.get must beEqualTo( 1 )
    }

    "distinct different keys" in {
      val counter = new AtomicInteger( 0 )
      cachedValue( "async-test-7A", counter ) must beSome( "value" )
      cachedValue( "async-test-7B", counter ) must beSome( "value" )
      counter.get must beEqualTo( 2 )
    }

    "perform future and store result" in {
      val counter = new AtomicInteger( 0 )
      // perform test
      for ( index <- 1 to 5 ) {
        AsyncCache.getOrElse[ String ]( "async-test-8" )( orElse( counter ) ).map( _.toOption ) must beSome( "value" )

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
      val future = Future.failed( new IllegalStateException( "Exception in test." ) )
      invoke inFuture AsyncCache.getOrElse[ String ]( "async-test-9" )( future ) must throwA( new IllegalStateException( "Exception in test." ) )
    }

    "support list" in {
      // store value
      invoke inFuture AsyncCache.set( "list", List( "A", "B", "C" ) ) must beEqualTo( Success( "OK" ) )
      // recall
      val list = invoke inFuture AsyncCache.get[ List[ String ] ]( "list" )
      list must beSome[ List[ String ] ]
      list must beSome( List( "A", "B", "C" ) )
    }

    "support a byte" in {
      invoke inFuture AsyncCache.set( "type.byte", 0xAB.toByte )
      AsyncCache.get[ Byte ]( "type.byte" ) must beSome[ Byte ]
      AsyncCache.get[ Byte ]( "type.byte" ) must beSome( 0xAB.toByte )
    }

    "support a char" in {
      invoke inFuture AsyncCache.set( "type.char.1", 'a' )
      AsyncCache.get[ Char ]( "type.char.1" ) must beSome[ Char ]
      AsyncCache.get[ Char ]( "type.char.1" ) must beSome( 'a' )
      invoke inFuture AsyncCache.set( "type.char.2", 'b' )
      AsyncCache.get[ Char ]( "type.char.2" ) must beSome( 'b' )
      invoke inFuture AsyncCache.set( "type.char.3", 'č' )
      AsyncCache.get[ Char ]( "type.char.3" ) must beSome( 'č' )
    }

    "support a short" in {
      invoke inFuture AsyncCache.set( "type.short", 12.toShort )
      AsyncCache.get[ Short ]( "type.short" ) must beSome[ Short ]
      AsyncCache.get[ Short ]( "type.short" ) must beSome( 12.toShort )
    }

    "support an int" in {
      invoke inFuture AsyncCache.set( "type.int", 0xAB.toByte )
      AsyncCache.get[ Byte ]( "type.int" ) must beSome( 0xAB.toByte )
    }

    "support a long" in {
      invoke inFuture AsyncCache.set( "type.long", 144L )
      AsyncCache.get[ Long ]( "type.long" ) must beSome[ Long ]
      AsyncCache.get[ Long ]( "type.long" ) must beSome( 144L )
    }

    "support a float" in {
      invoke inFuture AsyncCache.set( "type.float", 1.23f )
      AsyncCache.get[ Float ]( "type.float" ) must beSome[ Float ]
      AsyncCache.get[ Float ]( "type.float" ) must beSome( 1.23f )
    }

    "support a double" in {
      invoke inFuture AsyncCache.set( "type.double", 3.14 )
      AsyncCache.get[ Double ]( "type.double" ) must beSome[ Double ]
      AsyncCache.get[ Double ]( "type.double" ) must beSome( 3.14 )
    }

    "support a date" in {
      invoke inFuture AsyncCache.set( "type.date", new Date( 123 ) )
      AsyncCache.get[ Date ]( "type.date" ) must beSome[ Date ]
      AsyncCache.get[ Date ]( "type.date" ) must beSome( new Date( 123 ) )
    }

    "support a datetime" in {
      invoke inFuture AsyncCache.set( "type.datetime", new DateTime( 123456 ) )
      AsyncCache.get[ DateTime ]( "type.datetime" ) must beSome[ DateTime ]
      AsyncCache.get[ DateTime ]( "type.datetime" ) must beSome( new DateTime( 123456 ) )
    }

    "support a custom classes" in {
      invoke inFuture AsyncCache.set( "type.object", SimpleObject( "B", 3 ) )
      AsyncCache.get[ SimpleObject ]( "type.object" ) must beSome[ SimpleObject ]
      AsyncCache.get[ SimpleObject ]( "type.object" ) must beSome( SimpleObject( "B", 3 ) )
    }

    "support a null" in {
      invoke inFuture AsyncCache.set( "type.null", null )
      AsyncCache.get[ SimpleObject ]( "type.null" ) must beNone
    }
  }

  protected def cachedValue( key: String, counter: AtomicInteger ): Future[ Option[ String ] ] =
    AsyncCache.getOrElse[ String ]( key )( orElse( counter ) ).map( _.toOption )

  protected def orElse( counter: AtomicInteger ): Future[ String ] = {
    // access cached value
    // increment miss counter
    counter.incrementAndGet( )
    // return the value to cache
    Future.successful( "value" )
  }
}

case class SimpleObject( key: String, value: Int )
