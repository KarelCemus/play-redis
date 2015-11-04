package play.api.cache.redis

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.util._

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.Timeout
import brando.Request

/** Implicit helpers used within the redis cache implementation. These
  * handful tools simplifies code readability but has no major function.
  *
  * @author Karel Cemus
  */
trait Implicits {

  /** converts java.time.Duration into scala.concurrent.duration.Duration */
  protected implicit def asScalaDuration( duration: java.time.Duration ): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos( duration.toNanos )

  /** rich akka actor providing additional functionality and syntax sugar */
  protected implicit class RedisRef( brando: ActorRef ) {
    /** actor handler */
    private val actor = new AskableActorRef( brando )

    /** syntax sugar for querying the storage */
    def ?( request: Request )( implicit timeout: Timeout, context: ExecutionContext ): Future[ Any ] = actor ask request map Success.apply recover {
      case ex => Failure( ex ) // execution failed, recover
    }
  }
}
