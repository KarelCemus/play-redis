package play.api.cache.redis.impl

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Transforms future result produced by redis implementation to the result of the desired type
  *
  * @author Karel Cemus
  */
object Builders {
  import dsl._
  import play.api.cache.redis._
  import akka.pattern.AskTimeoutException

  trait ResultBuilder[ Result[ X ] ] {
    /** name of the builder used for internal purposes */
    def name: String = this.getClass.getSimpleName
    /** converts future result produced by Redis to the result of desired type */
    def toResult[ T ]( run: => Future[ T ], default: => Future[ T ] )( implicit runtime: RedisRuntime ): Result[ T ]
    /** show the builder name */
    override def toString = s"ResultBuilder($name)"
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ AsynchronousResult ] {

    override def name = "AsynchronousBuilder"

    override def toResult[ T ]( run: => Future[ T ], default: => Future[ T ] )( implicit runtime: RedisRuntime ): AsynchronousResult[ T ] =
      run recoverWith {
        // recover from known exceptions
        case failure: RedisException => runtime.policy.recoverFrom( run, default, failure )
      }
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ SynchronousResult ] {

    // INFO Await in reality does fiber fork and can result kernel panic if used
    // recklessly
    import scala.concurrent.Await
    import scala.util._

    override def name = "SynchronousBuilder"

    override def toResult[ T ]( run: => Future[ T ], default: => Future[ T ] )( implicit runtime: RedisRuntime ): SynchronousResult[ T ] =
      Try {
        // wait for the result
        Await.result( run, runtime.timeout.duration )
      }.recover {
        // it timed out, produce an expected exception
        case cause: AskTimeoutException => timedOut( cause )
      }.recover {
        // apply recovery policy to recover from expected exceptions
        case failure: RedisException => Await.result( runtime.policy.recoverFrom( run, default, failure ), runtime.timeout.duration )
      }.get // finally, regardless the recovery status, get the synchronous result
  }
}
