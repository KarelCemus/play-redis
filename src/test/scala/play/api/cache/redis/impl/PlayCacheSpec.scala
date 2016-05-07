package play.api.cache.redis.impl

import java.util.concurrent.atomic.AtomicInteger

import play.api.cache.redis.Redis

import org.specs2.mutable.Specification

/**
 * <p>Test of play.api.cache.CacheApi to verify compatibility with Redis implementation.</p>
 */
class PlayCacheSpec extends Specification with Redis {

  private type Cache = play.api.cache.CacheApi

  private val Cache = injector.instanceOf[ Cache ]
  
  private val prefix = "play"
  
  "play.api.cache.CacheApi" should {

    "miss on get" in {
      Cache.get[ String ]( s"$prefix-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( s"$prefix-test-2", "value" )
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome[ Any ]
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome( "value" )
    }

    "miss at first getOrElse " in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( s"$prefix-test-5" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "hit at second getOrElse" in {
      val counter = new AtomicInteger( 0 )
      for ( index <- 1 to 10 ) Cache.getOrElseCounting( s"$prefix-test-6" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "distinct different keys" in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( s"$prefix-test-7A" )( counter ) mustEqual "value"
      Cache.getOrElseCounting( s"$prefix-test-7B" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 2 )
    }

    "miss after remove" in {
      Cache.set( s"$prefix-test-3", "value" )
      Cache.get[ String ]( s"$prefix-test-3" ) must beSome[ Any ]
      Cache.remove( s"$prefix-test-3" )
      Cache.get[ String ]( s"$prefix-test-3" ) must beNone
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
  }
}
