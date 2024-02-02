package play.api.cache.redis

import scala.concurrent.Future

package object connector {

  implicit def future2expected[T](future: Future[T]): ExpectedFutureBuilder[T] = new ExpectedFutureBuilder[T](future)

  implicit class TupleHelper[+A, +B](private val tuple: (A, B)) extends AnyVal {
    @inline def key: A = tuple._1
    @inline def value: B = tuple._2
    @inline def asString: String = s"$key $value"
    @inline def isNull: Boolean = Option(value).isEmpty
  }

  implicit class StringWhen(private val value: String) extends AnyVal {
    def when(condition: Boolean): String = if (condition) value else ""
  }
}
