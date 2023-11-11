package play.api.cache.redis.test

import java.util.concurrent.CompletionStage
import scala.concurrent.Future
import scala.jdk.FutureConverters.FutureOps


final class OrElseProbe[T](queue: LazyList[T]) {

  private var called: Int = 0
  private var next = queue

  def calls: Int = called

  def execute(): T = {
    called += 1
    val result = next.head
    next = next.tail
    result
  }
}

object OrElseProbe {

  def const[T](value: T): OrElseProbe[T] =
    new OrElseProbe(LazyList.continually(value))

  def async[T](value: T): OrElseProbe[Future[T]] =
    new OrElseProbe(LazyList.continually(Future.successful(value)))

  def asyncJava[T](value: T): OrElseProbe[CompletionStage[T]] =
    new OrElseProbe(LazyList.continually(Future.successful(value).asJava))

  def failing[T](exception: Throwable): OrElseProbe[Future[T]] =
    new OrElseProbe(LazyList.continually(Future.failed(exception)))

  def generic[T](values: T*): OrElseProbe[T] =
    new OrElseProbe(LazyList(values: _*))
}
