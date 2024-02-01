package play.api.cache.redis.configuration

import play.api.cache.redis._

private[configuration] object Equals {

  // $COVERAGE-OFF$
  @inline
  def check[T](a: T, b: T)(property: (T => Any)*): Boolean = {
    property.forall(property => property(a) === property(b))
  }
  // $COVERAGE-ON$
}
