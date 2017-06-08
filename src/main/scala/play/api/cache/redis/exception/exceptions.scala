package play.api.cache.redis.exception

/**
  * Generic exception produced by the library indicating internal failure
  *
  * @author Karel Cemus
  */
abstract class RedisException( message: String, cause: Throwable ) extends RuntimeException( message, cause ) {
  def this( message: String ) = this( message, null )
}

/**
  * Request timeouts
  *
  * @author Karel Cemus
  */
case class TimeoutException( cause: Throwable ) extends RedisException( "Command execution timed out", cause )

/**
  * Command execution failed with exception
  *
  * @author Karel Cemus
  */
case class ExecutionFailedException( key: Option[ String ], command: String, cause: Throwable ) extends RedisException( s"Execution of '$command'${ key.map( key => s" for key '$key'" ) getOrElse "" } failed", cause )

/**
  * Request succeeded but returned unexpected value
  *
  * @author Karel Cemus
  */
case class UnexpectedResponseException( key: Option[ String ], command: String ) extends RedisException( s"Command '$command'${ key.map( key => s" for key '$key'" ) getOrElse "" } returned unexpected response" )

/**
  * Value serialization or deserialization failed.
  *
  * @author Karel Cemus
  */
case class SerializationException( key: String, message: String, cause: Throwable ) extends RedisException( s"$message for $key", cause )
