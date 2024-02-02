package play.api.cache.redis

/**
  * Generic exception produced by the library indicating internal failure
  */
sealed abstract class RedisException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}

/**
  * Request timeouts
*/
final case class TimeoutException(
                                   cause: Throwable) extends RedisException("Command execution timed out", cause)

/**
 * Command execution failed with exception
 */
final case class ExecutionFailedException(
                                           key: Option[String], command: String, statement: String, cause: Throwable) extends RedisException(s"Execution of '$command'${key.map(key => s" for key '$key'") getOrElse ""} failed", cause)

/**
 * Request succeeded but returned unexpected value
 */
final case class UnexpectedResponseException(
                                              key: Option[String], command: String) extends RedisException(s"Command '$command'${key.map(key => s" for key '$key'") getOrElse ""} returned unexpected response")

/**
 * Value serialization or deserialization failed.
 */
final case class SerializationException(
                                         key: String, message: String, cause: Throwable) extends RedisException(s"$message for $key", cause)

/**
  * Helper trait providing simplified and unified API to exception handling in play-redis
  */
trait ExceptionImplicits {

  /** helper throwing UnsupportedOperationException */
  @throws[UnsupportedOperationException]
  def unsupported(message: String): Nothing =
    throw new UnsupportedOperationException(message)

  /** helper indicating serialization failure, it throws an exception */
  @throws[SerializationException]
  def serializationFailed(key: String, message: String, cause: Throwable): Nothing =
    throw SerializationException(key, message, cause)

  /** helper indicating  command execution timed out */
  @throws[TimeoutException]
  def timedOut(cause: Throwable): Nothing =
    throw TimeoutException(cause)

  /** helper indicating the command execution returned unexpected exception */
  @throws[UnexpectedResponseException]
  def unexpected(key: Option[String], command: String): Nothing =
    throw UnexpectedResponseException(key, command)

  /** helper indicating command execution failed with exception */
  @throws[ExecutionFailedException]
  def failed(key: Option[String], command: String, statement: String, cause: Throwable): Nothing =
    throw ExecutionFailedException(key, command, statement, cause)

  /** helper indicating invalid configuration */
  @throws[IllegalStateException]
  def invalidConfiguration(message: String): Nothing =
    throw new IllegalStateException(message)
}
