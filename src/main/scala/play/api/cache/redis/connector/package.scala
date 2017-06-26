package play.api.cache.redis

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * @author Karel Cemus
  */
package object connector {

  implicit def future2expected[ T ]( future: Future[ T ] ): ExpectedFutureBuilder[ T ] = new ExpectedFutureBuilder[ T ]( future )

  implicit class TupleHelper[ +A, +B ]( val tuple: (A, B) ) extends AnyVal {
    @inline def key: A = tuple._1
    @inline def value: B = tuple._2
    @inline def asString = s"$key $value"
    @inline def isNull = value == null
  }
}
