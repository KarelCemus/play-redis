package play.cache.redis

import java.util.Date

import play.api.Play.current
import play.api.cache.Cache
import play.cache.{RedisCacheSupport, SimpleObject}

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class RedisCacheAdapterSpec extends Specification with RedisCacheSupport {

  "Cache Adapter" should {

    "miss on get" in {
      Cache.get( "sync-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( "sync-test-2", "value" )
      Cache.get( "sync-test-2" ) must beSome[ Any ]
      Cache.get( "sync-test-2" ) must beSome( "value" )
    }

    "miss after remove" in {
      Cache.set( "sync-test-3", "value" )
      Cache.get( "sync-test-3" ).isDefined must beTrue
      Cache.remove( "sync-test-3" )
      Cache.get( "sync-test-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      Cache.set( "sync-test-4", "value", 1 )
      Cache.get( "sync-test-4" ).isDefined must beTrue
      // wait until it expires
      Thread.sleep( 2000 )
      // miss
      Cache.get( "sync-test-4" ) must beNone
    }

    "support Int" in {
      Cache.set( "test-int-key", 123 )
      Cache.get( "test-int-key" ) must beSome( 123 )
    }

    "support Long" in {
      Cache.set( "test-long-key", 1234L )
      Cache.get( "test-long-key" ) must beSome( 1234L )
    }

    "support Date" in {
      Cache.set( "test-date-key", new Date( 123 ) )
      Cache.get( "test-date-key" ) must beSome( new Date( 123 ) )
    }

    "support DateTime" in {
      Cache.set( "test-datetime-key", new DateTime( 123 ) )
      Cache.get( "test-datetime-key" ) must beSome( new DateTime( 123 ) )
    }

    "support Boolean" in {
      Cache.set( "test-boolean-key", true )
      Cache.get( "test-boolean-key" ) must beSome( true )
    }

    "support custom objects" in {
      Cache.set( "test-object-key", SimpleObject( "A", 5 ) )
      Cache.get( "test-object-key" ) must beSome( SimpleObject( "A", 5 ) )
    }
  }
}
