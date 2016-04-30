package play.api.cache.redis

/**
  * Helper trait providing simplified and unified API to exception handling in play-redis
  *
  * @author Karel Cemus
  */
trait Exceptions {

  /** helper throwing UnsupportedOperationException */
  @throws[ UnsupportedOperationException ]
  def unsupported( message: String ): Nothing =
    throw new UnsupportedOperationException( message )

  /** helper indicating serialization failure, it throws an exception */
  @throws[ SerializationException ]
  def serializationFailed( key: String, message: String, cause: Throwable ) =
    throw new SerializationException( key, message )

  //  def timedOut( key: String ) = throw new TimeoutException( key )

  def unexpected( key: String, command: String ): Nothing =
    throw new UnexpectedResponseException( key, command )

  def failed( key: String, command: String, cause: Throwable ): Nothing =
    throw new ExecutionFailedException( key, command, cause )

}

/**
  * Generic exception produced by the library indicating internal failure
  *
  * @author Karel Cemus
  */
abstract sealed class RedisException( message: String, cause: Throwable ) extends RuntimeException( message, cause ) {

  def this( message: String ) = this( message, null )
}

/**
  * Value serialization or deserialization failed.
  *
  * @param message
  */
class SerializationException( value: Any, message: String ) extends RedisException( message )


class TimeoutException( key: String ) extends RedisException( "" )

class ExecutionFailedException( key: String, command: String, cause: Throwable ) extends RedisException( command, cause )

class UnexpectedResponseException( key: String, command: String ) extends RedisException( command )
