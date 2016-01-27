package play.api.cache.redis

import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class JavaCacheSpec extends Specification with Redis {

  private type Cache = play.cache.CacheApi

  private val Cache = application.injector.instanceOf[ play.cache.CacheApi ]

  "Cache" should {

    "miss on get" in {
      Cache.get[ String ]( "java-test-1" ) must beNull
    }

    "hit after set" in {
      Cache.set( "java-test-2", "value" )
      Cache.get[ String ]( "java-test-2" ) mustEqual "value"
    }

    "miss after remove" in {
      Cache.set( "java-test-3", "value" )
      Cache.get[ String ]( "java-test-3" ) mustEqual "value"
      Cache.remove( "java-test-3" )
      Cache.get[ String ]( "java-test-3" ) must beNull
    }

    "miss after timeout" in {
      // set
      Cache.set( "java-test-4", "value", 1 )
      Cache.get[ String ]( "java-test-4" ) mustEqual "value"
      // wait until it expires
      Thread.sleep( 1500 )
      // miss
      Cache.get[ String ]( "java-test-4" ) must beNull
    }

    "miss at first getOrElse " in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( "java-test-5" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "hit at second getOrElse" in {
      val counter = new AtomicInteger( 0 )
      for ( index <- 1 to 10 ) Cache.getOrElseCounting( "java-test-6" )( counter ) mustEqual "value"
      counter.get mustEqual 1
    }

    "distinct different keys" in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( "java-test-7A" )( counter ) mustEqual "value"
      Cache.getOrElseCounting( "java-test-7B" )( counter ) mustEqual "value"
      counter.get mustEqual 2
    }

    "support list" in {
      // store value
      Cache.set( "java-list", List( "A", "B", "C" ) )
      // recall
      Cache.get[ List[ String ] ]( "java-list" ) mustEqual List( "A", "B", "C" )
    }

    "support a byte" in {
      Cache.set( "java-type.byte", 0xAB.toByte )
      Cache.get[ Byte ]( "java-type.byte" ) mustEqual 0xAB.toByte
    }

    "support a char" in {
      Cache.set( "java-type.char.1", 'a' )
      Cache.get[ Char ]( "java-type.char.1" ) mustEqual 'a'
      Cache.set( "java-type.char.2", 'b' )
      Cache.get[ Char ]( "java-type.char.2" ) mustEqual 'b'
      Cache.set( "java-type.char.3", 'č' )
      Cache.get[ Char ]( "java-type.char.3" ) mustEqual 'č'
    }

    "support a short" in {
      Cache.set( "java-type.short", 12.toShort )
      Cache.get[ Short ]( "java-type.short" ) mustEqual 12.toShort
    }

    "support an int" in {
      Cache.set( "java-type.int", 0xAB.toByte )
      Cache.get[ Byte ]( "java-type.int" ) mustEqual 0xAB.toByte
    }

    "support a long" in {
      Cache.set( "java-type.long", 144L )
      Cache.get[ Long ]( "java-type.long" ) mustEqual 144L
    }

    "support a float" in {
      Cache.set( "java-type.float", 1.23f )
      Cache.get[ Float ]( "java-type.float" ) mustEqual 1.23f
    }

    "support a double" in {
      Cache.set( "java-type.double", 3.14 )
      Cache.get[ Double ]( "java-type.double" ) mustEqual 3.14
    }

    "support a date" in {
      Cache.set( "java-type.date", new Date( 123 ) )
      Cache.get[ Date ]( "java-type.date" ) mustEqual new Date( 123 )
    }

    "support a datetime" in {
      Cache.set( "java-type.datetime", new DateTime( 123456 ) )
      Cache.get[ DateTime ]( "java-type.datetime" ) mustEqual new DateTime( 123456 )
    }

    "support a custom classes" in {
      Cache.set( "java-type.object", SimpleObject( "B", 3 ) )
      Cache.get[ SimpleObject ]( "java-type.object" ) mustEqual SimpleObject( "B", 3 )
    }

    "support a null" in {
      Cache.set( "java-type.null", null )
      Cache.get[ SimpleObject ]( "java-type.null" ) must beNull
    }
  }


  implicit class RichCache( cache: Cache ) {
    private type Accumulator = AtomicInteger

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrElseCounting( key: String )( accumulator: Accumulator ) = cache.getOrElse[ String ]( key, new Callable[String] {
      override def call(): String = {
        // increment miss counter
        accumulator.incrementAndGet( )
        // return the value to store into the cache
        "value"
      }
    } )
  }

}
