package play.api.cache.redis.impl

import scala.reflect.ClassTag

import play.api.cache.redis._

import org.specs2.mutable.Specification

/**
  * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
  */
class RedisListSpec extends Specification with Redis {
  outer =>

  private type Cache = RedisCache[ SynchronousResult ]

  private val workingConnector = injector.instanceOf[ RedisConnector ]

  // test proper implementation, no fails
  new RedisListSuite( "implement", "redis-cache-implements", new RedisCache( "play", workingConnector )( Builders.SynchronousBuilder, FailThrough ), AlwaysSuccess )

  new RedisListSuite( "recover from", "redis-cache-recovery", new RedisCache( "play", FailingConnector )( Builders.SynchronousBuilder, RecoverWithDefault ), AlwaysDefault )

  new RedisListSuite( "fail on", "redis-cache-fail", new RedisCache( "play", FailingConnector )( Builders.SynchronousBuilder, FailThrough ), AlwaysException )

  class RedisListSuite( suiteName: String, prefix: String, cache: Cache, expectation: Expectation ) {

    def list[ T: ClassTag ]( key: String ) = cache.list[ T ]( key )

    def strings( key: String ) = list[ String ]( key )

    def objects( key: String ) = list[ SimpleObject ]( key )

    "SynchronousRedisList" should {

      import expectation._

      suiteName >> {

        "empty list when empty key" in {
          strings( s"$prefix-list-size-1" ).size must expectsNow( beEqualTo( 0 ) )
        }

        "prepend head into empty key" in {
          strings( s"$prefix-list-prepend-1A" ).prepend( "A" ).size must expectsNow( beEqualTo( 1 ), beEqualTo( 0 ) )
          ( "A" +: strings( s"$prefix-list-prepend-1B" ) ).size must expectsNow( beEqualTo( 1 ), beEqualTo( 0 ) )
        }

        "prepend head into existing key" in {
          strings( s"$prefix-list-prepend-2A" ).prepend( "B" ).prepend( "A" ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
          ( "A" +: "B" +: strings( s"$prefix-list-prepend-2B" ) ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
        }

        "fail head prepending into non-list key" in {
          cache.set( s"$prefix-list-prepend-3", "ABC" ) must expectsNow( beUnit )
          strings( s"$prefix-list-prepend-3" ).prepend( "A" ) must expectsNow( throwA[ IllegalArgumentException ], beAnInstanceOf[ RedisList[ String, AsynchronousResult ] ] )
        }

        "append into empty key" in {
          strings( s"$prefix-list-append-1A" ).append( "A" ).size must expectsNow( beEqualTo( 1 ), beEqualTo( 0 ) )
          ( strings( s"$prefix-list-append-1B" ) :+ "A" ).size must expectsNow( beEqualTo( 1 ), beEqualTo( 0 ) )
        }

        "append into existing key" in {
          strings( s"$prefix-list-append-2A" ).append( "B" ).append( "A" ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
          ( strings( s"$prefix-list-append-2B" ) :+ "B" :+ "A" ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
        }

        "prepend multiple values into empty key" in {
          strings( s"$prefix-list-prepend-multiple-1" ).size must expectsNow( beEqualTo( 0 ) )
          ( List( "A", "B" ) ++: strings( s"$prefix-list-prepend-multiple-1" ) ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
        }

        "prepend multiple values into existing key" in {
          strings( s"$prefix-list-prepend-multiple-2" ).size must expectsNow( beEqualTo( 0 ) )
          ( List( "A", "B" ) ++: strings( s"$prefix-list-prepend-multiple-2" ) ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
          ( List( "C", "D" ) ++: strings( s"$prefix-list-prepend-multiple-2" ) ).size must expectsNow( beEqualTo( 4 ), beEqualTo( 0 ) )
        }

        "append multiple values into empty key" in {
          strings( s"$prefix-list-append-multiple-1" ).size must expectsNow( beEqualTo( 0 ) )
          ( strings( s"$prefix-list-append-multiple-1" ) :++ List( "A", "B" ) ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
        }

        "append multiple values into existing key" in {
          strings( s"$prefix-list-append-multiple-2" ).size must expectsNow( beEqualTo( 0 ) )
          ( strings( s"$prefix-list-append-multiple-2" ) :++ List( "A", "B" ) ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
          ( strings( s"$prefix-list-append-multiple-2" ) :++ List( "C", "D" ) ).size must expectsNow( beEqualTo( 4 ), beEqualTo( 0 ) )
        }

        "fail on applying non-existing index" in {
          strings( s"$prefix-list-apply-1" ) apply 0 must expectsNow( throwA[ NoSuchElementException ] )
          ( strings( s"$prefix-list-apply-1" ) :++ List( "A", "B", "C", "D", "E" ) ).toList must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-apply-1" ) apply 5 must expectsNow( throwA[ NoSuchElementException ] )
        }

        "apply existing key" in {
          ( strings( s"$prefix-list-apply-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-apply-2" ) apply 0 must expectsNow( beEqualTo( "A" ), throwA[ NoSuchElementException ] )
          strings( s"$prefix-list-apply-2" ) apply 1 must expectsNow( beEqualTo( "B" ), throwA[ NoSuchElementException ] )
          strings( s"$prefix-list-apply-2" ) apply 2 must expectsNow( beEqualTo( "C" ), throwA[ NoSuchElementException ] )
          strings( s"$prefix-list-apply-2" ) apply -1 must expectsNow( beEqualTo( "E" ), throwA[ NoSuchElementException ] )
        }

        "get None on non-existing key" in {
          strings( s"$prefix-list-get-1" ) get 0 must expectsNow( beNone )
          ( strings( s"$prefix-list-get-1" ) :++ List( "A", "B", "C", "D", "E" ) ).toList must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-get-1" ) get 5 must expectsNow( beNone )
        }

        "get Some on existing key" in {
          ( strings( s"$prefix-list-get-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-get-2" ) get 0 must expectsNow( beSome( "A" ), beNone )
          strings( s"$prefix-list-get-2" ) get 1 must expectsNow( beSome( "B" ), beNone )
          strings( s"$prefix-list-get-2" ) get 2 must expectsNow( beSome( "C" ), beNone )
          strings( s"$prefix-list-get-2" ) get -1 must expectsNow( beSome( "E" ), beNone )
        }

        "get head on existing key" in {
          ( strings( s"$prefix-list-head-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-head-1" ).head must expectsNow( beEqualTo( "A" ), throwA[ NoSuchElementException ] )
        }

        "fail head on non-existing key" in {
          strings( s"$prefix-list-head-2" ).head must expectsNow( throwA[ NoSuchElementException ] )
        }

        "get Some on headOption on existing key" in {
          ( strings( s"$prefix-list-head-option-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-head-option-2" ).headOption must expectsNow( beSome( "A" ), beNone )
        }

        "fail None on headOption on non-existing key" in {
          strings( s"$prefix-list-head-option-1" ).headOption must expectsNow( beNone )
        }

        "get and remove on headPop on existing key" in {
          strings( s"$prefix-list-head-pop-1" ).size must expectsNow( beEqualTo( 0 ) )
          ( strings( s"$prefix-list-head-pop-1" ) :++ List( "A", "B", "C", "D", "E" ) ).toList must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-head-pop-1" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-head-pop-1" ).headPop must expectsNow( beSome( "A" ), beNone )
          strings( s"$prefix-list-head-pop-1" ).headPop must expectsNow( beSome( "B" ), beNone )
          strings( s"$prefix-list-head-pop-1" ).size must expectsNow( beEqualTo( 3 ), beEqualTo( 0 ) )
        }

        "get None on headPop on non-existing key" in {
          strings( s"$prefix-list-head-pop-2" ).size must expectsNow( beEqualTo( 0 ) )
          strings( s"$prefix-list-head-pop-2" ).headPop must expectsNow( beNone )
          strings( s"$prefix-list-head-pop-2" ).size must expectsNow( beEqualTo( 0 ) )
        }

        "get last on existing key" in {
          ( strings( s"$prefix-list-last-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-last-1" ).last must expectsNow( beEqualTo( "E" ), throwA[ NoSuchElementException ] )
        }

        "fail last on non-existing key" in {
          strings( s"$prefix-list-last-2" ).last must expectsNow( throwA[ NoSuchElementException ] )
        }

        "get empty list on toList on non-existing key" in {
          strings( s"$prefix-list-toList-1" ).toList must expectsNow( beEqualTo( List.empty ) )
        }

        "get all values on toList on existing key" in {
          ( List( "H", "G" ) ++: strings( s"$prefix-list-toList-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 7 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-toList-2" ).toList must expectsNow( beEqualTo( List( "G", "H", "A", "B", "C", "D", "E" ) ), beEqualTo( List.empty ) )
        }

        "insert before in empty list" in {
          strings( s"$prefix-list-insert-1" ).size must expectsNow( beEqualTo( 0 ) )
          strings( s"$prefix-list-insert-1" ).insertBefore( "B", "A" ) must expectsNow( beNone )
        }

        "insert before in single-valued list" in {
          ( strings( s"$prefix-list-insert-3" ) :++ List( "A" ) ).size must expectsNow( beEqualTo( 1 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-insert-3" ).size must expectsNow( beEqualTo( 1 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-insert-3" ).insertBefore( "A", "B" ) must expectsNow( beSome( 2 ), beNone )
          strings( s"$prefix-list-insert-3" ).toList must expectsNow( beEqualTo( List( "B", "A" ) ), beEqualTo( List.empty ) )
        }

        "insert on index in existing list" in {
          ( strings( s"$prefix-list-insert-2" ) :++ List( "A", "B", "C", "E", "F" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-insert-2" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-insert-2" ).insertBefore( "E", "D" ) must expectsNow( beSome( 6 ), beNone )
          strings( s"$prefix-list-insert-2" ).toList must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E", "F" ) ), beEqualTo( List.empty ) )
        }

        "override existing value in existing list" in {
          ( strings( s"$prefix-list-set-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-set-1" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-set-1" ).set( 2, "G" ).toList must expectsNow( beEqualTo( List( "A", "B", "G", "D", "E" ) ), beEqualTo( List.empty ) )
        }

        "fail overwriting existing value at non-existing position" in {
          ( strings( s"$prefix-list-set-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-set-2" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-set-2" ).set( 6, "G" ).toList must expectsNow( throwA[ IndexOutOfBoundsException ], beEqualTo( List.empty ) )
        }

        "return subset of list and not change the underlying collection" in {
          ( strings( s"$prefix-list-view-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-view-1" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-view-1" ).view.all must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-view-1" ).view.take( 2 ) must expectsNow( beEqualTo( List( "A", "B" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-view-1" ).view.drop( 2 ) must expectsNow( beEqualTo( List( "C", "D", "E" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-view-1" ).view.slice( 2, 3 ) must expectsNow( beEqualTo( List( "C", "D" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-view-1" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
        }

        "modify collection when trimming existing collection" in {
          ( strings( s"$prefix-list-modify-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-modify-1" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-modify-1" ).view.all must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-modify-1" ).modify.take( 4 ).collection.view.all must expectsNow( beEqualTo( List( "A", "B", "C", "D" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-modify-1" ).modify.drop( 1 ).collection.view.all must expectsNow( beEqualTo( List( "B", "C", "D" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-modify-1" ).modify.slice( 1, 2 ).collection.view.all must expectsNow( beEqualTo( List( "C", "D" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-modify-1" ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
        }

        "clear existing collection" in {
          ( strings( s"$prefix-list-clear-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-clear-1" ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-clear-1" ).modify.clear().collection.size must expectsNow( beEqualTo( 0 ) )
        }

        "not fail on clearing non-existing collection" in {
          strings( s"$prefix-list-clear-2" ).size must expectsNow( beEqualTo( 0 ) )
          strings( s"$prefix-list-clear-2" ).modify.clear().collection.size must expectsNow( beEqualTo( 0 ) )
        }

        "remove elements by value" in {
          ( strings( s"$prefix-list-remove-by-value-1" ) :++ List( "A", "B", "A", "D", "A" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-remove-by-value-1" ).remove( "A" ).size must expectsNow( beEqualTo( 4 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-remove-by-value-1" ).remove( "A", count = 5 ).size must expectsNow( beEqualTo( 2 ), beEqualTo( 0 ) )
        }

        "remove elements by index" in {
          ( strings( s"$prefix-list-remove-by-index-1" ) :++ List( "A", "B", "A", "D", "A" ) ).size must expectsNow( beEqualTo( 5 ), beEqualTo( 0 ) )
          strings( s"$prefix-list-remove-by-index-1" ).removeAt( 0 ).toList must expectsNow( beEqualTo( List( "B", "A", "D", "A" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-remove-by-index-1" ).removeAt( 2 ).toList must expectsNow( beEqualTo( List( "B", "A", "A" ) ), beEqualTo( List.empty ) )
          strings( s"$prefix-list-remove-by-index-1" ).removeAt( 5 ).toList must expectsNow( throwA[ IndexOutOfBoundsException ], beEqualTo( List.empty ) )
        }

        "work with objects" in {
          val A = SimpleObject( "A", 1 )
          val B = SimpleObject( "B", 2 )
          val C = SimpleObject( "C", 3 )
          val D = SimpleObject( "D", 4 )
          val E = SimpleObject( "E", 5 )

          ( A +: ( List( B ) ++: ( ( objects( s"$prefix-list-objects-1" ) :+ C ) :++ List( D ) ) ) ).size must expectsNow( beEqualTo( 4 ), beEqualTo( 0 ) )
          objects( s"$prefix-list-objects-1" ).view.all must expectsNow( beEqualTo( List( A, B, C, D ) ), beEqualTo( List.empty ) )
          objects( s"$prefix-list-objects-1" ).view.slice( 1, 2 ) must expectsNow( beEqualTo( List( B, C ) ), beEqualTo( List.empty ) )
          objects( s"$prefix-list-objects-1" ).set( 2, E ).toList must expectsNow( beEqualTo( List( A, B, E, D ) ), beEqualTo( List.empty ) )
          objects( s"$prefix-list-objects-1" ).headOption must expectsNow( beSome( A ), beNone )
          objects( s"$prefix-list-objects-1" ).lastOption must expectsNow( beSome( D ), beNone )
          objects( s"$prefix-list-objects-1" ).get( 1 ) must expectsNow( beSome( B ), beNone )
        }
      }
    }
  }

}
