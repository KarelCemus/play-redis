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
  new RedisListSuite( "implement", "redis-cache-implements", new RedisCache( workingConnector )( Builders.SynchronousBuilder, FailThrough ), AlwaysSuccess )

  new RedisListSuite( "recover from", "redis-cache-recovery", new RedisCache( FailingConnector )( Builders.SynchronousBuilder, RecoverWithDefault ), AlwaysDefault )

  new RedisListSuite( "fail on", "redis-cache-fail", new RedisCache( FailingConnector )( Builders.SynchronousBuilder, FailThrough ), AlwaysException )

  class RedisListSuite( suiteName: String, prefix: String, cache: Cache, expectation: Expectation ) {

    def list[ T: ClassTag ]( key: String ) = cache.list[ T ]( key )

    def strings( key: String ) = list[ String ]( key )

    def objects( key: String ) = list[ SimpleObject ]( key )

    "AsynchronousRedisList" should {

      import expectation._

      suiteName >> {

        "empty list when empty key" in {
          strings( s"$prefix-list-size-1" ).size must expectsNow( beEqualTo( 0 ) )
        }

        "prepend head into empty key" in {
          strings( s"$prefix-list-prepend-1A" ).prepend( "A" ).size must expectsNow( beEqualTo( 1 ) )
          ( "A" +: strings( s"$prefix-list-prepend-1B" ) ).size must expectsNow( beEqualTo( 1 ) )
        }

        "prepend head into existing key" in {
          strings( s"$prefix-list-prepend-2A" ).prepend( "B" ).prepend( "A" ).size must expectsNow( beEqualTo( 2 ) )
          ( "A" +: "B" +: strings( s"$prefix-list-prepend-2B" ) ).size must expectsNow( beEqualTo( 2 ) )
        }

        "fail head prepending into non-list key" in {
          cache.set( s"$prefix-list-prepend-3", "ABC" ) must expectsNow( beUnit )
          strings( s"$prefix-list-prepend-3" ).prepend( "A" ) must expectsNow( throwA[ IllegalArgumentException ] )
        }

        "append into empty key" in {
          strings( s"$prefix-list-append-1A" ).append( "A" ).size must expectsNow( beEqualTo( 1 ) )
          ( strings( s"$prefix-list-append-1B" ) :+ "A" ).size must expectsNow( beEqualTo( 1 ) )
        }

        "append into existing key" in {
          strings( s"$prefix-list-append-2A" ).append( "B" ).append( "A" ).size must expectsNow( beEqualTo( 2 ) )
          ( strings( s"$prefix-list-append-2B" ) :+ "B" :+ "A" ).size must expectsNow( beEqualTo( 2 ) )
        }

        "prepend multiple values into empty key" in {
          strings( s"$prefix-list-prepend-multiple-1" ).size must expectsNow( beEqualTo( 0 ) )
          ( List( "A", "B" ) ++: strings( s"$prefix-list-prepend-multiple-1" ) ).size must expectsNow( beEqualTo( 2 ) )
        }

        "prepend multiple values into existing key" in {
          strings( s"$prefix-list-prepend-multiple-2" ).size must expectsNow( beEqualTo( 0 ) )
          ( List( "A", "B" ) ++: strings( s"$prefix-list-prepend-multiple-2" ) ).size must expectsNow( beEqualTo( 2 ) )
          ( List( "C", "D" ) ++: strings( s"$prefix-list-prepend-multiple-2" ) ).size must expectsNow( beEqualTo( 4 ) )
        }

        "append multiple values into empty key" in {
          strings( s"$prefix-list-append-multiple-1" ).size must expectsNow( beEqualTo( 0 ) )
          ( strings( s"$prefix-list-append-multiple-1" ) :++ List( "A", "B" ) ).size must expectsNow( beEqualTo( 2 ) )
        }

        "append multiple values into existing key" in {
          strings( s"$prefix-list-append-multiple-2" ).size must expectsNow( beEqualTo( 0 ) )
          ( strings( s"$prefix-list-append-multiple-2" ) :++ List( "A", "B" ) ).size must expectsNow( beEqualTo( 2 ) )
          ( strings( s"$prefix-list-append-multiple-2" ) :++ List( "C", "D" ) ).size must expectsNow( beEqualTo( 4 ) )
        }

        "fail on applying non-existing index" in {
          strings( s"$prefix-list-apply-1" ) apply 0 must expectsNow( throwA[ NoSuchElementException ] )
          strings( s"$prefix-list-apply-1" ) :++ List( "A", "B", "C", "D", "E" )
          strings( s"$prefix-list-apply-1" ) apply 5 must expectsNow( throwA[ NoSuchElementException ] )
        }

        "apply existing key" in {
          ( strings( s"$prefix-list-apply-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-apply-2" ) apply 0 must expectsNow( beEqualTo( "A" ) )
          strings( s"$prefix-list-apply-2" ) apply 1 must expectsNow( beEqualTo( "B" ) )
          strings( s"$prefix-list-apply-2" ) apply 2 must expectsNow( beEqualTo( "C" ) )
          strings( s"$prefix-list-apply-2" ) apply -1 must expectsNow( beEqualTo( "E" ) )
        }

        "get None on non-existing key" in {
          strings( s"$prefix-list-get-1" ) get 0 must expectsNow( beNone )
          strings( s"$prefix-list-get-1" ) :++ List( "A", "B", "C", "D", "E" )
          strings( s"$prefix-list-get-1" ) get 5 must expectsNow( beNone )
        }

        "get Some on existing key" in {
          ( strings( s"$prefix-list-get-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-get-2" ) get 0 must expectsNow( beSome( "A" ) )
          strings( s"$prefix-list-get-2" ) get 1 must expectsNow( beSome( "B" ) )
          strings( s"$prefix-list-get-2" ) get 2 must expectsNow( beSome( "C" ) )
          strings( s"$prefix-list-get-2" ) get -1 must expectsNow( beSome( "E" ) )
        }

        "get head on existing key" in {
          ( strings( s"$prefix-list-head-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-head-1" ).head must expectsNow( beEqualTo( "A" ) )
        }

        "fail head on non-existing key" in {
          strings( s"$prefix-list-head-2" ).head must expectsNow( throwA[ NoSuchElementException ] )
        }

        "get Some on headOption on existing key" in {
          ( strings( s"$prefix-list-head-option-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-head-option-2" ).headOption must expectsNow( beSome( "A" ) )
        }

        "fail None on headOption on non-existing key" in {
          strings( s"$prefix-list-head-option-1" ).headOption must expectsNow( beNone )
        }

        "get and remove on headPop on existing key" in {
          strings( s"$prefix-list-head-pop-1" ).size must expectsNow( beEqualTo( 0 ) )
          strings( s"$prefix-list-head-pop-1" ) :++ List( "A", "B", "C", "D", "E" )
          strings( s"$prefix-list-head-pop-1" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-head-pop-1" ).headPop must expectsNow( beSome( "A" ) )
          strings( s"$prefix-list-head-pop-1" ).headPop must expectsNow( beSome( "B" ) )
          strings( s"$prefix-list-head-pop-1" ).size must expectsNow( beEqualTo( 3 ) )
        }

        "get None on headPop on non-existing key" in {
          strings( s"$prefix-list-head-pop-2" ).size must expectsNow( beEqualTo( 0 ) )
          strings( s"$prefix-list-head-pop-2" ).headPop must expectsNow( beNone )
          strings( s"$prefix-list-head-pop-2" ).size must expectsNow( beEqualTo( 0 ) )
        }

        "get last on existing key" in {
          ( strings( s"$prefix-list-last-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-last-1" ).last must expectsNow( beEqualTo( "E" ) )
        }

        "fail last on non-existing key" in {
          strings( s"$prefix-list-last-2" ).last must expectsNow( throwA[ NoSuchElementException ] )
        }

        "get empty list on toList on non-existing key" in {
          strings( s"$prefix-list-toList-1" ).toList must expectsNow( beEqualTo( List.empty ) )
        }

        "get all values on toList on existing key" in {
          ( List( "G", "H" ) ++: strings( s"$prefix-list-toList-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-toList-2" ).toList must expectsNow( beEqualTo( List( "G", "H", "A", "B", "C", "D", "E" ) ) )
        }

        "insert on index in empty list" in {
          strings( s"$prefix-list-insert-1" ).size must expectsNow( beEqualTo( 0 ) )
          strings( s"$prefix-list-insert-1" ).insert( 1, "A" ).insert( 0, "A" ).size must expectsNow( beEqualTo( 0 ) )
        }

        "insert on index in single-valued list" in {
          ( strings( s"$prefix-list-insert-3" ) :++ List( "A" ) ).size must expectsNow( beEqualTo( 1 ) )
          strings( s"$prefix-list-insert-3" ).size must expectsNow( beEqualTo( 1 ) )
          strings( s"$prefix-list-insert-3" ).insert( 1, "B" ).insert( 0, "C" ).size must expectsNow( beEqualTo( 3 ) )
          strings( s"$prefix-list-insert-3" ).toList must expectsNow( beEqualTo( List( "H", "A", "G" ) ) )
        }

        "insert on index in existing list" in {
          ( strings( s"$prefix-list-insert-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-insert-2" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-insert-2" ).insert( 1, "G" ).insert( 0, "H" ).size must expectsNow( beEqualTo( 7 ) )
          strings( s"$prefix-list-insert-2" ).toList must expectsNow( beEqualTo( List( "H", "A", "G", "B", "C", "D", "E" ) ) )
        }

        "insert multiple values in existing list" in {
          ( strings( s"$prefix-list-insert-multiple-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-insert-multiple-1" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-insert-multiple-1" ).insert( 1, "G", "H" ).size must expectsNow( beEqualTo( 7 ) )
          strings( s"$prefix-list-insert-multiple-1" ).toList must expectsNow( beEqualTo( List( "A", "G", "H", "B", "C", "D", "E" ) ) )
        }

        "override existing value in existing list" in {
          ( strings( s"$prefix-list-set-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-set-1" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-set-1" ).set( 2, "G" ).toList must expectsNow( beEqualTo( List( "A", "B", "G", "D", "E" ) ) )
        }

        "fail overwriting existing value at non-existing position" in {
          ( strings( s"$prefix-list-set-2" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-set-2" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-set-2" ).set( 6, "G" ).toList must expectsNow( throwA[ NoSuchElementException ] )
        }

        "return subset of list and not change the underlying collection" in {
          ( strings( s"$prefix-list-view-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-view-1" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-view-1" ).view.all must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E" ) ) )
          strings( s"$prefix-list-view-1" ).view.take( 2 ) must expectsNow( beEqualTo( List( "A", "B" ) ) )
          strings( s"$prefix-list-view-1" ).view.drop( 2 ) must expectsNow( beEqualTo( List( "C", "D", "E" ) ) )
          strings( s"$prefix-list-view-1" ).view.slice( 2, 4 ) must expectsNow( beEqualTo( List( "C", "D" ) ) )
          strings( s"$prefix-list-view-1" ).size must expectsNow( beEqualTo( 5 ) )
        }

        "modify collection when trimming existing collection" in {
          ( strings( s"$prefix-list-modify-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-modify-1" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-modify-1" ).view.all must expectsNow( beEqualTo( List( "A", "B", "C", "D", "E" ) ) )
          strings( s"$prefix-list-modify-1" ).modify.take( 4 ).collection.view.all must expectsNow( beEqualTo( List( "A", "B", "C", "D" ) ) )
          strings( s"$prefix-list-modify-1" ).modify.drop( 1 ).collection.view.all must expectsNow( beEqualTo( List( "B", "C", "D" ) ) )
          strings( s"$prefix-list-modify-1" ).modify.slice( 1, 2 ).collection.view.all must expectsNow( beEqualTo( List( "C", "D" ) ) )
          strings( s"$prefix-list-modify-1" ).size must expectsNow( beEqualTo( 2 ) )
        }

        "clear existing collection" in {
          ( strings( s"$prefix-list-clear-1" ) :++ List( "A", "B", "C", "D", "E" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-clear-1" ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-clear-1" ).modify.clear().collection.size must expectsNow( beEqualTo( 0 ) )
        }

        "not fail on clearing non-existing collection" in {
          strings( s"$prefix-list-clear-1" ).size must expectsNow( beEqualTo( 0 ) )
          strings( s"$prefix-list-clear-1" ).modify.clear().collection.size must expectsNow( beEqualTo( 0 ) )
        }

        "remove elements by value" in {
          ( strings( s"$prefix-list-remove-by-value-1" ) :++ List( "A", "B", "A", "D", "A" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-remove-by-value-1" ).remove( "A" ).size must expectsNow( beEqualTo( 4 ) )
          strings( s"$prefix-list-remove-by-value-1" ).remove( "A", count = 5 ).size must expectsNow( beEqualTo( 2 ) )
        }

        "remove elements by index" in {
          ( strings( s"$prefix-list-remove-by-index-1" ) :++ List( "A", "B", "A", "D", "A" ) ).size must expectsNow( beEqualTo( 5 ) )
          strings( s"$prefix-list-remove-by-index-1" ).removeAt( 0 ).toList must expectsNow( beEqualTo( List( "B", "A", "D", "A" ) ) )
          strings( s"$prefix-list-remove-by-index-1" ).removeAt( 2 ).toList must expectsNow( beEqualTo( List( "B", "A", "A" ) ) )
          strings( s"$prefix-list-remove-by-index-1" ).removeAt( 5 ).toList must expectsNow( throwA[ NoSuchElementException ] )
        }

        "work with objects" in {
          val A = SimpleObject( "A", 1 )
          val B = SimpleObject( "B", 2 )
          val C = SimpleObject( "C", 3 )
          val D = SimpleObject( "D", 4 )
          val E = SimpleObject( "E", 5 )

          ( A +: ( List( B ) ++: ( ( objects( s"$prefix-list-objects-1" ) :+ C ) :++ List( D ) ) ) ).size must expectsNow( beEqualTo( 4 ) )
          objects( s"$prefix-list-objects-1" ).view.all must expectsNow( beEqualTo( List( A, B, C, D ) ) )
          objects( s"$prefix-list-objects-1" ).view.slice( 1, 2 ) must expectsNow( beEqualTo( List( B, C ) ) )
          objects( s"$prefix-list-objects-1" ).set( 2, E ).toList must expectsNow( beEqualTo( List( A, B, E, D ) ) )
          objects( s"$prefix-list-objects-1" ).headOption must expectsNow( beSome( A ) )
          objects( s"$prefix-list-objects-1" ).lastOption must expectsNow( beSome( D ) )
          objects( s"$prefix-list-objects-1" ).get( 1 ) must expectsNow( beSome( B ) )
        }
      }
    }
  }

}
