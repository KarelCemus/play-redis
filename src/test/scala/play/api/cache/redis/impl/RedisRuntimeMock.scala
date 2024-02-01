package play.api.cache.redis.impl

import akka.util.Timeout
import org.scalamock.scalatest.AsyncMockFactoryBase
import play.api.cache.redis.{FailThrough, RecoverWithDefault, RecoveryPolicy, RedisException}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

private[impl] trait RedisRuntimeMock { outer: AsyncMockFactoryBase =>

  protected object recoveryPolicy {

    private class RerunPolicy extends RecoveryPolicy {
      override def recoverFrom[T](
         rerun: => Future[T],
         default: => Future[T],
         failure: RedisException
       ): Future[T] = rerun
    }

    val failThrough: RecoveryPolicy = new FailThrough {}
    val default: RecoveryPolicy = new RecoverWithDefault {}
    val rerun: RecoveryPolicy = new RerunPolicy
  }

  protected def redisRuntime(
    invocationPolicy: InvocationPolicy = EagerInvocation,
    recoveryPolicy: RecoveryPolicy = outer.recoveryPolicy.failThrough,
    timeout: FiniteDuration = 200.millis,
    prefix: RedisPrefix = RedisEmptyPrefix,
  ): RedisRuntime = {
    val runtime = mock[RedisRuntime]
    (() => runtime.context).expects().returns(ExecutionContext.global).anyNumberOfTimes()
    (() => runtime.invocation).expects().returns(invocationPolicy).anyNumberOfTimes()
    (() => runtime.prefix).expects().returns(prefix).anyNumberOfTimes()
    (() => runtime.policy).expects().returns(recoveryPolicy).anyNumberOfTimes()
    (() => runtime.timeout).expects().returns(Timeout(timeout)).anyNumberOfTimes()
    runtime
  }
}
