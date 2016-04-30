package play.api.cache.redis

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

/** Implicit helpers used within the redis cache implementation. These
  * handful tools simplifies code readability but has no major function.
  *
  * @author Karel Cemus
  */
trait Implicits {

  /** enriches any ref by toFuture converting a value to Future.successful */
  protected implicit class RichFuture[ T ]( any: T ) {
    def toFuture( implicit context: ExecutionContext ) = Future( any )
  }

}
