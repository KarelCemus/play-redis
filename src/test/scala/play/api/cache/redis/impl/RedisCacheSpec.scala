package play.api.cache.redis.impl

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

import play.api.cache.redis._

import org.joda.time.DateTime
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class RedisCacheSpec extends Specification with Redis {

  private type Cache = RedisCache[ SynchronousResult ]

  private val connector = injector.instanceOf[ RedisConnector ]

  // test proper implementation, no fails
  new RedisCacheSuite( "implements", "redis-cache-implements", new Cache( connector )( Builders.SynchronousBuilder, new LogAndFailPolicy ), AlwaysSuccess )

  class RedisCacheSuite( suiteName: String, prefix: String, cache: Cache, expectation: Expectation ) {

    "RedisCache" should {

      suiteName >> {

        "miss on get" in {
          cache.get[ String ]( s"$prefix-test-1" ) must beNone
        }

        "hit after set" in {
          cache.set( s"$prefix-test-2", "value" )
          cache.get[ String ]( s"$prefix-test-2" ) must beSome[ Any ]
          cache.get[ String ]( s"$prefix-test-2" ) must beSome( "value" )
        }

        "expire refreshes expiration" in {
          cache.set( s"$prefix-test-10", "value", 2.second )
          cache.get[ String ]( s"$prefix-test-10" ) must beSome( "value" )
          cache.expire( s"$prefix-test-10", 1.minute )
          // wait until the first duration expires
          Thread.sleep( 3000 )
          cache.get[ String ]( s"$prefix-test-10" ) must beSome( "value" )
        }

        "positive exists on existing keys" in {
          cache.set( s"$prefix-test-11", "value" )
          cache.exists( s"$prefix-test-11" ) must beTrue
        }

        "negative exists on expired and missing keys" in {
          cache.set( s"$prefix-test-12A", "value", 1.second )
          // wait until the duration expires
          Thread.sleep( 2000 )
          cache.exists( s"$prefix-test-12A" ) must beFalse
          cache.exists( s"$prefix-test-12B" ) must beFalse
        }

        "miss after remove" in {
          cache.set( s"$prefix-test-3", "value" )
          cache.get[ String ]( s"$prefix-test-3" ) must beSome[ Any ]
          cache.remove( s"$prefix-test-3" )
          cache.get[ String ]( s"$prefix-test-3" ) must beNone
        }

        "miss after timeout" in {
          // set
          cache.set( s"$prefix-test-4", "value", 1.second )
          cache.get[ String ]( s"$prefix-test-4" ) must beSome[ Any ]
          // wait until it expires
          Thread.sleep( 1500 )
          // miss
          cache.get[ String ]( s"$prefix-test-4" ) must beNone
        }

        "miss at first getOrElse " in {
          val counter = new AtomicInteger( 0 )
          cache.getOrElseCounting( s"$prefix-test-5" )( counter ) mustEqual "value"
          counter.get must beEqualTo( 1 )
        }

        "hit at second getOrElse" in {
          val counter = new AtomicInteger( 0 )
          for ( index <- 1 to 10 ) cache.getOrElseCounting( s"$prefix-test-6" )( counter ) mustEqual "value"
          counter.get must beEqualTo( 1 )
        }

        "find all matching keys" in {
          cache.set( s"$prefix-test-13-key-A", "value", 3.second )
          cache.set( s"$prefix-test-13-note-A", "value", 3.second )
          cache.set( s"$prefix-test-13-key-B", "value", 3.second )
          cache.matching( s"$prefix-test-13*" ) mustEqual Set( s"$prefix-test-13-key-A", s"$prefix-test-13-note-A", s"$prefix-test-13-key-B" )
          cache.matching( s"$prefix-test-13*A" ) mustEqual Set( s"$prefix-test-13-key-A", s"$prefix-test-13-note-A" )
          cache.matching( s"$prefix-test-13-key-*" ) mustEqual Set( s"$prefix-test-13-key-A", s"$prefix-test-13-key-B" )
          cache.matching( s"$prefix-test-13A*" ) mustEqual Set.empty
        }

        "remove all matching keys, wildcard at the end" in {
          cache.set( s"$prefix-test-14-key-A", "value", 3.second )
          cache.set( s"$prefix-test-14-note-A", "value", 3.second )
          cache.set( s"$prefix-test-14-key-B", "value", 3.second )
          cache.matching( s"$prefix-test-14*" ) mustEqual Set( s"$prefix-test-14-key-A", s"$prefix-test-14-note-A", s"$prefix-test-14-key-B" )
          cache.removeMatching( s"$prefix-test-14*" )
          cache.matching( s"$prefix-test-14*" ) mustEqual Set.empty
        }

        "remove all matching keys, wildcard in the middle" in {
          cache.set( s"$prefix-test-15-key-A", "value", 3.second )
          cache.set( s"$prefix-test-15-note-A", "value", 3.second )
          cache.set( s"$prefix-test-15-key-B", "value", 3.second )
          cache.matching( s"$prefix-test-15*A" ) mustEqual Set( s"$prefix-test-15-key-A", s"$prefix-test-15-note-A" )
          cache.removeMatching( s"$prefix-test-15*A")
          cache.matching( s"$prefix-test-15*A") mustEqual Set.empty
        }

        "remove all matching keys, no match" in {
          cache.matching( s"$prefix-test-16*" ) mustEqual Set.empty
          cache.removeMatching( s"$prefix-test-16*")
          cache.matching( s"$prefix-test-16*" ) mustEqual Set.empty
        }

        "distinct different keys" in {
          val counter = new AtomicInteger( 0 )
          cache.getOrElseCounting( s"$prefix-test-7A" )( counter ) mustEqual "value"
          cache.getOrElseCounting( s"$prefix-test-7B" )( counter ) mustEqual "value"
          counter.get must beEqualTo( 2 )
        }

        "perform future and store result" in {
          val counter = new AtomicInteger( 0 )
          // perform test
          for ( index <- 1 to 5 ) cache.getOrFutureCounting( s"$prefix-test-8" )( counter ).sync mustEqual "value"
          // verify
          counter.get must beEqualTo( 1 )
        }

        "propagate fail in future" in {
          cache.getOrFuture[ String ]( s"$prefix-test-9" ){
            Future.failed( new IllegalStateException( "Exception in test." ) )
          }.sync must throwA( new IllegalStateException( "Exception in test." ) )
        }

        "support list" in {
          // store value
          cache.set( s"$prefix-list", List( "A", "B", "C" ) )
          // recall
          cache.get[ List[ String ] ]( s"$prefix-list" ) must beSome[ List[ String ] ]( List( "A", "B", "C" ) )
        }

        "support a byte" in {
          cache.set( s"$prefix-type.byte", 0xAB.toByte )
          cache.get[ Byte ]( s"$prefix-type.byte" ) must beSome[ Byte ]
          cache.get[ Byte ]( s"$prefix-type.byte" ) must beSome( 0xAB.toByte )
        }

        "support a char" in {
          cache.set( s"$prefix-type.char.1", 'a' )
          cache.get[ Char ]( s"$prefix-type.char.1" ) must beSome[ Char ]
          cache.get[ Char ]( s"$prefix-type.char.1" ) must beSome( 'a' )
          cache.set( s"$prefix-type.char.2", 'b' )
          cache.get[ Char ]( s"$prefix-type.char.2" ) must beSome( 'b' )
          cache.set( s"$prefix-type.char.3", 'č' )
          cache.get[ Char ]( s"$prefix-type.char.3" ) must beSome( 'č' )
        }

        "support a short" in {
          cache.set( s"$prefix-type.short", 12.toShort )
          cache.get[ Short ]( s"$prefix-type.short" ) must beSome[ Short ]
          cache.get[ Short ]( s"$prefix-type.short" ) must beSome( 12.toShort )
        }

        "support an int" in {
          cache.set( s"$prefix-type.int", 15 )
          cache.get[ Int ]( s"$prefix-type.int" ) must beSome( 15 )
        }

        "support a long" in {
          cache.set( s"$prefix-type.long", 144L )
          cache.get[ Long ]( s"$prefix-type.long" ) must beSome[ Long ]
          cache.get[ Long ]( s"$prefix-type.long" ) must beSome( 144L )
        }

        "support a float" in {
          cache.set( s"$prefix-type.float", 1.23f )
          cache.get[ Float ]( s"$prefix-type.float" ) must beSome[ Float ]
          cache.get[ Float ]( s"$prefix-type.float" ) must beSome( 1.23f )
        }

        "support a double" in {
          cache.set( s"$prefix-type.double", 3.14 )
          cache.get[ Double ]( s"$prefix-type.double" ) must beSome[ Double ]
          cache.get[ Double ]( s"$prefix-type.double" ) must beSome( 3.14 )
        }

        "support a date" in {
          cache.set( s"$prefix-type.date", new Date( 123 ) )
          cache.get[ Date ]( s"$prefix-type.date" ) must beSome( new Date( 123 ) )
        }

        "support a datetime" in {
          cache.set( s"$prefix-type.datetime", new DateTime( 123456 ) )
          cache.get[ DateTime ]( s"$prefix-type.datetime" ) must beSome( new DateTime( 123456 ) )
        }

        "support a custom classes" in {
          cache.set( s"$prefix-type.object", SimpleObject( "B", 3 ) )
          cache.get[ SimpleObject ]( s"$prefix-type.object" ) must beSome( SimpleObject( "B", 3 ) )
        }

        "support a null" in {
          cache.set( s"$prefix-type.null", null )
          cache.get[ SimpleObject ]( s"$prefix-type.null" ) must beNone
        }

        "remove multiple keys at once" in {
          cache.set( s"$prefix-test-remove-multiple-1", "value" )
          cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must beSome[ Any ]
          cache.set( s"$prefix-test-remove-multiple-2", "value" )
          cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must beSome[ Any ]
          cache.set( s"$prefix-test-remove-multiple-3", "value" )
          cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must beSome[ Any ]
          cache.remove( s"$prefix-test-remove-multiple-1", s"$prefix-test-remove-multiple-2", s"$prefix-test-remove-multiple-3" )
          cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must beNone
          cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must beNone
          cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must beNone
        }

        "remove in batch" in {
          cache.set( s"$prefix-test-remove-batch-1", "value" )
          cache.get[ String ]( s"$prefix-test-remove-batch-1" ) must beSome[ Any ]
          cache.set( s"$prefix-test-remove-batch-2", "value" )
          cache.get[ String ]( s"$prefix-test-remove-batch-2" ) must beSome[ Any ]
          cache.set( s"$prefix-test-remove-batch-3", "value" )
          cache.get[ String ]( s"$prefix-test-remove-batch-3" ) must beSome[ Any ]
          cache.removeAll( Seq( s"$prefix-test-remove-batch-1", s"$prefix-test-remove-batch-2", s"$prefix-test-remove-batch-3" ): _* )
          cache.get[ String ]( s"$prefix-test-remove-batch-1" ) must beNone
          cache.get[ String ]( s"$prefix-test-remove-batch-2" ) must beNone
          cache.get[ String ]( s"$prefix-test-remove-batch-3" ) must beNone
        }
      }
    }
  }

  trait Expectation {
    def expects( success: => Matcher[ Any ], default: => Matcher[ Any ] ): Matcher[ Any ]
  }

  object AlwaysDefault extends Expectation {
    override def expects( success: => Matcher[ Any ], default: => Matcher[ Any ] ): Matcher[ Any ] = default
  }

  object AlwaysSuccess extends Expectation {
    override def expects( success: => Matcher[ Any ], default: => Matcher[ Any ] ): Matcher[ Any ] = success
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
