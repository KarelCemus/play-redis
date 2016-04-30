package play.api.cache.redis.exception

/**
  * @author Karel Cemus
  */
class ExecutionFailedException( key: String, command: String, cause: Throwable ) extends RedisException( command, cause )
