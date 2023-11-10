package play.api.cache.redis

import org.specs2.mutable.Specification

import java.time.Instant
import java.util.Date
import scala.concurrent.duration._

/**
  * <p>This specification tests expiration conversion</p>
  */
class ExpirationSpec extends Specification {

  "Expiration" should {

    val expireAt: Instant = Instant.now().plusSeconds(5.minutes.toSeconds).plusSeconds(30)

    val expiration = 5.minutes + 30.seconds
    val expirationFrom = expiration - 2.second
    val expirationTo = expiration + 1.second

    "from java.util.Date" in {
      new Date(expireAt.toEpochMilli).asExpiration must beBetween(expirationFrom, expirationTo)
    }

    "from java.time.LocalDateTime" in {
      import java.time._
      LocalDateTime.ofInstant(expireAt, ZoneId.systemDefault()).asExpiration must beBetween(expirationFrom, expirationTo)
    }
  }
}
