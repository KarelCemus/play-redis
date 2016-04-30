package play.api.cache.redis.exception

class UnexpectedResponseException( key: String, command: String ) extends RedisException( command )
