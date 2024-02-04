package play.api.cache.redis.test

import play.api.cache.redis.TimeoutException

import scala.util.control.NoStackTrace

sealed class SimulatedException extends RuntimeException("Simulated failure.") with NoStackTrace {
  def asRedis: TimeoutException = TimeoutException(this)
}

object SimulatedException extends SimulatedException
