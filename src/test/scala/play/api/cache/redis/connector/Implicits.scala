package play.api.cache.redis.connector

import scala.concurrent._
import scala.concurrent.duration._

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
    def after[ T ]( seconds: Int, value: T )( implicit system: ActorSystem, ec: ExecutionContext ): Future[ T ] = {
      val promise = Promise[ T ]()
      // after a timeout, resolve the promise
      akka.pattern.after( seconds.seconds, system.scheduler ) {
        promise.success( value )
        promise.future
      }
      // return the promise
      promise.future
    }

    def after( seconds: Int )( implicit system: ActorSystem, ec: ExecutionContext ): Future[ Unit ] = {
      after( seconds, Unit )
    }
  }

}
