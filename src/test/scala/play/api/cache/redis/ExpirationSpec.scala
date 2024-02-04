package play.api.cache.redis

import play.api.cache.redis.test._

import java.time.Instant
import java.util.Date
import scala.concurrent.duration._

/** <p>This specification tests expiration conversion</p> */
class ExpirationSpec extends UnitSpec {

  "Expiration" should {

    val expireAt: Instant = Instant.now().plusSeconds(5.minutes.toSeconds).plusSeconds(30)

    val expiration = 5.minutes + 30.seconds
    val expirationFrom = expiration - 2.second
    val expirationTo = expiration + 1.second

    "from java.util.Date" in {
      val expiration = new Date(expireAt.toEpochMilli).asExpiration
      expiration mustBe >=(expirationFrom)
      expiration mustBe <=(expirationTo)
    }

    "from java.time.LocalDateTime" in {
      import java.time._
      val expiration = LocalDateTime.ofInstant(expireAt, ZoneId.systemDefault()).asExpiration
      expiration mustBe >=(expirationFrom)
      expiration mustBe <=(expirationTo)
    }
  }

}
