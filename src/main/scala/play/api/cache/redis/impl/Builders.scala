package play.api.cache.redis.impl

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

/**
  * Transforms future result produced by redis implementation to the result of the desired type
  *
  * @author Karel Cemus
  */
object Builders {

  import play.api.cache.redis._
  import play.api.cache.redis.exception._

  import akka.pattern.AskTimeoutException

  trait ResultBuilder[ Result[ X ] ] {
    /** name of the builder used for internal purposes */
    def name: String = this.getClass.getSimpleName
    /** converts future result produced by Redis to the result of desired type */
    def toResult[ T ]( run: => Future[ T ], default: => Future[ T ] )( implicit policy: RecoveryPolicy, context: ExecutionContext, timeout: akka.util.Timeout ): Result[ T ]
    /** show the builder name */
    override def toString = s"ResultBuilder($name)"
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ AsynchronousResult ] {

    override def name = "AsynchronousBuilder"

    override def toResult[ T ]( run: => Future[ T ], default: => Future[ T ] )( implicit policy: RecoveryPolicy, context: ExecutionContext, timeout: akka.util.Timeout ): AsynchronousResult[ T ] =
      run recoverWith {
        // recover from known exceptions
        case failure: RedisException => policy.recoverFrom( run, default, failure )
      }
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ SynchronousResult ] {

    import scala.concurrent.Await
    import scala.util._

    override def name = "SynchronousBuilder"

    override def toResult[ T ]( run: => Future[ T ], default: => Future[ T ] )( implicit policy: RecoveryPolicy, context: ExecutionContext, timeout: akka.util.Timeout ): SynchronousResult[ T ] =
      Try {
        // wait for the result
        Await.result( run, timeout.duration )
      }.recover {
        // it timed out, produce an expected exception
        case cause: AskTimeoutException => timedOut( cause )
      }.recover {
        // apply recovery policy to recover from expected exceptions
        case failure: RedisException => Await.result( policy.recoverFrom( run, default, failure ), timeout.duration )
      }.get // finally, regardless the recovery status, get the synchronous result
  }

}
