package play.api.cache.redis

import scala.concurrent.duration._

/**
  * Provides implicit converters to convert expiration date into duration, which
  * is accepted by CacheApi. The conversion is performed from now, i.e., the
  * formula is:
  *
  * {{{
  * expireAt in seconds - now in seconds = duration in seconds
  * }}}
  */
private[redis] trait ExpirationImplicits {

  import java.time.{LocalDateTime, ZoneId}
  import java.util.Date

  implicit def javaDate2AsExpiration(expireAt: Date): Expiration = new Expiration(expireAt.getTime)

  implicit def java8Date2AsExpiration(expireAt: LocalDateTime): Expiration = new Expiration(expireAt.atZone(ZoneId.systemDefault()).toEpochSecond * 1000)
}

/**
  * computes cache duration from the given expiration date time.
  *
  * @param expireAt
  *   The class accepts timestamp in milliseconds since 1970
  */
class Expiration(val expireAt: Long) extends AnyVal {

  /** returns now in milliseconds */
  private def now: Long = System.currentTimeMillis()

  /**
    * converts given timestamp indication expiration date into duration from now
    */
  def asExpiration: FiniteDuration = (expireAt - now).milliseconds
}
