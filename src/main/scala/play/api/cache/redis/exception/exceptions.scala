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
  * Request timeouts
  *
  * @author Karel Cemus
  */
case class TimeoutException( cause: Throwable ) extends RedisException( "" )

/**
  * Command execution failed with exception
  *
  * @author Karel Cemus
  */
case class ExecutionFailedException( key: Option[ String ], command: String, cause: Throwable ) extends RedisException( command, cause )

/**
  * Request succeeded but returned unexpected value
  *
  * @author Karel Cemus
  */
case class UnexpectedResponseException( key: Option[ String ], command: String ) extends RedisException( command )

/**
  * Value serialization or deserialization failed.
  *
  * @author Karel Cemus
  */
case class SerializationException( key: String, message: String, cause: Throwable ) extends RedisException( message )
