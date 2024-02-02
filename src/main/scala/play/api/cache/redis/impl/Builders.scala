package play.api.cache.redis.impl

import scala.concurrent.Future

/**
  * Transforms future result produced by redis implementation to the result of the desired type
  */
private object Builders {
  import dsl._
  import play.api.cache.redis._
  import akka.pattern.AskTimeoutException

  trait ResultBuilder[Result[_]] {
    /** name of the builder used for internal purposes */
    def name: String
    /** converts future result produced by Redis to the result of desired type */
    def toResult[T](run: => Future[T], default: => Future[T])(implicit runtime: RedisRuntime): Result[T]
    /** maps the value */
    def map[T, U](result: Result[T])(f: T => U)(implicit runtime: RedisRuntime): Result[U]
    // $COVERAGE-OFF$
    /** show the builder name */
    override def toString: String = s"ResultBuilder($name)"
    // $COVERAGE-ON$
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[AsynchronousResult] {

    def name = "AsynchronousBuilder"

    override def toResult[T](run: => Future[T], default: => Future[T])(implicit runtime: RedisRuntime): AsynchronousResult[T] =
      run recoverWith {
        // recover from known exceptions
        case failure: RedisException => runtime.policy.recoverFrom(run, default, failure)
      }

    override def map[T, U](result: AsynchronousResult[T])(f: T => U)(implicit runtime: RedisRuntime): AsynchronousResult[U] =
      result.map(f)
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[SynchronousResult] {

    import scala.concurrent.Await
    import scala.util._

    def name = "SynchronousBuilder"

    override def toResult[T](run: => Future[T], default: => Future[T])(implicit runtime: RedisRuntime): SynchronousResult[T] =
      Try {
        // wait for the result
        Await.result(run, runtime.timeout.duration)
      }.recover[T] {
        // it timed out, produce an expected exception
        case cause: AskTimeoutException                   => timedOut(cause)
        case cause: java.util.concurrent.TimeoutException => timedOut(cause)
      }.recover {
        // apply recovery policy to recover from expected exceptions
        case failure: RedisException => Await.result(runtime.policy.recoverFrom(run, default, failure), runtime.timeout.duration)
      }.get // finally, regardless the recovery status, get the synchronous result

    override def map[T, U](result: SynchronousResult[T])(f: T => U)(implicit runtime: RedisRuntime): SynchronousResult[U] =
      f(result)
  }
}
