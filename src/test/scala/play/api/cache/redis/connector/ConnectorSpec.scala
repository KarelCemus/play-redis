package play.api.cache.redis.connector

import java.util.Date

import scala.concurrent.duration._

import play.api.cache.redis.{Redis, SimpleObject}

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class ConnectorSpec extends Specification with Redis {

  private type Cache = RedisConnector

  private val Cache = injector.instanceOf[ Cache ]

  "Cache" should {

    "miss on get" in {
      Cache.get[ String ]( "connector-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( "connector-test-2", "value" ).sync
      Cache.get[ String ]( "connector-test-2" ) must beSome[ Any ]
      Cache.get[ String ]( "connector-test-2" ) must beSome( "value" )
    }

    "expire refreshes expiration" in {
      Cache.set( "connector-test-10", "value", 2.second ).sync
      Cache.get[ String ]( "connector-test-10" ) must beSome( "value" )
      Cache.expire( "connector-test-10", 1.minute ).sync
      // wait until the first duration expires
      Thread.sleep( 3000 )
      Cache.get[ String ]( "connector-test-10" ) must beSome( "value" )
    }

    "positive exists on existing keys" in {
      Cache.set( "connector-test-11", "value" ).sync
      Cache.exists( "connector-test-11" ) must beTrue
    }

    "negative exists on expired and missing keys" in {
      Cache.set( "connector-test-12A", "value", 1.second ).sync
      // wait until the duration expires
      Thread.sleep( 2000 )
      Cache.exists( "connector-test-12A" ) must beFalse
      Cache.exists( "connector-test-12B" ) must beFalse
    }

    "miss after remove" in {
      Cache.set( "connector-test-3", "value" ).sync
      Cache.get[ String ]( "connector-test-3" ) must beSome[ Any ]
      Cache.remove( "connector-test-3" ).sync
      Cache.get[ String ]( "connector-test-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      Cache.set( "connector-test-4", "value", 1.second ).sync
      Cache.get[ String ]( "connector-test-4" ) must beSome[ Any ]
      // wait until it expires
      Thread.sleep( 1500 )
      // miss
      Cache.get[ String ]( "connector-test-4" ) must beNone
    }

    "find all matching keys" in {
      Cache.set( "connector-test-13-key-A", "value", 3.second ).sync
      Cache.set( "connector-test-13-note-A", "value", 3.second ).sync
      Cache.set( "connector-test-13-key-B", "value", 3.second ).sync
      Cache.matching( "connector-test-13*" ).sync mustEqual Set( "connector-test-13-key-A", "connector-test-13-note-A", "connector-test-13-key-B" )
      Cache.matching( "connector-test-13*A" ).sync mustEqual Set( "connector-test-13-key-A", "connector-test-13-note-A" )
      Cache.matching( "connector-test-13-key-*" ).sync mustEqual Set( "connector-test-13-key-A", "connector-test-13-key-B" )
      Cache.matching( "connector-test-13A*" ).sync mustEqual Set.empty
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
      Cache.set( "type.int", 15 ).sync
      Cache.get[ Int ]( "type.int" ) must beSome( 15 )
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
      Cache.set( "connector-test-remove-multiple-1", "value" ).sync
      Cache.get[ String ]( "connector-test-remove-multiple-1" ) must beSome[ Any ]
      Cache.set( "connector-test-remove-multiple-2", "value" ).sync
      Cache.get[ String ]( "connector-test-remove-multiple-2" ) must beSome[ Any ]
      Cache.set( "connector-test-remove-multiple-3", "value" ).sync
      Cache.get[ String ]( "connector-test-remove-multiple-3" ) must beSome[ Any ]
      Cache.remove( "connector-test-remove-multiple-1", "connector-test-remove-multiple-2", "connector-test-remove-multiple-3" ).sync
      Cache.get[ String ]( "connector-test-remove-multiple-1" ) must beNone
      Cache.get[ String ]( "connector-test-remove-multiple-2" ) must beNone
      Cache.get[ String ]( "connector-test-remove-multiple-3" ) must beNone
    }

    "remove in batch" in {
      Cache.set( "connector-test-remove-batch-1", "value" ).sync
      Cache.get[ String ]( "connector-test-remove-batch-1" ) must beSome[ Any ]
      Cache.set( "connector-test-remove-batch-2", "value" ).sync
      Cache.get[ String ]( "connector-test-remove-batch-2" ) must beSome[ Any ]
      Cache.set( "connector-test-remove-batch-3", "value" ).sync
      Cache.get[ String ]( "connector-test-remove-batch-3" ) must beSome[ Any ]
      Cache.remove( "connector-test-remove-batch-1", "connector-test-remove-batch-2", "connector-test-remove-batch-3" ).sync
      Cache.get[ String ]( "connector-test-remove-batch-1" ) must beNone
      Cache.get[ String ]( "connector-test-remove-batch-2" ) must beNone
      Cache.get[ String ]( "connector-test-remove-batch-3" ) must beNone
    }
  }

}
