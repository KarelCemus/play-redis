package play.api.cache.redis.test

import play.api.cache.redis.configuration._

final case class RedisSettingsTest(
  invocationContext: String,
  invocationPolicy: String,
  timeout: RedisTimeouts,
  recovery: String,
  source: String,
  prefix: Option[String] = None,
  threadPool: RedisThreadPools,
  sslSettings: Option[RedisSslSettings] = None,
) extends RedisSettings
