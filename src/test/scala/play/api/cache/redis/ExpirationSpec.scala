package play.api.cache.redis

import java.util.Date

import scala.concurrent.duration._

import org.joda.time._
import org.specs2.mutable.Specification

/**
  * <p>This specification tests expiration conversion</p>
  */
class ExpirationSpec extends Specification {

  "Expiration" should {

    def expireAt = DateTime.now().plusMinutes( 5 ).plusSeconds( 30 )

    val expiration = 5.minutes + 30.seconds
    val expirationFrom = expiration - 1.second
    val expirationTo = expiration + 1.second

    "from java.util.Date" in {
      new Date( expireAt.getMillis ).asExpiration must beBetween( expirationFrom, expirationTo )
    }

    "from org.joda.time.DateTime (deprecated)" in {
      expireAt.asExpiration must beBetween( expirationFrom, expirationTo )
    }

    "from java.time.LocalDateTime" in {
      import java.time._
      LocalDateTime.ofInstant( expireAt.toInstant.toDate.toInstant, ZoneId.systemDefault() ).asExpiration must beBetween( expirationFrom, expirationTo )
    }
  }
}
