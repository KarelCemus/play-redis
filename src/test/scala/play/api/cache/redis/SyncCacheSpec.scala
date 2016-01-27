package play.api.cache.redis

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class SyncCacheSpec extends Specification with Redis {

  private type Cache = CacheApi

  private val Cache = injector.instanceOf[ Cache ]

  "Cache" should {

    "miss on get" in {
      Cache.get[ String ]( "sync-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( "sync-test-2", "value" )
      Cache.get[ String ]( "sync-test-2" ) must beSome[ Any ]
      Cache.get[ String ]( "sync-test-2" ) must beSome( "value" )
    }

    "expire refreshes expiration" in {
      Cache.set( "sync-test-10", "value", 2.second )
      Cache.get[ String ]( "sync-test-10" ) must beSome( "value" )
      Cache.expire( "sync-test-10", 1.minute )
      // wait until the first duration expires
      Thread.sleep( 3000 )
      Cache.get[ String ]( "sync-test-10" ) must beSome( "value" )
    }

    "positive exists on existing keys" in {
      Cache.set( "sync-test-11", "value" )
      Cache.exists( "sync-test-11" ) must beTrue
    }

    "negative exists on expired and missing keys" in {
      Cache.set( "sync-test-12A", "value", 1.second )
      // wait until the duration expires
      Thread.sleep( 2000 )
      Cache.exists( "sync-test-12A" ) must beFalse
      Cache.exists( "sync-test-12B" ) must beFalse
    }

    "miss after remove" in {
      Cache.set( "sync-test-3", "value" )
      Cache.get[ String ]( "sync-test-3" ) must beSome[ Any ]
      Cache.remove( "sync-test-3" )
      Cache.get[ String ]( "sync-test-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      Cache.set( "sync-test-4", "value", 1.second )
      Cache.get[ String ]( "sync-test-4" ) must beSome[ Any ]
      // wait until it expires
      Thread.sleep( 1500 )
      // miss
      Cache.get[ String ]( "sync-test-4" ) must beNone
    }

    "miss at first getOrElse " in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( "sync-test-5" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "hit at second getOrElse" in {
      val counter = new AtomicInteger( 0 )
      for ( index <- 1 to 10 ) Cache.getOrElseCounting( "sync-test-6" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "distinct different keys" in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( "sync-test-7A" )( counter ) mustEqual "value"
      Cache.getOrElseCounting( "sync-test-7B" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 2 )
    }

    "perform future and store result" in {
      val counter = new AtomicInteger( 0 )
      // perform test
      for ( index <- 1 to 5 ) Cache.getOrFutureCounting( "sync-test-8" )( counter ).sync mustEqual "value"
      // verify
      counter.get must beEqualTo( 1 )
    }

    "propagate fail in future" in {
      Cache.getOrFuture[ String ]( "sync-test-9" ){
        Future.failed( new IllegalStateException( "Exception in test." ) )
      }.sync must throwA( new IllegalStateException( "Exception in test." ) )
    }

    "support list" in {
      // store value
      Cache.set( "list", List( "A", "B", "C" ) )
      // recall
      Cache.get[ List[ String ] ]( "list" ) must beSome[ List[ String ] ]( List( "A", "B", "C" ) )
    }

    "support a byte" in {
      Cache.set( "type.byte", 0xAB.toByte )
      Cache.get[ Byte ]( "type.byte" ) must beSome[ Byte ]
      Cache.get[ Byte ]( "type.byte" ) must beSome( 0xAB.toByte )
    }

    "support a char" in {
      Cache.set( "type.char.1", 'a' )
      Cache.get[ Char ]( "type.char.1" ) must beSome[ Char ]
      Cache.get[ Char ]( "type.char.1" ) must beSome( 'a' )
      Cache.set( "type.char.2", 'b' )
      Cache.get[ Char ]( "type.char.2" ) must beSome( 'b' )
      Cache.set( "type.char.3", 'č' )
      Cache.get[ Char ]( "type.char.3" ) must beSome( 'č' )
    }

    "support a short" in {
      Cache.set( "type.short", 12.toShort )
      Cache.get[ Short ]( "type.short" ) must beSome[ Short ]
      Cache.get[ Short ]( "type.short" ) must beSome( 12.toShort )
    }

    "support an int" in {
      Cache.set( "type.int", 0xAB.toByte )
      Cache.get[ Byte ]( "type.int" ) must beSome( 0xAB.toByte )
    }

    "support a long" in {
      Cache.set( "type.long", 144L )
      Cache.get[ Long ]( "type.long" ) must beSome[ Long ]
      Cache.get[ Long ]( "type.long" ) must beSome( 144L )
    }

    "support a float" in {
      Cache.set( "type.float", 1.23f )
      Cache.get[ Float ]( "type.float" ) must beSome[ Float ]
      Cache.get[ Float ]( "type.float" ) must beSome( 1.23f )
    }

    "support a double" in {
      Cache.set( "type.double", 3.14 )
      Cache.get[ Double ]( "type.double" ) must beSome[ Double ]
      Cache.get[ Double ]( "type.double" ) must beSome( 3.14 )
    }

    "support a date" in {
      Cache.set( "type.date", new Date( 123 ) )
      Cache.get[ Date ]( "type.date" ) must beSome( new Date( 123 ) )
    }

    "support a datetime" in {
      Cache.set( "type.datetime", new DateTime( 123456 ) )
      Cache.get[ DateTime ]( "type.datetime" ) must beSome( new DateTime( 123456 ) )
    }

    "support a custom classes" in {
      Cache.set( "type.object", SimpleObject( "B", 3 ) )
      Cache.get[ SimpleObject ]( "type.object" ) must beSome( SimpleObject( "B", 3 ) )
    }

    "support a null" in {
      Cache.set( "type.null", null )
      Cache.get[ SimpleObject ]( "type.null" ) must beNone
    }

    "remove multiple keys at once" in {
      Cache.set( "sync-test-remove-multiple-1", "value" )
      Cache.get[ String ]( "sync-test-remove-multiple-1" ) must beSome[ Any ]
      Cache.set( "sync-test-remove-multiple-2", "value" )
      Cache.get[ String ]( "sync-test-remove-multiple-2" ) must beSome[ Any ]
      Cache.set( "sync-test-remove-multiple-3", "value" )
      Cache.get[ String ]( "sync-test-remove-multiple-3" ) must beSome[ Any ]
      Cache.remove( "sync-test-remove-multiple-1", "sync-test-remove-multiple-2", "sync-test-remove-multiple-3" )
      Cache.get[ String ]( "sync-test-remove-multiple-1" ) must beNone
      Cache.get[ String ]( "sync-test-remove-multiple-2" ) must beNone
      Cache.get[ String ]( "sync-test-remove-multiple-3" ) must beNone
    }
  }


  implicit class RichCache( cache: Cache ) {
    private type Accumulator = AtomicInteger

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrElseCounting( key: String )( accumulator: Accumulator ) = cache.getOrElse( key ) {
      // increment miss counter
      accumulator.incrementAndGet( )
      // return the value to store into the cache
      "value"
    }

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrFutureCounting( key: String )( accumulator: Accumulator ) = cache.getOrFuture[ String ]( key ){
      Future {
        // increment miss counter
        accumulator.incrementAndGet( )
        // return the value to store into the cache
        "value"
      }
    }
  }
}
