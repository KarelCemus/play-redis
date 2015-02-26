package play.cache.redis

import java.util.Date

import scala.concurrent.Await
import scala.concurrent.duration._

import play.api.cache.Cache
import play.api.test._

import org.joda.time.DateTime
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification._

/**
 * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
 */
class RedisCacheAdapterSpec extends Specification with AroundExample {

  lazy val invalidate = {
    // invalidate redis cache for test
    Await.result( play.cache.Cache.invalidate( ), Duration( "5s" ) )
  }

  /** application context to perform operations in */
  protected def application = new FakeApplication( additionalPlugins = Seq(
    "play.api.libs.concurrent.AkkaPlugin",
    "play.cache.redis.RedisCachePlugin",
    "play.cache.redis.RedisCachePlugin20",
    "play.cache.redis.RedisCacheAdapterPlugin"
  ) )

  override def around[ T: AsResult ]( t: => T ): Result = {
    Helpers.running( application ) {
      // internally initialise
      play.cache.Cache.reload( )
      // invalidate redis cache for test
      invalidate
      // run in fake application to let cache working
      AsResult.effectively( t )
    }
  }

  "Cache Adapter" should {

    import play.api.Play.current

    "miss on get" in {
      Cache.get( "test-key-1" ) must beNone
    }

    "hit after set" in {
      Cache.set( "test-key-2", "value" )
      Cache.get( "test-key-2" ) must beSome[ Any ]
      Cache.get( "test-key-2" ) must beSome( "value" )
    }

    "miss after remove" in {
      Cache.set( "test-key-3", "value" )
      Cache.get( "test-key-3" ).isDefined must beTrue
      Cache.remove( "test-key-3" )
      Cache.get( "test-key-3" ) must beNone
    }

    "miss after timeout" in {
      // set
      Cache.set( "test-key-4", "value", 1 )
      Cache.get( "test-key-4" ).isDefined must beTrue
      // wait until it expires
      Thread.sleep( 2000 )
      // miss
      Cache.get( "test-key-4" ) must beNone
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
      Cache.set( "test-int-key", new Date( 123 ) )
      Cache.get( "test-int-key" ) must beSome( new Date( 123 ) )
    }

    "support DateTime" in {
      Cache.set( "test-int-key", new DateTime( 123 ) )
      Cache.get( "test-int-key" ) must beSome( new DateTime( 123 ) )
    }

    "support Boolean" in {
      Cache.set( "test-int-key", true )
      Cache.get( "test-int-key" ) must beSome( true )
    }
  }
}
