package play.api.cache.redis.exception

/**
  * Generic exception produced by the library indicating internal failure
  *
  * @author Karel Cemus
  */
abstract class RedisException( message: String, cause: Throwable ) extends RuntimeException( message, cause ) {

  def this( message: String ) = this( message, null )
}
