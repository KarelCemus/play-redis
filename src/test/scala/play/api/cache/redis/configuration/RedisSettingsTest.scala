package play.api.cache.redis.configuration

/**
  * @author Karel Cemus
  */
case class RedisSettingsTest
(
  invocationContext: String,
  invocationPolicy: String,
  timeout: RedisTimeouts,
  recovery: String,
  source: String,
  prefix: Option[ String ] = None

) extends RedisSettings
