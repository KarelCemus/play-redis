package play.api.cache.redis.impl

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.cache.redis._
import play.api.cache.redis.exception._

import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
  * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
  */
class AsynchronousCacheSpec extends Specification with Redis {

  private type Cache = RedisCache[ AsynchronousResult ]

  private val workingConnector = injector.instanceOf[ RedisConnector ]

  // test proper implementation, no fails
  new RedisCacheSuite( "implement", "redis-cache-implements", new RedisCache( "play", workingConnector )( Builders.AsynchronousBuilder, FailThrough ), AlwaysSuccess )

  new RedisCacheSuite( "recover from with working connector", "redis-cache-implements-and-recovery", new RedisCache( "play", workingConnector )( Builders.AsynchronousBuilder, RecoverWithDefault ), SuccessOrDefault )

  new RedisCacheSuite( "recover from", "redis-cache-recovery", new RedisCache( "play", FailingConnector )( Builders.AsynchronousBuilder, RecoverWithDefault ), AlwaysDefault )

  new RedisCacheSuite( "fail on", "redis-cache-fail", new RedisCache( "play", FailingConnector )( Builders.AsynchronousBuilder, FailThrough ), AlwaysException )

  class RedisCacheSuite( suiteName: String, prefix: String, cache: Cache, expectation: Expectation ) {

    "AsynchronousCacheApi" should {

      import expectation._

      suiteName >> {

        "miss on get" in {
          cache.get[ String ]( s"$prefix-test-1" ) must expects( beNone )
        }

        "hit after set" in {
          cache.set( s"$prefix-test-2", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-2" ) must expects( beSome[ Any ], beNone )
          cache.get[ String ]( s"$prefix-test-2" ) must expects( beSome( "value" ), beNone )
        }

        "expire refreshes expiration" in {
          cache.set( s"$prefix-test-10", "value", 2.second ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-10" ) must expects( beSome( "value" ), beNone )
          cache.expire( s"$prefix-test-10", 1.minute ) must expects( beUnit )
          // wait until the first duration expires
          Thread.sleep( 3000 )
          cache.get[ String ]( s"$prefix-test-10" ) must expects( beSome( "value" ), beNone )
        }

        "positive exists on existing keys" in {
          cache.set( s"$prefix-test-11", "value" ) must expects( beUnit )
          cache.exists( s"$prefix-test-11" ) must expects( beTrue, beFalse )
        }

        "negative exists on expired and missing keys" in {
          cache.set( s"$prefix-test-12A", "value", 1.second ) must expects( beUnit )
          // wait until the duration expires
          Thread.sleep( 2000 )
          cache.exists( s"$prefix-test-12A" ) must expects( beFalse, beFalse )
          cache.exists( s"$prefix-test-12B" ) must expects( beFalse, beFalse )
        }

        "ignore set if not exists when already defined and preserve original expiration mark" in {
          cache.set( s"$prefix-test-if-not-exists-when-exists", "previous", 2.seconds ) must expects( beUnit )
          cache.setIfNotExists( s"$prefix-test-if-not-exists-when-exists", "value", 5.seconds ) must expects( beFalse, beTrue )
          cache.get[ String ]( s"$prefix-test-if-not-exists-when-exists" ) must expects( beSome[ Any ], beNone )
          cache.get[ String ]( s"$prefix-test-if-not-exists-when-exists" ) must expects( beSome( "previous" ), beNone )
          // wait until the duration expires
          Thread.sleep( 3000 )
          cache.get[ String ]( s"$prefix-test-if-not-exists-when-exists" ) must expects( beNone )
        }

        "perform set if not exists when undefined" in {
          cache.setIfNotExists( s"$prefix-test-if-not-exists", "value" ) must expects( beTrue )
          cache.get[ String ]( s"$prefix-test-if-not-exists" ) must expects( beSome[ Any ], beNone )
          cache.get[ String ]( s"$prefix-test-if-not-exists" ) must expects( beSome( "value" ), beNone )
        }

        "perform set if not exists when undefined but expire after some time" in {
          cache.setIfNotExists( s"$prefix-test-if-not-exists-and-expire", "value", 1.seconds ) must expects( beTrue )
          cache.get[ String ]( s"$prefix-test-if-not-exists-and-expire" ) must expects( beSome[ Any ], beNone )
          cache.get[ String ]( s"$prefix-test-if-not-exists-and-expire" ) must expects( beSome( "value" ), beNone )
          // wait until the duration expires
          Thread.sleep( 2000 )
          cache.get[ String ]( s"$prefix-test-if-not-exists-and-expire" ) must expects( beNone )
        }

        "miss after remove" in {
          cache.set( s"$prefix-test-3", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-3" ) must expects( beSome[ Any ], beNone )
          cache.remove( s"$prefix-test-3" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-3" ) must expects( beNone )
        }

        "miss after timeout" in {
          // set
          cache.set( s"$prefix-test-4", "value", 1.second ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-4" ) must expects( beSome[ Any ], beNone )
          // wait until it expires
          Thread.sleep( 1500 )
          // miss
          cache.get[ String ]( s"$prefix-test-4" ) must expects( beNone )
        }

        "miss at first getOrElse " in {
          val counter = new AtomicInteger( 0 )
          cache.getOrElseCounting( s"$prefix-test-5" )( counter ) must expects( beEqualTo( "value" ) )
          counter.get must expectsNow( beEqualTo( 1 ), beEqualTo( 1 ), beEqualTo( 0 ) )
        }

        "hit at second getOrElse" in {
          val counter = new AtomicInteger( 0 )
          for ( index <- 1 to 10 ) cache.getOrElseCounting( s"$prefix-test-6" )( counter ) must expects( beEqualTo( "value" ) )
          counter.get must expectsNow( beEqualTo( 1 ), beEqualTo( 10 ), beEqualTo( 0 ) )
        }

        "find all matching keys" in {
          cache.set( s"$prefix-test-13-key-A", "value", 3.second ) must expects( beUnit )
          cache.set( s"$prefix-test-13-note-A", "value", 3.second ) must expects( beUnit )
          cache.set( s"$prefix-test-13-key-B", "value", 3.second ) must expects( beUnit )
          cache.matching( s"$prefix-test-13*" ).map( _.sorted ) must expects( beEqualTo( Seq( s"$prefix-test-13-key-A", s"$prefix-test-13-note-A", s"$prefix-test-13-key-B" ).sorted ), beEqualTo( Seq.empty ) )
          cache.matching( s"$prefix-test-13*A" ).map( _.sorted ) must expects( beEqualTo( Seq( s"$prefix-test-13-key-A", s"$prefix-test-13-note-A" ).sorted ), beEqualTo( Seq.empty ) )
          cache.matching( s"$prefix-test-13-key-*" ).map( _.sorted ) must expects( beEqualTo( Seq( s"$prefix-test-13-key-A", s"$prefix-test-13-key-B" ).sorted ), beEqualTo( Seq.empty ) )
          cache.matching( s"$prefix-test-13A*" ) must expects( beEqualTo( Seq.empty ) )
        }

        "remove all matching keys, wildcard at the end" in {
          cache.set( s"$prefix-test-14-key-A", "value", 3.second ) must expects( beUnit )
          cache.set( s"$prefix-test-14-note-A", "value", 3.second ) must expects( beUnit )
          cache.set( s"$prefix-test-14-key-B", "value", 3.second ) must expects( beUnit )
          cache.matching( s"$prefix-test-14*" ).map( _.sorted ) must expects( beEqualTo( Seq( s"$prefix-test-14-key-A", s"$prefix-test-14-note-A", s"$prefix-test-14-key-B" ).sorted ), beEqualTo( Seq.empty ) )
          cache.removeMatching( s"$prefix-test-14*" ) must expects( beUnit )
          cache.matching( s"$prefix-test-14*" ) must expects( beEqualTo( Seq.empty ) )
        }

        "remove all matching keys, wildcard in the middle" in {
          cache.set( s"$prefix-test-15-key-A", "value", 3.second ) must expects( beUnit )
          cache.set( s"$prefix-test-15-note-A", "value", 3.second ) must expects( beUnit )
          cache.set( s"$prefix-test-15-key-B", "value", 3.second ) must expects( beUnit )
          cache.matching( s"$prefix-test-15*A" ).map( _.sorted ) must expects( beEqualTo( Seq( s"$prefix-test-15-key-A", s"$prefix-test-15-note-A" ).sorted ), beEqualTo( Seq.empty ) )
          cache.removeMatching( s"$prefix-test-15*A" ) must expects( beUnit )
          cache.matching( s"$prefix-test-15*A" ) must expects( beEqualTo( Seq.empty ) )
        }

        "remove all matching keys, no match" in {
          cache.matching( s"$prefix-test-16*" ) must expects( beEqualTo( Seq.empty ) )
          cache.removeMatching( s"$prefix-test-16*" ) must expects( beUnit )
          cache.matching( s"$prefix-test-16*" ) must expects( beEqualTo( Seq.empty ) )
        }

        "distinct different keys" in {
          val counter = new AtomicInteger( 0 )
          cache.getOrElseCounting( s"$prefix-test-7A" )( counter ) must expects( beEqualTo( "value" ) )
          cache.getOrElseCounting( s"$prefix-test-7B" )( counter ) must expects( beEqualTo( "value" ) )
          counter.get must expectsNow( beEqualTo( 2 ), beEqualTo( 2 ), beEqualTo( 0 ) )
        }

        "perform future and store result" in {
          val counter = new AtomicInteger( 0 )
          // perform test
          for ( index <- 1 to 5 ) cache.getOrFutureCounting( s"$prefix-test-8" )( counter ) must expects( beEqualTo( "value" ) )
          // verify
          counter.get must expectsNow( beEqualTo( 1 ), beEqualTo( 5 ), beEqualTo( 0 ) )
        }

        "propagate fail in future" in {
          cache.getOrFuture[ String ]( s"$prefix-test-9" ) {
            Future.failed( new IllegalStateException( "Exception in test." ) )
          } must expects( throwA( new IllegalStateException( "Exception in test." ) ) )
        }

        "support list" in {
          // store value
          cache.set( s"$prefix-list", List( "A", "B", "C" ) ) must expects( beUnit )
          // recall
          cache.get[ List[ String ] ]( s"$prefix-list" ) must expects( beSome[ List[ String ] ]( List( "A", "B", "C" ) ), beNone )
        }

        "support a byte" in {
          cache.set( s"$prefix-type.byte", 0xAB.toByte ) must expects( beUnit )
          cache.get[ Byte ]( s"$prefix-type.byte" ) must expects( beSome[ Byte ], beNone )
          cache.get[ Byte ]( s"$prefix-type.byte" ) must expects( beSome( 0xAB.toByte ), beNone )
        }

        "support a char" in {
          cache.set( s"$prefix-type.char.1", 'a' ) must expects( beUnit )
          cache.get[ Char ]( s"$prefix-type.char.1" ) must expects( beSome[ Char ], beNone )
          cache.get[ Char ]( s"$prefix-type.char.1" ) must expects( beSome( 'a' ), beNone )
          cache.set( s"$prefix-type.char.2", 'b' ) must expects( beUnit )
          cache.get[ Char ]( s"$prefix-type.char.2" ) must expects( beSome( 'b' ), beNone )
          cache.set( s"$prefix-type.char.3", 'č' ) must expects( beUnit )
          cache.get[ Char ]( s"$prefix-type.char.3" ) must expects( beSome( 'č' ), beNone )
        }

        "support a short" in {
          cache.set( s"$prefix-type.short", 12.toShort ) must expects( beUnit )
          cache.get[ Short ]( s"$prefix-type.short" ) must expects( beSome[ Short ], beNone )
          cache.get[ Short ]( s"$prefix-type.short" ) must expects( beSome( 12.toShort ), beNone )
        }

        "support an int" in {
          cache.set( s"$prefix-type.int", 15 ) must expects( beUnit )
          cache.get[ Int ]( s"$prefix-type.int" ) must expects( beSome( 15 ), beNone )
        }

        "support a long" in {
          cache.set( s"$prefix-type.long", 144L ) must expects( beUnit )
          cache.get[ Long ]( s"$prefix-type.long" ) must expects( beSome[ Long ], beNone )
          cache.get[ Long ]( s"$prefix-type.long" ) must expects( beSome( 144L ), beNone )
        }

        "support a float" in {
          cache.set( s"$prefix-type.float", 1.23f ) must expects( beUnit )
          cache.get[ Float ]( s"$prefix-type.float" ) must expects( beSome[ Float ], beNone )
          cache.get[ Float ]( s"$prefix-type.float" ) must expects( beSome( 1.23f ), beNone )
        }

        "support a double" in {
          cache.set( s"$prefix-type.double", 3.14 ) must expects( beUnit )
          cache.get[ Double ]( s"$prefix-type.double" ) must expects( beSome[ Double ], beNone )
          cache.get[ Double ]( s"$prefix-type.double" ) must expects( beSome( 3.14 ), beNone )
        }

        "support a date" in {
          cache.set( s"$prefix-type.date", new Date( 123 ) ) must expects( beUnit )
          cache.get[ Date ]( s"$prefix-type.date" ) must expects( beSome( new Date( 123 ) ), beNone )
        }

        "support a datetime" in {
          cache.set( s"$prefix-type.datetime", new DateTime( 123456 ) ) must expects( beUnit )
          cache.get[ DateTime ]( s"$prefix-type.datetime" ) must expects( beSome( new DateTime( 123456 ) ), beNone )
        }

        "support a custom classes" in {
          cache.set( s"$prefix-type.object", SimpleObject( "B", 3 ) ) must expects( beUnit )
          cache.get[ SimpleObject ]( s"$prefix-type.object" ) must expects( beSome( SimpleObject( "B", 3 ) ), beNone )
        }

        "support a null" in {
          cache.set( s"$prefix-type.null", null ) must expects( beUnit )
          cache.get[ SimpleObject ]( s"$prefix-type.null" ) must expects( beNone )
        }

        "remove multiple keys at once" in {
          cache.set( s"$prefix-test-remove-multiple-1", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must expects( beSome[ Any ], beNone )
          cache.set( s"$prefix-test-remove-multiple-2", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must expects( beSome[ Any ], beNone )
          cache.set( s"$prefix-test-remove-multiple-3", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must expects( beSome[ Any ], beNone )
          cache.remove( s"$prefix-test-remove-multiple-1", s"$prefix-test-remove-multiple-2", s"$prefix-test-remove-multiple-3" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-multiple-1" ) must expects( beNone )
          cache.get[ String ]( s"$prefix-test-remove-multiple-2" ) must expects( beNone )
          cache.get[ String ]( s"$prefix-test-remove-multiple-3" ) must expects( beNone )
        }

        "remove in batch" in {
          cache.set( s"$prefix-test-remove-batch-1", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-batch-1" ) must expects( beSome[ Any ], beNone )
          cache.set( s"$prefix-test-remove-batch-2", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-batch-2" ) must expects( beSome[ Any ], beNone )
          cache.set( s"$prefix-test-remove-batch-3", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-batch-3" ) must expects( beSome[ Any ], beNone )
          cache.removeAll( Seq( s"$prefix-test-remove-batch-1", s"$prefix-test-remove-batch-2", s"$prefix-test-remove-batch-3" ): _* ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-remove-batch-1" ) must expects( beNone )
          cache.get[ String ]( s"$prefix-test-remove-batch-2" ) must expects( beNone )
          cache.get[ String ]( s"$prefix-test-remove-batch-3" ) must expects( beNone )
        }

        "set a zero when not exists and then increment" in {
          cache.increment( s"$prefix-test-incr-null" ) must expects( beEqualTo( 1 ), beEqualTo( 1 ) )
        }

        "throw an exception when not integer" in {
          cache.set( s"$prefix-test-incr-string", "value" ) must expects( beUnit )
          cache.increment( s"$prefix-test-incr-string", 1 ) must expects( throwA[ ExecutionFailedException ], beEqualTo( 1 ) )
        }

        "increment by one" in {
          cache.set( s"$prefix-test-incr-by-one", 5 ) must expects( beUnit )
          cache.increment( s"$prefix-test-incr-by-one" ) must expects( beEqualTo( 6 ), beEqualTo( 1 ) )
          cache.increment( s"$prefix-test-incr-by-one" ) must expects( beEqualTo( 7 ), beEqualTo( 1 ) )
          cache.increment( s"$prefix-test-incr-by-one" ) must expects( beEqualTo( 8 ), beEqualTo( 1 ) )
        }

        "increment by some" in {
          cache.set( s"$prefix-test-incr-by-some", 5 ) must expects( beUnit )
          cache.increment( s"$prefix-test-incr-by-some", 1 ) must expects( beEqualTo( 6 ), beEqualTo( 1 ) )
          cache.increment( s"$prefix-test-incr-by-some", 2 ) must expects( beEqualTo( 8 ), beEqualTo( 2 ) )
          cache.increment( s"$prefix-test-incr-by-some", 3 ) must expects( beEqualTo( 11 ), beEqualTo( 3 ) )
        }

        "decrement by one" in {
          cache.set( s"$prefix-test-decr-by-one", 5 ) must expects( beUnit )
          cache.decrement( s"$prefix-test-decr-by-one" ) must expects( beEqualTo( 4 ), beEqualTo( -1 ) )
          cache.decrement( s"$prefix-test-decr-by-one" ) must expects( beEqualTo( 3 ), beEqualTo( -1 ) )
          cache.decrement( s"$prefix-test-decr-by-one" ) must expects( beEqualTo( 2 ), beEqualTo( -1 ) )
          cache.decrement( s"$prefix-test-decr-by-one" ) must expects( beEqualTo( 1 ), beEqualTo( -1 ) )
          cache.decrement( s"$prefix-test-decr-by-one" ) must expects( beEqualTo( 0 ), beEqualTo( -1 ) )
          cache.decrement( s"$prefix-test-decr-by-one" ) must expects( beEqualTo( -1 ), beEqualTo( -1 ) )
        }

        "decrement by some" in {
          cache.set( s"$prefix-test-decr-by-some", 5 ) must expects( beUnit )
          cache.decrement( s"$prefix-test-decr-by-some", 1 ) must expects( beEqualTo( 4 ), beEqualTo( -1 ) )
          cache.decrement( s"$prefix-test-decr-by-some", 2 ) must expects( beEqualTo( 2 ), beEqualTo( -2 ) )
          cache.decrement( s"$prefix-test-decr-by-some", 3 ) must expects( beEqualTo( -1 ), beEqualTo( -3 ) )
        }

        "append like set when value is undefined" in {
          cache.get[ String ]( s"$prefix-test-append-to-null" ) must expects( beNone )
          cache.append( s"$prefix-test-append-to-null", "value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-append-to-null" ) must expects( beSome( "value" ), beNone )
        }

        "append to existing string" in {
          cache.set( s"$prefix-test-append-to-some", "some" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-append-to-some" ) must expects( beSome( "some" ), beNone )
          cache.append( s"$prefix-test-append-to-some", " value" ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-append-to-some" ) must expects( beSome( "some value" ), beNone )
        }

        "append with applied expiration" in {
          cache.get[ String ]( s"$prefix-test-append-and-expire" ) must expects( beNone )
          cache.append( s"$prefix-test-append-and-expire", "value", 2.seconds ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-append-and-expire" ) must expects( beSome( "value" ), beNone )
          // wait until the first duration expires
          Thread.sleep( 3000 )
          cache.get[ String ]( s"$prefix-test-append-and-expire" ) must expects( beNone )
        }

        "append but do not apply expiration" in {
          cache.set( s"$prefix-test-append-and-not-expire", "some", 5.seconds ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-append-and-not-expire" ) must expects( beSome( "some" ), beNone )
          cache.append( s"$prefix-test-append-and-not-expire", " value", 2.seconds ) must expects( beUnit )
          cache.get[ String ]( s"$prefix-test-append-and-not-expire" ) must expects( beSome( "some value" ), beNone )
          // wait until the first duration expires
          Thread.sleep( 3000 )
          cache.get[ String ]( s"$prefix-test-append-and-not-expire" ) must expects( beSome( "some value" ), beNone )
        }

        "with failing serialization" in {
          cache.set( s"$prefix-test-with-failing-serialization", UnserializableObject( "some" ), 5.seconds ) must expects( throwA[ SerializationException ], beUnit )
          cache.getOrElse( s"$prefix-test-with-failing-serialization" )( UnserializableObject( "some" ) ) must expects(
            throwA[ SerializationException ], beEqualTo( UnserializableObject( "some" ) )
          )
        }
      }
    }
  }
}

case class UnserializableObject( value: String )

class FailingSerializer extends akka.serialization.Serializer {

  def identifier = 2017052801

  def toBinary( o: AnyRef ) = throw new IllegalStateException("Failing serialization")

  def includeManifest = false

  def fromBinary( bytes: Array[ Byte ], manifest: Option[ Class[ _ ] ] ) = throw new IllegalStateException("Failing deserialization")
}
