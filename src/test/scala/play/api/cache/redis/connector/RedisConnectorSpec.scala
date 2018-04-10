package play.api.cache.redis.connector

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import play.api.cache.redis._
import play.api.cache.redis.impl._
import play.api.inject.ApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.{AfterAll, BeforeAll}

/**
  * <p>Specification of the low level connector implementing basic commands</p>
  */
class RedisConnectorSpec( implicit ee: ExecutionEnv ) extends Specification with BeforeAll with AfterAll {
  import Implicits._

  private val application = GuiceApplicationBuilder().build()

  implicit private val lifecycle = application.injector.instanceOf[ ApplicationLifecycle ]

  implicit private val system = application.actorSystem

  implicit private val runtime = RedisRuntime( "connector", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation )

  private val serializer = new AkkaSerializerImpl( system )

  private val connector: RedisConnector = new RedisConnectorProvider( defaultInstance, serializer ).get

  val prefix = "connector-test"

  "RedisConnector" should {

    "pong on ping" in new TestCase {
      connector.ping( ) must not( throwA[ Throwable ] ).await
    }

    "miss on get" in new TestCase {
      connector.get[ String ]( s"$prefix-$idx" ) must beNone.await
    }

    "hit after set" in new TestCase {
      connector.set( s"$prefix-$idx", "value" ).await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome[ Any ].await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome( "value" ).await
    }

    "ignore set if not exists when already defined" in new TestCase {
      connector.set( s"$prefix-if-not-exists-when-exists", "previous" ).await
      connector.setIfNotExists( s"$prefix-if-not-exists-when-exists", "value" ) must beFalse.await
      connector.get[ String ]( s"$prefix-if-not-exists-when-exists" ) must beSome[ Any ].await
      connector.get[ String ]( s"$prefix-if-not-exists-when-exists" ) must beSome( "previous" ).await
    }

    "perform set if not exists when undefined" in new TestCase {
      connector.setIfNotExists( s"$prefix-if-not-exists", "value" ) must beTrue.await
      connector.get[ String ]( s"$prefix-if-not-exists" ) must beSome[ Any ].await
      connector.get[ String ]( s"$prefix-if-not-exists" ) must beSome( "value" ).await
    }

    "hit after mset" in new TestCase {
      connector.mSet( s"$prefix-mset-$idx-1" -> "value-1", s"$prefix-mset-$idx-2" -> "value-2" ).await
      connector.mGet[ String ]( s"$prefix-mset-$idx-1", s"$prefix-mset-$idx-2", s"$prefix-mset-$idx-3" ) must beEqualTo( List( Some( "value-1" ), Some( "value-2" ), None ) ).await
      connector.mSet( s"$prefix-mset-$idx-3" -> "value-3", s"$prefix-mset-$idx-2" -> null ).await
      connector.mGet[ String ]( s"$prefix-mset-$idx-1", s"$prefix-mset-$idx-2", s"$prefix-mset-$idx-3" ) must beEqualTo( List( Some( "value-1" ), None, Some( "value-3" ) ) ).await
      connector.mSet( s"$prefix-mset-$idx-3" -> null ).await
      connector.mGet[ String ]( s"$prefix-mset-$idx-1", s"$prefix-mset-$idx-2", s"$prefix-mset-$idx-3" ) must beEqualTo( List( Some( "value-1" ), None, None ) ).await
    }

    "ignore msetnx if already defined" in new TestCase {
      connector.mSetIfNotExist( s"$prefix-msetnx-$idx-1" -> "value-1", s"$prefix-msetnx-$idx-2" -> "value-2" ) must beTrue.await
      connector.mGet[ String ]( s"$prefix-msetnx-$idx-1", s"$prefix-msetnx-$idx-2" ) must beEqualTo( List( Some( "value-1" ), Some( "value-2" ) ) ).await
      connector.mSetIfNotExist( s"$prefix-msetnx-$idx-3" -> "value-3", s"$prefix-msetnx-$idx-2" -> "value-2" ) must beFalse.await
    }

    "expire refreshes expiration" in new TestCase {
      connector.set( s"$prefix-$idx", "value", 2.second ).await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome( "value" ).await
      connector.expire( s"$prefix-$idx", 1.minute ).await
      // wait until the first duration expires
      Future.after( 3 ) must not( throwA[ Throwable ] ).awaitFor( 4.seconds )
      connector.get[ String ]( s"$prefix-$idx" ) must beSome( "value" ).await
    }

    "positive exists on existing keys" in new TestCase {
      connector.set( s"$prefix-$idx", "value" ).await
      connector.exists( s"$prefix-$idx" ) must beTrue.await
    }

    "negative exists on expired and missing keys" in new TestCase {
      connector.set( s"$prefix-$idx-1", "value", 1.second ).await
      // wait until the duration expires
      Future.after( 2 ) must not( throwA[ Throwable ] ).awaitFor( 3.seconds )
      connector.exists( s"$prefix-$idx-1" ) must beFalse.await
      connector.exists( s"$prefix-$idx-2" ) must beFalse.await
    }

    "miss after remove" in new TestCase {
      connector.set( s"$prefix-$idx", "value" ).await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome[ Any ].await
      connector.remove( s"$prefix-$idx" ) must not( throwA[ Throwable ] ).await
      connector.get[ String ]( s"$prefix-$idx" ) must beNone.await
    }

    "remove on empty key" in new TestCase {
      connector.get[ String ]( s"$prefix-$idx-A" ) must beNone.await
      connector.remove( s"$prefix-$idx-A" ) must not( throwA[ Throwable ] ).await
      connector.get[ String ]( s"$prefix-$idx-A" ) must beNone.await
    }

    "remove with empty args" in new TestCase {
      val toBeRemoved = List.empty
      connector.remove( toBeRemoved: _* ) must not( throwA[ Throwable ] ).await
    }

    "clear with setting null" in new TestCase {
      connector.set( s"$prefix-$idx", "value" ).await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome[ Any ].await
      connector.set( s"$prefix-$idx", null ).await
      connector.get[ String ]( s"$prefix-$idx" ) must beNone.await
    }

    "miss after timeout" in new TestCase {
      // set
      connector.set( s"$prefix-$idx", "value", 1.second ).await
      connector.get[ String ]( s"$prefix-$idx" ) must beSome[ Any ].await
      // wait until it expires
      Future.after( 2 ) must not( throwA[ Throwable ] ).awaitFor( 3.seconds )
      // miss
      connector.get[ String ]( s"$prefix-$idx" ) must beNone.await
    }

    "find all matching keys" in new TestCase {
      connector.set( s"$prefix-$idx-key-A", "value", 3.second ).await
      connector.set( s"$prefix-$idx-note-A", "value", 3.second ).await
      connector.set( s"$prefix-$idx-key-B", "value", 3.second ).await
      connector.matching( s"$prefix-$idx*" ).map( _.toSet ) must beEqualTo( Set( s"$prefix-$idx-key-A", s"$prefix-$idx-note-A", s"$prefix-$idx-key-B" ) ).await
      connector.matching( s"$prefix-$idx*A" ).map( _.toSet ) must beEqualTo( Set( s"$prefix-$idx-key-A", s"$prefix-$idx-note-A" ) ).await
      connector.matching( s"$prefix-$idx-key-*" ).map( _.toSet ) must beEqualTo( Set( s"$prefix-$idx-key-A", s"$prefix-$idx-key-B" ) ).await
      connector.matching( s"$prefix-${idx}A*" ) must beEqualTo( Seq.empty ).await
    }

    "remove multiple keys at once" in new TestCase {
      connector.set( s"$prefix-remove-multiple-1", "value" ).await
      connector.get[ String ]( s"$prefix-remove-multiple-1" ) must beSome[ Any ].await
      connector.set( s"$prefix-remove-multiple-2", "value" ).await
      connector.get[ String ]( s"$prefix-remove-multiple-2" ) must beSome[ Any ].await
      connector.set( s"$prefix-remove-multiple-3", "value" ).await
      connector.get[ String ]( s"$prefix-remove-multiple-3" ) must beSome[ Any ].await
      connector.remove( s"$prefix-remove-multiple-1", s"$prefix-remove-multiple-2", s"$prefix-remove-multiple-3" ).await
      connector.get[ String ]( s"$prefix-remove-multiple-1" ) must beNone.await
      connector.get[ String ]( s"$prefix-remove-multiple-2" ) must beNone.await
      connector.get[ String ]( s"$prefix-remove-multiple-3" ) must beNone.await
    }

    "remove in batch" in new TestCase {
      connector.set( s"$prefix-remove-batch-1", "value" ).await
      connector.get[ String ]( s"$prefix-remove-batch-1" ) must beSome[ Any ].await
      connector.set( s"$prefix-remove-batch-2", "value" ).await
      connector.get[ String ]( s"$prefix-remove-batch-2" ) must beSome[ Any ].await
      connector.set( s"$prefix-remove-batch-3", "value" ).await
      connector.get[ String ]( s"$prefix-remove-batch-3" ) must beSome[ Any ].await
      connector.remove( s"$prefix-remove-batch-1", s"$prefix-remove-batch-2", s"$prefix-remove-batch-3" ).await
      connector.get[ String ]( s"$prefix-remove-batch-1" ) must beNone.await
      connector.get[ String ]( s"$prefix-remove-batch-2" ) must beNone.await
      connector.get[ String ]( s"$prefix-remove-batch-3" ) must beNone.await
    }

    "set a zero when not exists and then increment" in new TestCase {
      connector.increment( s"$prefix-incr-null", 1 ) must beEqualTo( 1 ).await
    }

    "throw an exception when not integer" in new TestCase {
      connector.set( s"$prefix-incr-string", "value" ).await
      connector.increment( s"$prefix-incr-string", 1 ) must throwA[ ExecutionFailedException ].await
    }

    "increment by one" in new TestCase {
      connector.set( s"$prefix-incr-by-one", 5 ).await
      connector.increment( s"$prefix-incr-by-one", 1 ) must beEqualTo( 6 ).await
      connector.increment( s"$prefix-incr-by-one", 1 ) must beEqualTo( 7 ).await
      connector.increment( s"$prefix-incr-by-one", 1 ) must beEqualTo( 8 ).await
    }

    "increment by some" in new TestCase {
      connector.set( s"$prefix-incr-by-some", 5 ).await
      connector.increment( s"$prefix-incr-by-some", 1 ) must beEqualTo( 6 ).await
      connector.increment( s"$prefix-incr-by-some", 2 ) must beEqualTo( 8 ).await
      connector.increment( s"$prefix-incr-by-some", 3 ) must beEqualTo( 11 ).await
    }

    "decrement by one" in new TestCase {
      connector.set( s"$prefix-decr-by-one", 5 ).await
      connector.increment( s"$prefix-decr-by-one", -1 ) must beEqualTo( 4 ).await
      connector.increment( s"$prefix-decr-by-one", -1 ) must beEqualTo( 3 ).await
      connector.increment( s"$prefix-decr-by-one", -1 ) must beEqualTo( 2 ).await
      connector.increment( s"$prefix-decr-by-one", -1 ) must beEqualTo( 1 ).await
      connector.increment( s"$prefix-decr-by-one", -1 ) must beEqualTo( 0 ).await
      connector.increment( s"$prefix-decr-by-one", -1 ) must beEqualTo( -1 ).await
    }

    "decrement by some" in new TestCase {
      connector.set( s"$prefix-decr-by-some", 5 ).await
      connector.increment( s"$prefix-decr-by-some", -1 ) must beEqualTo( 4 ).await
      connector.increment( s"$prefix-decr-by-some", -2 ) must beEqualTo( 2 ).await
      connector.increment( s"$prefix-decr-by-some", -3 ) must beEqualTo( -1 ).await
    }

    "append like set when value is undefined" in new TestCase {
      connector.get[ String ]( s"$prefix-append-to-null" ) must beNone.await
      connector.append( s"$prefix-append-to-null", "value" ).await
      connector.get[ String ]( s"$prefix-append-to-null" ) must beSome( "value" ).await
    }

    "append to existing string" in new TestCase {
      connector.set( s"$prefix-append-to-some", "some" ).await
      connector.get[ String ]( s"$prefix-append-to-some" ) must beSome( "some" ).await
      connector.append( s"$prefix-append-to-some", " value" ).await
      connector.get[ String ]( s"$prefix-append-to-some" ) must beSome( "some value" ).await
    }

    "list push left" in new TestCase {
      connector.listPrepend( s"$prefix-list-prepend", "A", "B", "C" ) must beEqualTo( 3 ).await
      connector.listPrepend( s"$prefix-list-prepend", "D", "E", "F" ) must beEqualTo( 6 ).await
      connector.listSlice[ String ]( s"$prefix-list-prepend", 0, -1 ) must beEqualTo( List( "F", "E", "D", "C", "B", "A" ) ).await
    }

    "list push right" in new TestCase {
      connector.listAppend( s"$prefix-list-append", "A", "B", "C" ) must beEqualTo( 3 ).await
      connector.listAppend( s"$prefix-list-append", "D", "E", "A" ) must beEqualTo( 6 ).await
      connector.listSlice[ String ]( s"$prefix-list-append", 0, -1 ) must beEqualTo( List( "A", "B", "C", "D", "E", "A" ) ).await
    }

    "list size" in new TestCase {
      connector.listSize( s"$prefix-list-size" ) must beEqualTo( 0 ).await
      connector.listPrepend( s"$prefix-list-size", "A", "B", "C" ) must beEqualTo( 3 ).await
      connector.listSize( s"$prefix-list-size" ) must beEqualTo( 3 ).await
    }

    "list overwrite at index" in new TestCase {
      connector.listPrepend( s"$prefix-list-set", "C", "B", "A" ) must beEqualTo( 3 ).await
      connector.listSetAt( s"$prefix-list-set", 1, "D" ).await
      connector.listSlice[ String ]( s"$prefix-list-set", 0, -1 ) must beEqualTo( List( "A", "D", "C" ) ).await
      connector.listSetAt( s"$prefix-list-set", 3, "D" ) must throwA[ IndexOutOfBoundsException ].await
    }

    "list pop head" in new TestCase {
      connector.listHeadPop[ String ]( s"$prefix-list-pop" ) must beNone.await
      connector.listPrepend( s"$prefix-list-pop", "C", "B", "A" ) must beEqualTo( 3 ).await
      connector.listHeadPop[ String ]( s"$prefix-list-pop" ) must beSome( "A" ).await
      connector.listHeadPop[ String ]( s"$prefix-list-pop" ) must beSome( "B" ).await
      connector.listHeadPop[ String ]( s"$prefix-list-pop" ) must beSome( "C" ).await
      connector.listHeadPop[ String ]( s"$prefix-list-pop" ) must beNone.await
    }

    "list slice view" in new TestCase {
      connector.listSlice[ String ]( s"$prefix-list-slice", 0, -1 ) must beEqualTo( List.empty ).await
      connector.listPrepend( s"$prefix-list-slice", "C", "B", "A" ) must beEqualTo( 3 ).await
      connector.listSlice[ String ]( s"$prefix-list-slice", 0, -1 ) must beEqualTo( List( "A", "B", "C" ) ).await
      connector.listSlice[ String ]( s"$prefix-list-slice", 0, 0 ) must beEqualTo( List( "A" ) ).await
      connector.listSlice[ String ]( s"$prefix-list-slice", -2, -1 ) must beEqualTo( List( "B", "C" ) ).await
    }

    "list remove by value" in new TestCase {
      connector.listRemove( s"$prefix-list-remove", "A", count = 1 ) must beEqualTo( 0 ).await
      connector.listPrepend( s"$prefix-list-remove", "A", "B", "C" ) must beEqualTo( 3 ).await
      connector.listRemove( s"$prefix-list-remove", "A", count = 1 ) must beEqualTo( 1 ).await
      connector.listSize( s"$prefix-list-remove" ) must beEqualTo( 2 ).await
    }

    "list trim" in new TestCase {
      connector.listPrepend( s"$prefix-list-trim", "C", "B", "A" ) must beEqualTo( 3 ).await
      connector.listTrim( s"$prefix-list-trim", 1, 2 ).await
      connector.listSize( s"$prefix-list-trim" ) must beEqualTo( 2 ).await
      connector.listSlice[ String ]( s"$prefix-list-trim", 0, -1 ) must beEqualTo( List( "B", "C" ) ).await
    }

    "list insert" in new TestCase {
      connector.listSize( s"$prefix-list-insert-1" ) must beEqualTo( 0 ).await
      connector.listInsert( s"$prefix-list-insert-1", "C", "B" ) must beNone.await
      connector.listPrepend( s"$prefix-list-insert-1", "C", "A" ) must beEqualTo( 2 ).await
      connector.listInsert( s"$prefix-list-insert-1", "C", "B" ) must beSome( 3L ).await
      connector.listInsert( s"$prefix-list-insert-1", "E", "D" ) must beNone.await
      connector.listSlice[ String ]( s"$prefix-list-insert-1", 0, -1 ) must beEqualTo( List( "A", "B", "C" ) ).await
    }

    "list set to invalid type" in new TestCase {
      connector.set( s"$prefix-list-invalid-$idx", "value" ) must not( throwA[ Throwable ] ).await
      connector.get[ String ]( s"$prefix-list-invalid-$idx" ) must beSome( "value" ).await
      connector.listPrepend( s"$prefix-list-invalid-$idx", "A" ) must throwA[ IllegalArgumentException ].await
      connector.listAppend( s"$prefix-list-invalid-$idx", "C", "B" ) must throwA[ IllegalArgumentException ].await
      connector.listInsert( s"$prefix-list-invalid-$idx", "C", "B" ) must throwA[ IllegalArgumentException ].await
    }

    "set add" in new TestCase {
      connector.setSize( s"$prefix-set-add" ) must beEqualTo( 0 ).await
      connector.setAdd( s"$prefix-set-add", "A", "B" ) must beEqualTo( 2 ).await
      connector.setSize( s"$prefix-set-add" ) must beEqualTo( 2 ).await
      connector.setAdd( s"$prefix-set-add", "C", "B" ) must beEqualTo( 1 ).await
      connector.setSize( s"$prefix-set-add" ) must beEqualTo( 3 ).await
    }

    "set add into invalid type" in new TestCase {
      connector.set( s"$prefix-set-invalid-$idx", "value" ) must not( throwA[ Throwable ] ).await
      connector.get[ String ]( s"$prefix-set-invalid-$idx" ) must beSome( "value" ).await
      connector.setAdd( s"$prefix-set-invalid-$idx", "A", "B" ) must throwA[ IllegalArgumentException ].await
    }

    "set rank" in new TestCase {
      connector.setSize( s"$prefix-set-rank" ) must beEqualTo( 0 ).await
      connector.setAdd( s"$prefix-set-rank", "A", "B" ) must beEqualTo( 2 ).await
      connector.setSize( s"$prefix-set-rank" ) must beEqualTo( 2 ).await

      connector.setIsMember( s"$prefix-set-rank", "A" ) must beTrue.await
      connector.setIsMember( s"$prefix-set-rank", "B" ) must beTrue.await
      connector.setIsMember( s"$prefix-set-rank", "C" ) must beFalse.await

      connector.setAdd( s"$prefix-set-rank", "C", "B" ) must beEqualTo( 1 ).await

      connector.setIsMember( s"$prefix-set-rank", "A" ) must beTrue.await
      connector.setIsMember( s"$prefix-set-rank", "B" ) must beTrue.await
      connector.setIsMember( s"$prefix-set-rank", "C" ) must beTrue.await
    }

    "set size" in new TestCase {
      connector.setSize( s"$prefix-set-size" ) must beEqualTo( 0 ).await
      connector.setAdd( s"$prefix-set-size", "A", "B" ) must beEqualTo( 2 ).await
      connector.setSize( s"$prefix-set-size" ) must beEqualTo( 2 ).await
    }

    "set rem" in new TestCase {
      connector.setSize( s"$prefix-set-rem" ) must beEqualTo( 0 ).await
      connector.setAdd( s"$prefix-set-rem", "A", "B", "C" ) must beEqualTo( 3 ).await
      connector.setSize( s"$prefix-set-rem" ) must beEqualTo( 3 ).await

      connector.setRemove( s"$prefix-set-rem", "A" ) must beEqualTo( 1 ).await
      connector.setSize( s"$prefix-set-rem" ) must beEqualTo( 2 ).await
      connector.setRemove( s"$prefix-set-rem", "B", "C", "D" ) must beEqualTo( 2 ).await
      connector.setSize( s"$prefix-set-rem" ) must beEqualTo( 0 ).await
    }

    "set slice" in new TestCase {
      connector.setSize( s"$prefix-set-slice" ) must beEqualTo( 0 ).await
      connector.setAdd( s"$prefix-set-slice", "A", "B", "C" ) must beEqualTo( 3 ).await
      connector.setSize( s"$prefix-set-slice" ) must beEqualTo( 3 ).await

      connector.setMembers[ String ]( s"$prefix-set-slice" ) must beEqualTo( Set( "A", "B", "C" ) ).await

      connector.setSize( s"$prefix-set-slice" ) must beEqualTo( 3 ).await
    }

    "hash set values" in new TestCase {
      val key = s"$prefix-hash-set"

      connector.hashSize( key ) must beEqualTo( 0 ).await
      connector.hashGetAll( key ) must beEqualTo( Map.empty ).await
      connector.hashKeys( key ) must beEqualTo( Set.empty ).await
      connector.hashValues[ String ]( key ) must beEqualTo( Set.empty ).await

      connector.hashGet[ String ]( key, "KA" ) must beNone.await
      connector.hashSet( key, "KA", "VA1" ) must beTrue.await
      connector.hashGet[ String ]( key, "KA" ) must beSome( "VA1" ).await
      connector.hashSet( key, "KA", "VA2" ) must beFalse.await
      connector.hashGet[ String ]( key, "KA" ) must beSome( "VA2" ).await
      connector.hashSet( key, "KB", "VB" ) must beTrue.await

      connector.hashExists( key, "KB" ) must beTrue.await
      connector.hashExists( key, "KC" ) must beFalse.await

      connector.hashSize( key ) must beEqualTo( 2 ).await
      connector.hashGetAll[ String ]( key ) must beEqualTo( Map( "KA" -> "VA2", "KB" -> "VB" ) ).await
      connector.hashKeys( key ) must beEqualTo( Set( "KA", "KB" ) ).await
      connector.hashValues[ String ]( key ) must beEqualTo( Set( "VA2", "VB" ) ).await

      connector.hashRemove( key, "KB" ) must beEqualTo( 1 ).await
      connector.hashRemove( key, "KC" ) must beEqualTo( 0 ).await
      connector.hashExists( key, "KB" ) must beFalse.await
      connector.hashExists( key, "KA" ) must beTrue.await

      connector.hashSize( key ) must beEqualTo( 1 ).await
      connector.hashGetAll[ String ]( key ) must beEqualTo( Map( "KA" -> "VA2" ) ).await
      connector.hashKeys( key ) must beEqualTo( Set( "KA" ) ).await
      connector.hashValues[ String ]( key ) must beEqualTo( Set( "VA2" ) ).await

      connector.hashSet( key, "KD", 5 ) must beTrue.await
      connector.hashIncrement( key, "KD", 2 ) must beEqualTo( 7 ).await
      connector.hashGet[ Int ]( key, "KD" ) must beSome( 7 ).await
    }

    "hash set into invalid type" in new TestCase {
      connector.set( s"$prefix-hash-invalid-$idx", "value" ) must not( throwA[ Throwable ] ).await
      connector.get[ String ]( s"$prefix-hash-invalid-$idx" ) must beSome( "value" ).await
      connector.hashSet( s"$prefix-hash-invalid-$idx", "KA", "VA1" ) must throwA[ IllegalArgumentException ].await
    }
  }

  def beforeAll( ) = {
    // initialize the connector by flushing the database
    connector.matching( s"$prefix-*" ).flatMap( connector.remove ).await
  }

  def afterAll( ) = {
    lifecycle.stop()
  }
}
