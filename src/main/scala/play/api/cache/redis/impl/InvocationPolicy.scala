package play.api.cache.redis.impl

import scala.concurrent.{ExecutionContext, Future}

/**
  * Invocation policy implements whether to wait for the operation result or not.
  * This applies only in the limited number of operations. The best examples are `getOrElse`
  * and `getOrFuture`. First, both methods invoke `get`, then, if missed, compute `orElse` clause.
  * Finally, there is the invocation of `set`, however, in some scenarios, there is not required to
  * wait for the result of `set` operation. The value can be returned earlier. This is the difference
  * between `Eager` (not waiting) and `Lazy` (waiting) invocation policies.
  */
sealed trait InvocationPolicy {
  def invoke[T](f: => Future[Any], thenReturn: T)(implicit context: ExecutionContext): Future[T]
}

object EagerInvocation extends InvocationPolicy {
  def invoke[T](f: => Future[Any], thenReturn: T)(implicit context: ExecutionContext) = { f; Future successful thenReturn }
}

object LazyInvocation extends InvocationPolicy {
  def invoke[T](f: => Future[Any], thenReturn: T)(implicit context: ExecutionContext) = f.map(_ => thenReturn)
}
