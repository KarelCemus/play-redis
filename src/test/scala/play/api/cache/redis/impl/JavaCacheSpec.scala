package play.api.cache.redis.impl

import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

import play.api.{Configuration, Environment}
import play.api.cache.redis.{Redis, SimpleObject}
import play.core.j.JavaHelpers
import play.mvc.Http
import play.test.Helpers

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class JavaCacheSpec extends Specification with Redis {

  private type Cache = play.cache.SyncCacheApi

  private val Cache = injector.instanceOf[ play.cache.SyncCacheApi ]

  private val configuration = injector.instanceOf[ Configuration ]
  private val environment = injector.instanceOf[ Environment ]

  private val prefix = "java"

  "play.cache.CacheApi" should {

    "miss on get" in {
      Cache.get[ String ]( s"$prefix-test-1" ) must beNull
    }

    "hit after set" in {
      Cache.set( s"$prefix-test-2", "value" )
      Cache.get[ String ]( s"$prefix-test-2" ) mustEqual "value"
    }

    "miss after remove" in {
      Cache.set( s"$prefix-test-3", "value" )
      Cache.get[ String ]( s"$prefix-test-3" ) mustEqual "value"
      Cache.remove( s"$prefix-test-3" )
      Cache.get[ String ]( s"$prefix-test-3" ) must beNull
    }

    "miss after timeout" in {
      // set
      Cache.set( s"$prefix-test-4", "value", 1 )
      Cache.get[ String ]( s"$prefix-test-4" ) mustEqual "value"
      // wait until it expires
      Thread.sleep( 1500 )
      // miss
      Cache.get[ String ]( s"$prefix-test-4" ) must beNull
    }

    "miss at first getOrElse " in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( s"$prefix-test-5" )( counter ) mustEqual "value"
      counter.get must beEqualTo( 1 )
    }

    "hit at second getOrElse" in {
      val counter = new AtomicInteger( 0 )
      for ( index <- 1 to 10 ) Cache.getOrElseCounting( s"$prefix-test-6" )( counter ) mustEqual "value"
      counter.get mustEqual 1
    }

    "getOrElseUpdate" in {
      Cache.get[ String ]( s"$prefix-test-getOrElseUpdate" ) must beNull
      val orElse = new Callable[ String ] { def call( ) = "value" }
      Cache.getOrElseUpdate[ String ]( s"$prefix-test-getOrElseUpdate", orElse ) mustEqual "value"
      Cache.get[ String ]( s"$prefix-test-getOrElseUpdate" ) mustEqual "value"
    }

    "getOrElseUpdate uses HttpContext" in {
      Cache.get[ String ]( s"$prefix-test-getOrElseUpdate-2" ) must beNull
      val request = Helpers.fakeRequest().path( "request-path" ).build()
      val context = new Http.Context( request, JavaHelpers.createContextComponents( configuration, environment ) )
      Http.Context.current.set( context )
      val orElse = new Callable[ String ] {
        def call( ) = Http.Context.current().request().path()
      }
      Http.Context.current().request().path() mustEqual "request-path"
      Cache.getOrElseUpdate[ String ]( s"$prefix-test-getOrElseUpdate-2", orElse ) mustEqual "request-path"
      Cache.get[ String ]( s"$prefix-test-getOrElseUpdate-2" ) mustEqual "request-path"
    }

    "distinct different keys" in {
      val counter = new AtomicInteger( 0 )
      Cache.getOrElseCounting( s"$prefix-test-7A" )( counter ) mustEqual "value"
      Cache.getOrElseCounting( s"$prefix-test-7B" )( counter ) mustEqual "value"
      counter.get mustEqual 2
    }

    "support list" in {
      // store value
      Cache.set( s"$prefix-list", List( "A", "B", "C" ) )
      // recall
      Cache.get[ List[ String ] ]( s"$prefix-list" ) mustEqual List( "A", "B", "C" )
    }

    "support a byte" in {
      Cache.set( s"$prefix-type.byte", 0xAB.toByte )
      Cache.get[ Byte ]( s"$prefix-type.byte" ) mustEqual 0xAB.toByte
    }

    "support a char" in {
      Cache.set( s"$prefix-type.char.1", 'a' )
      Cache.get[ Char ]( s"$prefix-type.char.1" ) mustEqual 'a'
      Cache.set( s"$prefix-type.char.2", 'b' )
      Cache.get[ Char ]( s"$prefix-type.char.2" ) mustEqual 'b'
      Cache.set( s"$prefix-type.char.3", 'č' )
      Cache.get[ Char ]( s"$prefix-type.char.3" ) mustEqual 'č'
    }

    "support a short" in {
      Cache.set( s"$prefix-type.short", 12.toShort )
      Cache.get[ Short ]( s"$prefix-type.short" ) mustEqual 12.toShort
    }

    "support an int" in {
      Cache.set( s"$prefix-type.int", 0xAB.toByte )
      Cache.get[ Byte ]( s"$prefix-type.int" ) mustEqual 0xAB.toByte
    }

    "support a long" in {
      Cache.set( s"$prefix-type.long", 144L )
      Cache.get[ Long ]( s"$prefix-type.long" ) mustEqual 144L
    }

    "support a float" in {
      Cache.set( s"$prefix-type.float", 1.23f )
      Cache.get[ Float ]( s"$prefix-type.float" ) mustEqual 1.23f
    }

    "support a double" in {
      Cache.set( s"$prefix-type.double", 3.14 )
      Cache.get[ Double ]( s"$prefix-type.double" ) mustEqual 3.14
    }

    "support a date" in {
      Cache.set( s"$prefix-type.date", new Date( 123 ) )
      Cache.get[ Date ]( s"$prefix-type.date" ) mustEqual new Date( 123 )
    }

    "support a datetime" in {
      Cache.set( s"$prefix-type.datetime", new DateTime( 123456 ) )
      Cache.get[ DateTime ]( s"$prefix-type.datetime" ) mustEqual new DateTime( 123456 )
    }

    "support a custom classes" in {
      Cache.set( s"$prefix-type.object", SimpleObject( "B", 3 ) )
      Cache.get[ SimpleObject ]( s"$prefix-type.object" ) mustEqual SimpleObject( "B", 3 )
    }

    "support a null" in {
      Cache.set( s"$prefix-type.null", null )
      Cache.get[ SimpleObject ]( s"$prefix-type.null" ) must beNull
    }
  }
}
