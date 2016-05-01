package play.api.cache.redis.impl

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

import play.api.cache.redis._

/**
  * Transforms future result produced by redis implementation to the result of the desired type
  *
  * @author Karel Cemus
  */
object Builders {

  trait ResultBuilder[ Result[ X ] ] {
    /** converts future result produced by Redis to the result of desired type */
    def toResult[ T ]( value: AsynchronousResult[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): Result[ T ]
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ AsynchronousResult ] {
    override def toResult[ T ]( future: AsynchronousResult[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): AsynchronousResult[ T ] =
      future
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ SynchronousResult ] {
    import scala.concurrent.Await
    override def toResult[ T ]( future: AsynchronousResult[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): SynchronousResult[ T ] =
      Await.result( future, timeout.duration )
  }

}
