package play.api.cache.redis.impl

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

/** Implicit helpers used within the redis cache implementation. These
  * handful tools simplifies code readability but has no major function.
  *
  * @author Karel Cemus
  */
private[ impl ] trait Implicits {

  /** enriches any ref by toFuture converting a value to Future.successful */
  protected implicit class RichFuture[ T ]( any: T ) {
    def toFuture( implicit context: ExecutionContext ) = Future( any )
  }

  /** Transforms the promise into desired builder results */
  protected implicit def build[ T, Result[ _ ] ]( value: Future[ T ] )( implicit builder: Builders.ResultBuilder[ Result ], context: ExecutionContext, timeout: akka.util.Timeout ) =
    builder.toResult( value )
}
