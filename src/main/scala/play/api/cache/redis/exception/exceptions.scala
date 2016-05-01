package play.api.cache.redis.exception

/**
  * Generic exception produced by the library indicating internal failure
  *
  * @author Karel Cemus
  */
sealed abstract class RedisException( message: String, cause: Throwable ) extends RuntimeException( message, cause ) {

  def this( message: String ) = this( message, null )
}

/**
  * @author Karel Cemus
  */
class TimeoutException( key: String ) extends RedisException( "" )

/**
  * @author Karel Cemus
  */
class ExecutionFailedException( key: String, command: String, cause: Throwable ) extends RedisException( command, cause )

class UnexpectedResponseException( key: String, command: String ) extends RedisException( command )

/** Value serialization or deserialization failed. */
class SerializationException( value: Any, message: String ) extends RedisException( message )
