package play.api.cache.redis.connector

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import akka.actor.ActorSystem

/**
  * @author Karel Cemus
  */
object Implicits extends play.api.cache.redis.TestImplicits {

  private[ connector ] implicit class FutureAwait[ T ]( val future: Future[ T ] ) extends AnyVal {
    def await = Await.result( future, 2.minutes )
  }

  implicit class RichFutureObject( val future: Future.type ) extends AnyVal {
    /** returns a future resolved in given number of seconds */
    def after( seconds: Int )( implicit system: ActorSystem, ec: ExecutionContext ): Future[ Unit ] = {
      val promise = Promise[ Unit ]()
      // after a timeout, resolve the promise
      akka.pattern.after( seconds.seconds, system.scheduler ) {
        promise.success( Unit )
        promise.future
      }
      // return the promise
      promise.future
    }
  }
}
