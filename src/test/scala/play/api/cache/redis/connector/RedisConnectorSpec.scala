package play.api.cache.redis.connector

import java.util.Date

import scala.concurrent.duration._

import play.api.cache.redis._
import play.api.cache.redis.exception.ExecutionFailedException

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
  * <p>Specification of the low level connector implementing basic commands</p>
  */
class RedisConnectorSpec extends Specification with Redis {

  private type Cache = RedisConnector

  private val Cache = injector.instanceOf[ Cache ]

  private val prefix = "connector"

  "RedisConnector" should {

    "miss on get" in {
      Cache.get[ String ]( s"$prefix-test-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( s"$prefix-test-2", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome[ Any ]
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome( "value" )
    }

    "ignore set if not exists when already defined" in {
      Cache.set( s"$prefix-test-if-not-exists-when-exists", "previous" ).sync
      Cache.setIfNotExists( s"$prefix-test-if-not-exists-when-exists", "value" ) must beFalse
      Cache.get[ String ]( s"$prefix-test-if-not-exists-when-exists" ) must beSome[ Any ]
      Cache.get[ String ]( s"$prefix-test-if-not-exists-when-exists" ) must beSome( "previous" )
    }

    "perform set if not exists when undefined" in {
      Cache.setIfNotExists( s"$prefix-test-if-not-exists", "value" ) must beTrue
      Cache.get[ String ]( s"$prefix-test-if-not-exists" ) must beSome[ Any ]
      Cache.get[ String ]( s"$prefix-test-if-not-exists" ) must beSome( "value" )
    }

    "expire refreshes expiration" in {
      Cache.set( s"$prefix-test-10", "value", 2.second ).sync
      Cache.get[ String ]( s"$prefix-test-10" ) must beSome( "value" )
      Cache.expire( s"$prefix-test-10", 1.minute ).sync
      // wait until the first duration expires
      Thread.sleep( 3000 )
      Cache.get[ String ]( s"$prefix-test-10" ) must beSome( "value" )
    }

    "positive exists on existing keys" in {
      Cache.set( s"$prefix-test-11", "value" ).sync
      Cache.exists( s"$prefix-test-11" ) must beTrue
    }

    "negative exists on expired and missing keys" in {
      Cache.set( s"$prefix-test-12A", "value", 1.second ).sync
      // wait until the duration expires
      Thread.sleep( 2000 )
      Cache.exists( s"$prefix-test-12A" ) must beFalse
      Cache.exists( s"$prefix-test-12B" ) must beFalse
    }

    "miss after remove" in {
      Cache.set( s"$prefix-test-3", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-3" ) must beSome[ Any ]
      Cache.remove( s"$prefix-test-3" ).sync
      Cache.get[ String ]( s"$prefix-test-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      Cache.set( s"$prefix-test-4", "value", 1.second ).sync
      Cache.get[ String ]( s"$prefix-test-4" ) must beSome[ Any ]
      // wait until it expires
      Thread.sleep( 1500 )
      // miss
      Cache.get[ String ]( s"$prefix-test-4" ) must beNone
    }

    "find all matching keys" in {
      Cache.set( s"$prefix-test-13-key-A", "value", 3.second ).sync
      Cache.set( s"$prefix-test-13-note-A", "value", 3.second ).sync
      Cache.set( s"$prefix-test-13-key-B", "value", 3.second ).sync
      Cache.matching( s"$prefix-test-13*" ).sync mustEqual Set( s"$prefix-test-13-key-A", s"$prefix-test-13-note-A", s"$prefix-test-13-key-B" )
      Cache.matching( s"$prefix-test-13*A" ).sync mustEqual Set( s"$prefix-test-13-key-A", s"$prefix-test-13-note-A" )
      Cache.matching( s"$prefix-test-13-key-*" ).sync mustEqual Set( s"$prefix-test-13-key-A", s"$prefix-test-13-key-B" )
      Cache.matching( s"$prefix-test-13A*" ).sync mustEqual Set.empty
    }

    "support list" in {
      // store value
      Cache.set( s"$prefix-list", List( "A", "B", "C" ) ).sync
      // recall
      Cache.get[ List[ String ] ]( s"$prefix-list" ) must beSome[ List[ String ] ]( List( "A", "B", "C" ) )
    }

    "support a byte" in {
      Cache.set( s"$prefix-type.byte", 0xAB.toByte ).sync
      Cache.get[ Byte ]( s"$prefix-type.byte" ) must beSome[ Byte ]
      Cache.get[ Byte ]( s"$prefix-type.byte" ) must beSome( 0xAB.toByte )
    }

    "support a char" in {
      Cache.set( s"$prefix-type.char.1", 'a' ).sync
      Cache.get[ Char ]( s"$prefix-type.char.1" ) must beSome[ Char ]
      Cache.get[ Char ]( s"$prefix-type.char.1" ) must beSome( 'a' )
      Cache.set( s"$prefix-type.char.2", 'b' ).sync
      Cache.get[ Char ]( s"$prefix-type.char.2" ) must beSome( 'b' )
      Cache.set( s"$prefix-type.char.3", 'č' ).sync
      Cache.get[ Char ]( s"$prefix-type.char.3" ) must beSome( 'č' )
    }

    "support a short" in {
      Cache.set( s"$prefix-type.short", 12.toShort ).sync
      Cache.get[ Short ]( s"$prefix-type.short" ) must beSome[ Short ]
      Cache.get[ Short ]( s"$prefix-type.short" ) must beSome( 12.toShort )
    }

    "support an int" in {
      Cache.set( s"$prefix-type.int", 15 ).sync
      Cache.get[ Int ]( s"$prefix-type.int" ) must beSome( 15 )
    }

    "support a long" in {
      Cache.set( s"$prefix-type.long", 144L ).sync
      Cache.get[ Long ]( s"$prefix-type.long" ) must beSome[ Long ]
      Cache.get[ Long ]( s"$prefix-type.long" ) must beSome( 144L )
    }

    "support a float" in {
      Cache.set( s"$prefix-type.float", 1.23f ).sync
      Cache.get[ Float ]( s"$prefix-type.float" ) must beSome[ Float ]
      Cache.get[ Float ]( s"$prefix-type.float" ) must beSome( 1.23f )
    }

    "support a double" in {
      Cache.set( s"$prefix-type.double", 3.14 ).sync
      Cache.get[ Double ]( s"$prefix-type.double" ) must beSome[ Double ]
      Cache.get[ Double ]( s"$prefix-type.double" ) must beSome( 3.14 )
    }

    "support a date" in {
      Cache.set( s"$prefix-type.date", new Date( 123 ) ).sync
      Cache.get[ Date ]( s"$prefix-type.date" ) must beSome( new Date( 123 ) )
    }

    "support a datetime" in {
      Cache.set( s"$prefix-type.datetime", new DateTime( 123456 ) ).sync
      Cache.get[ DateTime ]( s"$prefix-type.datetime" ) must beSome( new DateTime( 123456 ) )
    }

    "support a custom classes" in {
      Cache.set( s"$prefix-type.object", SimpleObject( "B", 3 ) ).sync
      Cache.get[ SimpleObject ]( s"$prefix-type.object" ) must beSome( SimpleObject( "B", 3 ) )
    }

    "support a null" in {
      Cache.set( s"$prefix-type.null", null ).sync
      Cache.get[ SimpleObject ]( s"$prefix-type.null" ) must beNone
    }

    "remove multiple keys at once" in {
      Cache.set( s"$prefix-test-remove-multiple-1", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must beSome[ Any ]
      Cache.set( s"$prefix-test-remove-multiple-2", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must beSome[ Any ]
      Cache.set( s"$prefix-test-remove-multiple-3", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must beSome[ Any ]
      Cache.remove( s"$prefix-test-remove-multiple-1", s"$prefix-test-remove-multiple-2", s"$prefix-test-remove-multiple-3" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must beNone
      Cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must beNone
      Cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must beNone
    }

    "remove in batch" in {
      Cache.set( s"$prefix-test-remove-batch-1", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-batch-1" ) must beSome[ Any ]
      Cache.set( s"$prefix-test-remove-batch-2", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-batch-2" ) must beSome[ Any ]
      Cache.set( s"$prefix-test-remove-batch-3", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-batch-3" ) must beSome[ Any ]
      Cache.remove( s"$prefix-test-remove-batch-1", s"$prefix-test-remove-batch-2", s"$prefix-test-remove-batch-3" ).sync
      Cache.get[ String ]( s"$prefix-test-remove-batch-1" ) must beNone
      Cache.get[ String ]( s"$prefix-test-remove-batch-2" ) must beNone
      Cache.get[ String ]( s"$prefix-test-remove-batch-3" ) must beNone
    }

    "set a zero when not exists and then increment" in {
      Cache.increment( s"$prefix-test-incr-null", 1 ).sync must beEqualTo( 1 )
    }

    "throw an exception when not integer" in {
      Cache.set( s"$prefix-test-incr-string", "value" ).sync
      Cache.increment( s"$prefix-test-incr-string", 1 ).sync must throwA[ ExecutionFailedException ]
    }

    "increment by one" in {
      Cache.set( s"$prefix-test-incr-by-one", 5 ).sync
      Cache.increment( s"$prefix-test-incr-by-one", 1 ).sync must beEqualTo( 6 )
      Cache.increment( s"$prefix-test-incr-by-one", 1 ).sync must beEqualTo( 7 )
      Cache.increment( s"$prefix-test-incr-by-one", 1 ).sync must beEqualTo( 8 )
    }

    "increment by some" in {
      Cache.set( s"$prefix-test-incr-by-some", 5 ).sync
      Cache.increment( s"$prefix-test-incr-by-some", 1 ).sync must beEqualTo( 6 )
      Cache.increment( s"$prefix-test-incr-by-some", 2 ).sync must beEqualTo( 8 )
      Cache.increment( s"$prefix-test-incr-by-some", 3 ).sync must beEqualTo( 11 )
    }

    "decrement by one" in {
      Cache.set( s"$prefix-test-decr-by-one", 5 ).sync
      Cache.increment( s"$prefix-test-decr-by-one", -1 ).sync must beEqualTo( 4 )
      Cache.increment( s"$prefix-test-decr-by-one", -1 ).sync must beEqualTo( 3 )
      Cache.increment( s"$prefix-test-decr-by-one", -1 ).sync must beEqualTo( 2 )
      Cache.increment( s"$prefix-test-decr-by-one", -1 ).sync must beEqualTo( 1 )
      Cache.increment( s"$prefix-test-decr-by-one", -1 ).sync must beEqualTo( 0 )
      Cache.increment( s"$prefix-test-decr-by-one", -1 ).sync must beEqualTo( -1 )
    }

    "decrement by some" in {
      Cache.set( s"$prefix-test-decr-by-some", 5 ).sync
      Cache.increment( s"$prefix-test-decr-by-some", -1 ).sync must beEqualTo( 4 )
      Cache.increment( s"$prefix-test-decr-by-some", -2 ).sync must beEqualTo( 2 )
      Cache.increment( s"$prefix-test-decr-by-some", -3 ).sync must beEqualTo( -1 )
    }

    "append like set when value is undefined" in {
      Cache.get[ String ]( s"$prefix-test-append-to-null" ) must beNone
      Cache.append( s"$prefix-test-append-to-null", "value" ).sync
      Cache.get[ String ]( s"$prefix-test-append-to-null" ) must beSome( "value" )
    }

    "append to existing string" in {
      Cache.set( s"$prefix-test-append-to-some", "some" ).sync
      Cache.get[ String ]( s"$prefix-test-append-to-some" ) must beSome( "some" )
      Cache.append( s"$prefix-test-append-to-some", " value" ).sync
      Cache.get[ String ]( s"$prefix-test-append-to-some" ) must beSome( "some value" )
    }

    "list push left" in {
      Cache.listPrepend( s"$prefix-test-list-prepend", "A", "B", "C" ).sync must beEqualTo( 3 )
      Cache.listPrepend( s"$prefix-test-list-prepend", "D", "E", "F" ).sync must beEqualTo( 6 )
      Cache.listSlice[ String ]( s"$prefix-test-list-prepend", 0, -1 ).sync must beEqualTo( List( "F", "E", "D", "C", "B", "A" ) )
    }

    "list push right" in {
      Cache.listAppend( s"$prefix-test-list-append", "A", "B", "C" ).sync must beEqualTo( 3 )
      Cache.listAppend( s"$prefix-test-list-append", "D", "E", "A" ).sync must beEqualTo( 6 )
      Cache.listSlice[ String ]( s"$prefix-test-list-append", 0, -1 ).sync must beEqualTo( List( "A", "B", "C", "D", "E", "A" ) )
    }

    "list size" in {
      Cache.listSize( s"$prefix-test-list-size" ).sync must beEqualTo( 0 )
      Cache.listPrepend( s"$prefix-test-list-size", "A", "B", "C" ).sync must beEqualTo( 3 )
      Cache.listSize( s"$prefix-test-list-size" ).sync must beEqualTo( 3 )
    }

    "list overwrite at index" in {
      Cache.listPrepend( s"$prefix-test-list-set", "C", "B", "A" ).sync must beEqualTo( 3 )
      Cache.listSetAt( s"$prefix-test-list-set", 1, "D" ).sync
      Cache.listSlice[ String ]( s"$prefix-test-list-set", 0, -1 ).sync must beEqualTo( List( "A", "D", "C" ) )
      Cache.listSetAt( s"$prefix-test-list-set", 3, "D" ).sync must throwA[ IndexOutOfBoundsException ]
    }

    "list pop head" in {
      Cache.listHeadPop[ String ]( s"$prefix-test-list-pop" ).sync must beNone
      Cache.listPrepend( s"$prefix-test-list-pop", "C", "B", "A" ).sync must beEqualTo( 3 )
      Cache.listHeadPop[ String ]( s"$prefix-test-list-pop" ).sync must beSome( "A" )
      Cache.listHeadPop[ String ]( s"$prefix-test-list-pop" ).sync must beSome( "B" )
      Cache.listHeadPop[ String ]( s"$prefix-test-list-pop" ).sync must beSome( "C" )
      Cache.listHeadPop[ String ]( s"$prefix-test-list-pop" ).sync must beNone
    }

    "list slice view" in {
      Cache.listSlice[ String ]( s"$prefix-test-list-slice", 0, -1 ).sync must beEqualTo( List.empty )
      Cache.listPrepend( s"$prefix-test-list-slice", "C", "B", "A" ).sync must beEqualTo( 3 )
      Cache.listSlice[ String ]( s"$prefix-test-list-slice", 0, -1 ).sync must beEqualTo( List( "A", "B", "C" ) )
      Cache.listSlice[ String ]( s"$prefix-test-list-slice", 0, 0 ).sync must beEqualTo( List( "A" ) )
      Cache.listSlice[ String ]( s"$prefix-test-list-slice", -2, -1 ).sync must beEqualTo( List( "B", "C" ) )
    }

    "list remove by value" in {
      Cache.listRemove( s"$prefix-test-list-remove", "A", count = 1 ).sync must beEqualTo( 0 )
      Cache.listPrepend( s"$prefix-test-list-remove", "A", "B", "C" ).sync must beEqualTo( 3 )
      Cache.listRemove( s"$prefix-test-list-remove", "A", count = 1 ).sync must beEqualTo( 1 )
      Cache.listSize( s"$prefix-test-list-remove" ).sync must beEqualTo( 2 )
    }

    "list trim" in {
      Cache.listPrepend( s"$prefix-test-list-trim", "C", "B", "A" ).sync must beEqualTo( 3 )
      Cache.listTrim( s"$prefix-test-list-trim", 1, 2 ).sync
      Cache.listSize( s"$prefix-test-list-trim" ).sync must beEqualTo( 2 )
      Cache.listSlice[ String ]( s"$prefix-test-list-trim", 0, -1 ).sync must beEqualTo( List( "B", "C" ) )
    }

    "list insert" in {
      Cache.listSize( s"$prefix-test-list-insert-1" ).sync must beEqualTo( 0 )
      Cache.listInsert( s"$prefix-test-list-insert-1", "C", "B" ).sync must beNone
      Cache.listPrepend( s"$prefix-test-list-insert-1", "C", "A" ).sync must beEqualTo( 2 )
      Cache.listInsert( s"$prefix-test-list-insert-1", "C", "B" ).sync must beSome( 3 )
      Cache.listInsert( s"$prefix-test-list-insert-1", "E", "D" ).sync must beNone
      Cache.listSlice[ String ]( s"$prefix-test-list-insert-1", 0, -1 ).sync must beEqualTo( List( "A", "B", "C" ) )

      Cache.set( s"$prefix-test-list-insert-2", "string value" ).sync
      Cache.listInsert( s"$prefix-test-list-insert-2", "C", "B" ).sync must throwA[ IllegalArgumentException ]
    }
  }

}
