package play.api.cache.redis.connector

import play.api.cache.redis._

import scala.concurrent.{ExecutionContext, Future}

/**
  * The extended future implements advanced response handling.
  * It unifies maintenance of unexpected responses
  */
private[connector] trait ExpectedFuture[T] {

  protected def future: Future[T]

  protected def cmd: String

  /** received an unexpected response */
  protected def onUnexpected: PartialFunction[Any, Nothing]

  protected def onFailed(ex: Throwable): Nothing

  /** execution failed with an exception */
  private def onException: PartialFunction[Throwable, Nothing] = {
    case ex: RedisException => throw ex
    case ex                 => onFailed(ex)
  }

  /** invokes logging statements when the expected future is completed */
  def logging(doLogging: PartialFunction[T, Unit])(implicit context: ExecutionContext): Future[T] = {
    future foreach doLogging
    future recover onException
  }

  /** handles both expected and unexpected responses and failure recovery */
  def expects[U](expected: PartialFunction[T, U])(implicit context: ExecutionContext): Future[U] = {
    future map (expected orElse onUnexpected) recover onException
  }
}

private[connector] object ExpectedFuture {

  /** converts future to Future[Unit] */
  @inline implicit def futureToUnit[T](future: Future[T])(implicit context: ExecutionContext): Future[Unit] = future.map(_ => ())
}

private[connector] class ExpectedFutureWithoutKey[T](protected val future: Future[T], protected val cmd: String) extends ExpectedFuture[T] {

  protected def onUnexpected: PartialFunction[Any, Nothing] = {
    case _ => unexpected(None, cmd)
  }

  protected def onFailed(ex: Throwable): Nothing =
    failed(None, cmd, cmd, ex)

  def withKey(key: String): ExpectedFutureWithKey[T] = new ExpectedFutureWithKey[T](future, cmd, key, s"$cmd $key")

  def withKeys(keys: Iterable[String]): ExpectedFutureWithKey[T] = withKey(keys mkString " ")

  override def toString: String = s"ExpectedFuture($cmd)"
}

private[connector] class ExpectedFutureWithKey[T](protected val future: Future[T], protected val cmd: String, key: String, statement: => String) extends ExpectedFuture[T] {

  protected def onUnexpected: PartialFunction[Any, Nothing] = {
    case _ => unexpected(Some(key), cmd)
  }

  protected def onFailed(ex: Throwable): Nothing =
    failed(Some(key), cmd, statement, ex)

  def andParameter(param: => Any): ExpectedFutureWithKey[T] = andParameters(param.toString)

  def andParameters(params: Iterable[Any]): ExpectedFutureWithKey[T] = andParameters(params mkString " ")

  def andParameters(params: => String): ExpectedFutureWithKey[T] = new ExpectedFutureWithKey(future, cmd, key, s"$statement $params")

  def asCommand(commandOverride: => String) = new ExpectedFutureWithKey(future, cmd, key, s"$cmd $commandOverride")

  override def toString: String = s"ExpectedFuture($statement)"
}

/**
  * Constructs expected future from provided parameters, this serves as syntax sugar
  */
private[connector] class ExpectedFutureBuilder[T](val future: Future[T]) extends AnyVal {

  def executing(cmd: String): ExpectedFutureWithoutKey[T] = new ExpectedFutureWithoutKey[T](future, cmd)
}
