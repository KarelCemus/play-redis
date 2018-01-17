package play.api.cache.redis.util

import java.util.Date

import play.api.cache.redis.{CacheApi, Expiration, ExpirationImplicits, Redis}

// INFO JodaDateTime is deprecated as for 2018 use ZonedDateTime or LocalDateTime
import org.joda.time.DateTime
import org.specs2.mutable.Specification

/**
  * <p>This specification tests expiration conversion</p>
  */
class ExpirationSpec extends Specification with Redis with ExpirationImplicits {

  private val Cache = injector.instanceOf[ CacheApi ]

  private def nowInJoda = new DateTime( )

  private def nowInJava = new Date( )

  private def in2seconds = nowInJoda.plusSeconds( 2 )

  private val prefix = "expiration"

  "Expiration" should {

    "properly compute positive duration for org.joda.time.DateTime" in {
      val expiration = nowInJoda.plusSeconds( 5 ).asExpiration
      expiration.toSeconds >= 4 must beTrue
      expiration.toSeconds <= 6 must beTrue
    }

    "properly compute positive duration for java.util.Date" in {
      val expiration = new Date( nowInJava.getTime + 5 * 1000 ).asExpiration
      expiration.toSeconds >= 4 must beTrue
      expiration.toSeconds <= 6 must beTrue
    }

    "properly compute negative duration" in {
      val expiration = nowInJoda.minusSeconds( 5 ).asExpiration
      expiration.toSeconds <= -4 must beTrue
      expiration.toSeconds >= -6 must beTrue
    }

    "hit after set" in {
      Cache.set( s"$prefix-test-1", "value", in2seconds.asExpiration )
      Cache.get[ String ]( s"$prefix-test-1" ) must beSome( "value" )
    }

    "miss after expiration" in {
      Cache.set( s"$prefix-test-2", "value", in2seconds.asExpiration )
      Cache.get[ String ]( s"$prefix-test-2" ) must beSome( "value" )
      // wait until the duration expires
      Thread.sleep( 2500 )
      Cache.get[ String ]( s"$prefix-test-2" ) must beNone
    }
  }
}
