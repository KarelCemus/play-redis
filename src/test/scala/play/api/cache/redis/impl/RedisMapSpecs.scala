package play.api.cache.redis.impl

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.ClassTag

import play.api.cache.redis._

import org.specs2.mutable.Specification

/**
  * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
  */
class RedisMapSpecs extends Specification with Redis {
  outer =>

  import play.api.cache.redis.TestHelpers._

  private type Cache = RedisCache[ SynchronousResult ]

  private val workingConnector = injector.instanceOf[ RedisConnector ]

  implicit def runtime( policy: RecoveryPolicy ) =  RedisRuntime( "play", 3.minutes, ExecutionContext.Implicits.global, policy, invocation = LazyInvocation )

  // test proper implementation, no fails
  new RedisMapSuite( "implement", "redis-cache-implements", new RedisCache( workingConnector, Builders.SynchronousBuilder )( FailThrough ), AlwaysSuccess )

  new RedisMapSuite( "recover from", "redis-cache-recovery", new RedisCache( FailingConnector, Builders.SynchronousBuilder )( RecoverWithDefault ), AlwaysDefault )

  new RedisMapSuite( "fail on", "redis-cache-fail", new RedisCache( FailingConnector, Builders.SynchronousBuilder )( FailThrough ), AlwaysException )

  class RedisMapSuite( suiteName: String, prefix: String, cache: Cache, expectation: Expectation ) {

    def map[ T: ClassTag ]( key: String ) = cache.map[ T ]( key )

    def numbers( implicit theKey: Key ) = map[ Long ]( theKey.key )

    def strings( implicit theKey: Key ) = map[ String ]( theKey.key )

    def objects( implicit theKey: Key ) = map[ SimpleObject ]( theKey.key )

    "SynchronousRedisMap" should {

      import expectation._

      suiteName >> {

        "add into the map" in {
          implicit val key: Key = s"$prefix-map-add"

          strings.size must expectsNow( 0 )
          strings.isEmpty must expectsNow( beTrue )
          strings.nonEmpty must expectsNow( beFalse )

          strings.add( "KA", "A1" ).add( "KB", "B" ).add( "KC", "C" ).add( "KA", "A2" ).size must expectsNow( 3, 0 )
          strings.isEmpty must expectsNow( beFalse, beTrue )
          strings.nonEmpty must expectsNow( beTrue, beFalse )

          strings.toMap must expectsNow( Map( "KA" -> "A2", "KB" -> "B", "KC" -> "C" ), Map.empty[ String, String ] )
          strings.contains( "KA" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KB" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KC" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KD" ) must expectsNow( beFalse, beFalse )
        }

        "increment in the map" in {
          implicit val key: Key = s"$prefix-map-increment"

          numbers.size must expectsNow( 0 )
          numbers.isEmpty must expectsNow( beTrue )
          numbers.nonEmpty must expectsNow( beFalse )

          numbers.add( "A", 0 ).add( "B", 5 ).add( "C", 10 ).size must expectsNow( 3, 0 )
          numbers.isEmpty must expectsNow( beFalse, beTrue )
          numbers.nonEmpty must expectsNow( beTrue, beFalse )

          numbers.increment( "A" ) must expectsNow( 1, 1 )
          numbers.increment( "B", 2 ) must expectsNow( 7, 2 )
          numbers.increment( "C", -2 ) must expectsNow( 8, -2 )
          numbers.increment( "D", 10 ) must expectsNow( 10, 10 )
          numbers.toMap must expectsNow( Map( "A" -> 1, "B" -> 7, "C" -> 8, "D" -> 10 ), Map.empty[ String, Long ] )
        }

        "remove from the map" in {
          implicit val key: Key = s"$prefix-map-remove"

          strings.size must expectsNow( 0 )
          strings.add( "KA", "A1" ).add( "KB", "B" ).add( "KC", "C" ).add( "KA", "A2" ).size must expectsNow( 3, 0 )

          strings.contains( "KA" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KB" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KC" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KD" ) must expectsNow( beFalse, beFalse )

          strings.remove( "KA" ).size must expectsNow( 2, 0 )
          strings.contains( "KA" ) must expectsNow( beFalse, beFalse )
          strings.contains( "KB" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KC" ) must expectsNow( beTrue, beFalse )
          strings.contains( "KD" ) must expectsNow( beFalse, beFalse )

          strings.remove( "KB", "KC" ).size must expectsNow( 0, 0 )
          strings.contains( "KA" ) must expectsNow( beFalse, beFalse )
          strings.contains( "KB" ) must expectsNow( beFalse, beFalse )
          strings.contains( "KC" ) must expectsNow( beFalse, beFalse )
          strings.contains( "KD" ) must expectsNow( beFalse, beFalse )
        }

        "working with the map at non-map key" in {
          implicit val key: Key = s"$prefix-map-invalid"

          cache.set( key.key, "invalid" ) must expectsNow( beUnit )
          strings.add( "KA", "A" ) must expectsNow( throwA[ IllegalArgumentException ], beAnInstanceOf[ RedisMap[ String, AsynchronousResult ] ] )
        }

        "objects in the map" in {
          implicit val key: Key = s"$prefix-map-objects"

          def A = SimpleObject( "A", 1 )

          def B = SimpleObject( "B", 2 )

          def C = SimpleObject( "C", 3 )

          def D = SimpleObject( "D", 4 )

          def E = SimpleObject( "E", 5 )

          objects.size must expectsNow( 0 )
          objects.add( "A", A ).add( "B", B ).add( "C", C ).add( "A", A ).size must expectsNow( 3, 0 )

          objects.contains( "A" ) must expectsNow( beTrue, beFalse )
          objects.contains( "B" ) must expectsNow( beTrue, beFalse )
          objects.contains( "C" ) must expectsNow( beTrue, beFalse )
          objects.contains( "D" ) must expectsNow( beFalse, beFalse )

          objects.remove( "A" ).size must expectsNow( 2, 0 )
          objects.contains( "A" ) must expectsNow( beFalse, beFalse )
          objects.contains( "B" ) must expectsNow( beTrue, beFalse )
          objects.contains( "C" ) must expectsNow( beTrue, beFalse )
          objects.contains( "D" ) must expectsNow( beFalse, beFalse )

          objects.remove( "B", "C", "D" ).size must expectsNow( 0, 0 )
          objects.contains( "A" ) must expectsNow( beFalse, beFalse )
          objects.contains( "B" ) must expectsNow( beFalse, beFalse )
          objects.contains( "C" ) must expectsNow( beFalse, beFalse )
          objects.contains( "D" ) must expectsNow( beFalse, beFalse )
        }
      }
    }
  }

}
