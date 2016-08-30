package play.api.cache.redis

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * @author Karel Cemus
  */
package object connector {

  implicit def future2expected[ T ]( future: Future[ T ] ): ExpectedFutureBuilder[ T ] = new ExpectedFutureBuilder[ T ]( future )
}
