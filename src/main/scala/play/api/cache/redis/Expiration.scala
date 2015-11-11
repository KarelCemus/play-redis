package play.api.cache.redis

import java.util.Date

import scala.concurrent.duration._
import scala.language.implicitConversions

import org.joda.time.DateTime

/**
  * Provides implicit converters to convert expiration date into duration, which is accepted by CacheApi.
  * The conversion is performed from now, i.e., the formula is:
  *
  * {{{
  * expireAt in seconds - now in seconds = duration in seconds
  * }}}
  *
  * @author Karel Cemus
  */
trait Expiration {

  /**
    * converts given timestamp indication expiration date into duration from now
    *
    * @param expireAt The class accepts timestamp in milliseconds since 1970
    */
  class AsExpiration private( expireAt: Long ) {

    def this( expireAt: DateTime ) = this( expireAt.getMillis )

    def this( expireAt: Date ) = this( expireAt.getTime )

    /** returns now in milliseconds */
    private def now = new Date( ).getTime

    /** converts given timestamp indication expiration date into duration from now */
    def asExpiration = ( expireAt - now ).milliseconds
  }

  implicit def javaDate2AsExpiration( expireAt: Date ): AsExpiration = new AsExpiration( expireAt )

  implicit def jodaDate2AsExpiration( expireAt: DateTime ): AsExpiration = new AsExpiration( expireAt )
}
