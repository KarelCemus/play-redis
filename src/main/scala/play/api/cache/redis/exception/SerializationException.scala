package play.api.cache.redis.exception

/**
  * Value serialization or deserialization failed.
  *
  * @param message
  */
class SerializationException( value: Any, message: String ) extends RedisException( message )
