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
class RedisCacheSpec extends Specification with Redis {

  private type Cache = CacheAsyncApi

  private val Cache = injector.instanceOf[ Cache ]

  "Cache" should {

    "miss on get" in {
      Cache.get[ String ]( "async-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( "async-test-2", "value" ).sync
      Cache.get[ String ]( "async-test-2" ) must beSome[ Any ]
      Cache.get[ String ]( "async-test-2" ) must beSome( "value" )
    }

    "expire refreshes expiration" in {
      Cache.set( "async-test-10", "value", 2.second ).sync
      Cache.get[ String ]( "async-test-10" ) must beSome( "value" )
      Cache.expire( "async-test-10", 1.minute ).sync
      // wait until the first duration expires
      Thread.sleep( 3000 )
      Cache.get[ String ]( "async-test-10" ) must beSome( "value" )
    }

    "positive exists on existing keys" in {
      Cache.set( "async-test-11", "value" ).sync
      Cache.exists( "async-test-11" ) must beTrue
    }

    "negative exists on expired and missing keys" in {
      Cache.set( "async-test-12A", "value", 1.second ).sync
      // wait until the duration expires
      Thread.sleep( 2000 )
      Cache.exists( "async-test-12A" ) must beFalse
      Cache.exists( "async-test-12B" ) must beFalse
    }

    "miss after remove" in {
      Cache.set( "async-test-3", "value" ).sync
      Cache.get[ String ]( "async-test-3" ) must beSome[ Any ]
      Cache.remove( "async-test-3" ).sync
      Cache.get[ String ]( "async-test-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      Cache.set( "async-test-4", "value", 1.second ).sync
      Cache.get[ String ]( "async-test-4" ) must beSome[ Any ]
      // wait until it expires
      Thread.sleep( 1500 )
      // miss
      Cache.get[ String ]( "async-test-4" ) must beNone
    }

    "miss at first getOrElse " in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( "async-test-5" )( counter ).sync mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "hit at second getOrElse" in {
      val counter = new AtomicInteger( 0 )
      for ( index <- 1 to 10 ) Cache.getOrElseCounting( "async-test-6" )( counter ).sync mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "find all matching keys" in {
      Cache.set( "async-test-13-key-A", "value", 3.second ).sync
      Cache.set( "async-test-13-note-A", "value", 3.second ).sync
      Cache.set( "async-test-13-key-B", "value", 3.second ).sync
      Cache.matching( "async-test-13*" ).sync mustEqual Set( "async-test-13-key-A", "async-test-13-note-A", "async-test-13-key-B" )
      Cache.matching( "async-test-13*A" ).sync mustEqual Set( "async-test-13-key-A", "async-test-13-note-A" )
      Cache.matching( "async-test-13-key-*" ).sync mustEqual Set( "async-test-13-key-A", "async-test-13-key-B" )
      Cache.matching( "async-test-13A*" ).sync mustEqual Set.empty
    }

    "remove all matching keys, wildcard at the end" in {
      Cache.set( "async-test-14-key-A", "value", 3.second ).sync
      Cache.set( "async-test-14-note-A", "value", 3.second ).sync
      Cache.set( "async-test-14-key-B", "value", 3.second ).sync
      Cache.matching( "async-test-14*" ).sync mustEqual Set( "async-test-14-key-A", "async-test-14-note-A", "async-test-14-key-B" )
      Cache.removeMatching( "async-test-14*" ).sync
      Cache.matching( "async-test-14*" ).sync mustEqual Set.empty
    }

    "remove all matching keys, wildcard in the middle" in {
      Cache.set( "async-test-15-key-A", "value", 3.second ).sync
      Cache.set( "async-test-15-note-A", "value", 3.second ).sync
      Cache.set( "async-test-15-key-B", "value", 3.second ).sync
      Cache.matching( "async-test-15*A" ).sync mustEqual Set( "async-test-15-key-A", "async-test-15-note-A" )
      Cache.removeMatching( "async-test-15*A").sync
      Cache.matching( "async-test-15*A").sync mustEqual Set.empty
    }

    "remove all matching keys, no match" in {
      Cache.matching( "async-test-16*" ).sync mustEqual Set.empty
      Cache.removeMatching( "async-test-16*").sync
      Cache.matching( "async-test-16*" ).sync mustEqual Set.empty
    }

    "distinct different keys" in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( "async-test-7A" )( counter ).sync mustEqual "value"
      Cache.getOrElseCounting( "async-test-7B" )( counter ).sync mustEqual "value"
      counter.get must beEqualTo( 2 )
    }

    "perform future and store result" in {
      val counter = new AtomicInteger( 0 )
      // perform test
      for ( index <- 1 to 5 ) Cache.getOrFutureCounting( "async-test-8" )( counter ).sync mustEqual "value"
      // verify
      counter.get must beEqualTo( 1 )
    }

    "propagate fail in future" in {
      Cache.getOrFuture[ String ]( "async-test-9" ){
        Future.failed( new IllegalStateException( "Exception in test." ) )
      }.sync must throwA( new IllegalStateException( "Exception in test." ) )
    }

    "support list" in {
      // store value
      Cache.set( "list", List( "A", "B", "C" ) ).sync
      // recall
      Cache.get[ List[ String ] ]( "list" ) must beSome[ List[ String ] ]( List( "A", "B", "C" ) )
    }

    "support a byte" in {
      Cache.set( "type.byte", 0xAB.toByte ).sync
      Cache.get[ Byte ]( "type.byte" ) must beSome[ Byte ]
      Cache.get[ Byte ]( "type.byte" ) must beSome( 0xAB.toByte )
    }

    "support a char" in {
      Cache.set( "type.char.1", 'a' ).sync
      Cache.get[ Char ]( "type.char.1" ) must beSome[ Char ]
      Cache.get[ Char ]( "type.char.1" ) must beSome( 'a' )
      Cache.set( "type.char.2", 'b' ).sync
      Cache.get[ Char ]( "type.char.2" ) must beSome( 'b' )
      Cache.set( "type.char.3", 'č' ).sync
      Cache.get[ Char ]( "type.char.3" ) must beSome( 'č' )
    }

    "support a short" in {
      Cache.set( "type.short", 12.toShort ).sync
      Cache.get[ Short ]( "type.short" ) must beSome[ Short ]
      Cache.get[ Short ]( "type.short" ) must beSome( 12.toShort )
    }

    "support an int" in {
      Cache.set( "type.int", 0xAB.toByte ).sync
      Cache.get[ Byte ]( "type.int" ) must beSome( 0xAB.toByte )
    }

    "support a long" in {
      Cache.set( "type.long", 144L ).sync
      Cache.get[ Long ]( "type.long" ) must beSome[ Long ]
      Cache.get[ Long ]( "type.long" ) must beSome( 144L )
    }

    "support a float" in {
      Cache.set( "type.float", 1.23f ).sync
      Cache.get[ Float ]( "type.float" ) must beSome[ Float ]
      Cache.get[ Float ]( "type.float" ) must beSome( 1.23f )
    }

    "support a double" in {
      Cache.set( "type.double", 3.14 ).sync
      Cache.get[ Double ]( "type.double" ) must beSome[ Double ]
      Cache.get[ Double ]( "type.double" ) must beSome( 3.14 )
    }

    "support a date" in {
      Cache.set( "type.date", new Date( 123 ) ).sync
      Cache.get[ Date ]( "type.date" ) must beSome( new Date( 123 ) )
    }

    "support a datetime" in {
      Cache.set( "type.datetime", new DateTime( 123456 ) ).sync
      Cache.get[ DateTime ]( "type.datetime" ) must beSome( new DateTime( 123456 ) )
    }

    "support a custom classes" in {
      Cache.set( "type.object", SimpleObject( "B", 3 ) ).sync
      Cache.get[ SimpleObject ]( "type.object" ) must beSome( SimpleObject( "B", 3 ) )
    }

    "support a null" in {
      Cache.set( "type.null", null ).sync
      Cache.get[ SimpleObject ]( "type.null" ) must beNone
    }

    "remove multiple keys at once" in {
      Cache.set( "async-test-remove-multiple-1", "value" ).sync
      Cache.get[ String ]( "async-test-remove-multiple-1" ) must beSome[ Any ]
      Cache.set( "async-test-remove-multiple-2", "value" ).sync
      Cache.get[ String ]( "async-test-remove-multiple-2" ) must beSome[ Any ]
      Cache.set( "async-test-remove-multiple-3", "value" ).sync
      Cache.get[ String ]( "async-test-remove-multiple-3" ) must beSome[ Any ]
      Cache.remove( "async-test-remove-multiple-1", "async-test-remove-multiple-2", "async-test-remove-multiple-3" ).sync
      Cache.get[ String ]( "async-test-remove-multiple-1" ) must beNone
      Cache.get[ String ]( "async-test-remove-multiple-2" ) must beNone
      Cache.get[ String ]( "async-test-remove-multiple-3" ) must beNone
    }

    "remove in batch" in {
      Cache.set( "async-test-remove-batch-1", "value" ).sync
      Cache.get[ String ]( "async-test-remove-batch-1" ) must beSome[ Any ]
      Cache.set( "async-test-remove-batch-2", "value" ).sync
      Cache.get[ String ]( "async-test-remove-batch-2" ) must beSome[ Any ]
      Cache.set( "async-test-remove-batch-3", "value" ).sync
      Cache.get[ String ]( "async-test-remove-batch-3" ) must beSome[ Any ]
      Cache.removeAll( Seq( "async-test-remove-batch-1", "async-test-remove-batch-2", "async-test-remove-batch-3" ): _* ).sync
      Cache.get[ String ]( "async-test-remove-batch-1" ) must beNone
      Cache.get[ String ]( "async-test-remove-batch-2" ) must beNone
      Cache.get[ String ]( "async-test-remove-batch-3" ) must beNone
    }
  }


  implicit class RichCache( cache: Cache ) {
    private type Accumulator = AtomicInteger

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrElseCounting( key: String )( accumulator: Accumulator ) = {
      cache.getOrElse( key ) {
        // increment miss counter
        accumulator.incrementAndGet( )
        // return the value to store into the cache
        "value"
      }
    }

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrFutureCounting( key: String )( accumulator: Accumulator ) = {
      cache.getOrFuture[ String ]( key ){
        Future {
          // increment miss counter
          accumulator.incrementAndGet( )
          // return the value to store into the cache
          "value"
        }
      }
    }
  }

}
