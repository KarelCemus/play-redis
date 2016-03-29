package play.api.cache.redis

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
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

  /** waits for future responses and returns them synchronously */
  protected implicit class Synchronizer[ T ]( future: Future[ T ] ) {
    def sync( implicit timeout: Duration ) = Await.result( future, timeout )
  }

  /** Transforms the promise into desired builder results */
  protected implicit def build[ T, Result[ _ ] ]( value: Future[ T ] )( implicit builder: Builders.ResultBuilder[ Result ], context: ExecutionContext, timeout: akka.util.Timeout ) =
    builder.toResult( value )
}
