package play.api.cache.redis.impl

import scala.reflect.ClassTag

import play.api.cache.redis._

import org.specs2.mutable.Specification

/**
  * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
  */
class RedisSetSpecs extends Specification with Redis {
  outer =>

  import play.api.cache.redis.TestHelpers._

  private type Cache = RedisCache[ SynchronousResult ]

  private val workingConnector = injector.instanceOf[ RedisConnector ]

  // test proper implementation, no fails
  new RedisSetSuite( "implement", "redis-cache-implements", new RedisCache( "play", workingConnector )( Builders.SynchronousBuilder, FailThrough ), AlwaysSuccess )

  new RedisSetSuite( "recover from", "redis-cache-recovery", new RedisCache( "play", FailingConnector )( Builders.SynchronousBuilder, RecoverWithDefault ), AlwaysDefault )

  new RedisSetSuite( "fail on", "redis-cache-fail", new RedisCache( "play", FailingConnector )( Builders.SynchronousBuilder, FailThrough ), AlwaysException )

  class RedisSetSuite( suiteName: String, prefix: String, cache: Cache, expectation: Expectation ) {

    def set[ T: ClassTag ]( key: String ) = cache.set[ T ]( key )

    def strings( implicit theKey: Key ) = set[ String ]( theKey.key )

    def objects( implicit theKey: Key ) = set[ SimpleObject ]( theKey.key )

    "SynchronousRedisSet" should {

      import expectation._

      suiteName >> {

        "add into the set" in {
          implicit val key: Key = s"$prefix-set-add"

          strings.size must expectsNow( 0 )
          strings.isEmpty must expectsNow( beTrue )
          strings.nonEmpty must expectsNow( beFalse )

          strings.add( "A", "B", "D" ).add( "C" ).add( "A" ).size must expectsNow( 4, 0 )
          strings.isEmpty must expectsNow( beFalse, beTrue )
          strings.nonEmpty must expectsNow( beTrue, beFalse )

          strings.toSet must expectsNow( Set( "A", "B", "C", "D" ), Set.empty )
          strings.contains( "A" ) must expectsNow( beTrue, beFalse )
          strings.contains( "B" ) must expectsNow( beTrue, beFalse )
          strings.contains( "C" ) must expectsNow( beTrue, beFalse )
          strings.contains( "D" ) must expectsNow( beTrue, beFalse )
          strings.contains( "E" ) must expectsNow( beFalse, beFalse )
        }

        "remove from the set" in {
          implicit val key: Key = s"$prefix-set-remove"

          strings.size must expectsNow( 0 )
          strings.add( "A", "B", "D" ).size must expectsNow( 3, 0 )

          strings.contains( "A" ) must expectsNow( beTrue, beFalse )
          strings.contains( "B" ) must expectsNow( beTrue, beFalse )
          strings.contains( "C" ) must expectsNow( beFalse, beFalse )
          strings.contains( "D" ) must expectsNow( beTrue, beFalse )

          strings.remove( "A" ).size must expectsNow( 2, 0 )
          strings.contains( "A" ) must expectsNow( beFalse, beFalse )
          strings.contains( "B" ) must expectsNow( beTrue, beFalse )
          strings.contains( "C" ) must expectsNow( beFalse, beFalse )
          strings.contains( "D" ) must expectsNow( beTrue, beFalse )

          strings.remove( "B", "C", "D" ).size must expectsNow( 0, 0 )
          strings.contains( "A" ) must expectsNow( beFalse, beFalse )
          strings.contains( "B" ) must expectsNow( beFalse, beFalse )
          strings.contains( "C" ) must expectsNow( beFalse, beFalse )
          strings.contains( "D" ) must expectsNow( beFalse, beFalse )
        }

        "working with the set at non-set key" in {
          implicit val key: Key = s"$prefix-set-invalid"

          cache.set( key.key, "invalid" ) must expectsNow( beUnit )
          strings.add( "A" ) must expectsNow( throwA[ IllegalArgumentException ], beAnInstanceOf[ RedisSet[ String, AsynchronousResult ] ] )
        }

        "objects in the set" in {
          implicit val key: Key = s"$prefix-set-objects"

          def A = SimpleObject( "A", 1 )
          def B = SimpleObject( "B", 2 )
          def C = SimpleObject( "C", 3 )
          def D = SimpleObject( "D", 4 )
          def E = SimpleObject( "E", 5 )

          objects.size must expectsNow( 0 )
          objects.add( A, B, D ).add( A ).size must expectsNow( 3, 0 )

          objects.contains( A ) must expectsNow( beTrue, beFalse )
          objects.contains( B ) must expectsNow( beTrue, beFalse )
          objects.contains( C ) must expectsNow( beFalse, beFalse )
          objects.contains( D ) must expectsNow( beTrue, beFalse )

          objects.remove( A ).size must expectsNow( 2, 0 )
          objects.contains( A ) must expectsNow( beFalse, beFalse )
          objects.contains( B ) must expectsNow( beTrue, beFalse )
          objects.contains( C ) must expectsNow( beFalse, beFalse )
          objects.contains( D ) must expectsNow( beTrue, beFalse )

          objects.remove( B, C, D ).size must expectsNow( 0, 0 )
          objects.contains( A ) must expectsNow( beFalse, beFalse )
          objects.contains( B ) must expectsNow( beFalse, beFalse )
          objects.contains( C ) must expectsNow( beFalse, beFalse )
          objects.contains( D ) must expectsNow( beFalse, beFalse )
        }
      }
    }
  }

}
