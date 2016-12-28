package play.api.cache.redis.impl

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.cache.redis.{CacheApi, Redis, SimpleObject}

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class SynchronousCacheSpec extends Specification with Redis {

  private type Cache = CacheApi

  private val Cache = injector.instanceOf[ Cache ]

  val prefix = "sync"

  "SynchronousCacheApi" should {

    "miss on get" in {
      Cache.get[ String ]( s"$prefix-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( s"$prefix-test-2", "value" )
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome[ Any ]
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome( "value" )
    }

    "expire refreshes expiration" in {
      Cache.set( s"$prefix-test-10", "value", 2.second )
      Cache.get[ String ]( s"$prefix-test-10" ) must beSome( "value" )
      Cache.expire( s"$prefix-test-10", 1.minute )
      // wait until the first duration expires
      Thread.sleep( 3000 )
      Cache.get[ String ]( s"$prefix-test-10" ) must beSome( "value" )
    }

    "positive exists on existing keys" in {
      Cache.set( s"$prefix-test-11", "value" )
      Cache.exists( s"$prefix-test-11" ) must beTrue
    }

    "negative exists on expired and missing keys" in {
      Cache.set( s"$prefix-test-12A", "value", 1.second )
      // wait until the duration expires
      Thread.sleep( 2000 )
      Cache.exists( s"$prefix-test-12A" ) must beFalse
      Cache.exists( s"$prefix-test-12B" ) must beFalse
    }

    "miss after remove" in {
      Cache.set( s"$prefix-test-3", "value" )
      Cache.get[ String ]( s"$prefix-test-3" ) must beSome[ Any ]
      Cache.remove( s"$prefix-test-3" )
      Cache.get[ String ]( s"$prefix-test-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      Cache.set( s"$prefix-test-4", "value", 1.second )
      Cache.get[ String ]( s"$prefix-test-4" ) must beSome[ Any ]
      // wait until it expires
      Thread.sleep( 1500 )
      // miss
      Cache.get[ String ]( s"$prefix-test-4" ) must beNone
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

    "perform future and store result" in {
      val counter = new AtomicInteger( 0 )
      // perform test
      for ( index <- 1 to 5 ) Cache.getOrFutureCounting( s"$prefix-test-8" )( counter ).sync mustEqual "value"
      // verify
      counter.get must beEqualTo( 1 )
    }

    "propagate fail in future" in {
      Cache.getOrFuture[ String ]( s"$prefix-test-9" ){
        Future.failed( new IllegalStateException( "Exception in test." ) )
      }.sync must throwA( new IllegalStateException( "Exception in test." ) )
    }

    "support list" in {
      // store value
      Cache.set( s"$prefix-list", List( "A", "B", "C" ) )
      // recall
      Cache.get[ List[ String ] ]( s"$prefix-list" ) must beSome[ List[ String ] ]( List( "A", "B", "C" ) )
    }

    "support a byte" in {
      Cache.set( s"$prefix-type.byte", 0xAB.toByte )
      Cache.get[ Byte ]( s"$prefix-type.byte" ) must beSome[ Byte ]
      Cache.get[ Byte ]( s"$prefix-type.byte" ) must beSome( 0xAB.toByte )
    }

    "support a char" in {
      Cache.set( s"$prefix-type.char.1", 'a' )
      Cache.get[ Char ]( s"$prefix-type.char.1" ) must beSome[ Char ]
      Cache.get[ Char ]( s"$prefix-type.char.1" ) must beSome( 'a' )
      Cache.set( s"$prefix-type.char.2", 'b' )
      Cache.get[ Char ]( s"$prefix-type.char.2" ) must beSome( 'b' )
      Cache.set( s"$prefix-type.char.3", 'č' )
      Cache.get[ Char ]( s"$prefix-type.char.3" ) must beSome( 'č' )
    }

    "support a short" in {
      Cache.set( s"$prefix-type.short", 12.toShort )
      Cache.get[ Short ]( s"$prefix-type.short" ) must beSome[ Short ]
      Cache.get[ Short ]( s"$prefix-type.short" ) must beSome( 12.toShort )
    }

    "support an int" in {
      Cache.set( s"$prefix-type.int", 15 )
      Cache.get[ Int ]( s"$prefix-type.int" ) must beSome( 15 )
    }

    "support a long" in {
      Cache.set( s"$prefix-type.long", 144L )
      Cache.get[ Long ]( s"$prefix-type.long" ) must beSome[ Long ]
      Cache.get[ Long ]( s"$prefix-type.long" ) must beSome( 144L )
    }

    "support a float" in {
      Cache.set( s"$prefix-type.float", 1.23f )
      Cache.get[ Float ]( s"$prefix-type.float" ) must beSome[ Float ]
      Cache.get[ Float ]( s"$prefix-type.float" ) must beSome( 1.23f )
    }

    "support a double" in {
      Cache.set( s"$prefix-type.double", 3.14 )
      Cache.get[ Double ]( s"$prefix-type.double" ) must beSome[ Double ]
      Cache.get[ Double ]( s"$prefix-type.double" ) must beSome( 3.14 )
    }

    "support a date" in {
      Cache.set( s"$prefix-type.date", new Date( 123 ) )
      Cache.get[ Date ]( s"$prefix-type.date" ) must beSome( new Date( 123 ) )
    }

    "support a datetime" in {
      Cache.set( s"$prefix-type.datetime", new DateTime( 123456 ) )
      Cache.get[ DateTime ]( s"$prefix-type.datetime" ) must beSome( new DateTime( 123456 ) )
    }

    "support a custom classes" in {
      Cache.set( s"$prefix-type.object", SimpleObject( "B", 3 ) )
      Cache.get[ SimpleObject ]( s"$prefix-type.object" ) must beSome( SimpleObject( "B", 3 ) )
    }

    "support a null" in {
      Cache.set( s"$prefix-type.null", null )
      Cache.get[ SimpleObject ]( s"$prefix-type.null" ) must beNone
    }

    "remove multiple keys at once" in {
      Cache.set( s"$prefix-test-remove-multiple-1", "value" )
      Cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must beSome[ Any ]
      Cache.set( s"$prefix-test-remove-multiple-2", "value" )
      Cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must beSome[ Any ]
      Cache.set( s"$prefix-test-remove-multiple-3", "value" )
      Cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must beSome[ Any ]
      Cache.remove( s"$prefix-test-remove-multiple-1", s"$prefix-test-remove-multiple-2", s"$prefix-test-remove-multiple-3" )
      Cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must beNone
      Cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must beNone
      Cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must beNone
    }
  }
}
