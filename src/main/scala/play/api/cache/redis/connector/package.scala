package play.api.cache.redis

import scala.concurrent.Future
import scala.language.implicitConversions

package object connector {

  implicit def future2expected[T](future: Future[T]): ExpectedFutureBuilder[T] = new ExpectedFutureBuilder[T](future)

  implicit class TupleHelper[+A, +B](val tuple: (A, B)) extends AnyVal {
    @inline def key: A = tuple._1
    @inline def value: B = tuple._2
    @inline def asString = s"$key $value"
    @inline def isNull = value == null
  }

  implicit class StringWhen(val value: String) extends AnyVal {
    def when(condition: Boolean) = if (condition) value else ""
  }
}
