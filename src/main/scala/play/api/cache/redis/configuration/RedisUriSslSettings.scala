package play.api.cache.redis.configuration

import com.typesafe.config.Config
import play.api.cache.redis.configuration.RedisConfigLoader.{ConfigOption, ConfigPath}
import play.api.cache.redis.configuration.RedisSslSettings.VerifyPeerMode

trait RedisUriSslSettings {
  def enabled: Boolean
  def verifyPeerMode: VerifyPeerMode
}

object RedisUriSslSettings {

  def requiredDefault: RedisUriSslSettings = RedisUriSslSettingsImpl(false, VerifyPeerMode.NONE)
  final case class RedisUriSslSettingsImpl(enabled: Boolean, verifyPeerMode: VerifyPeerMode) extends RedisUriSslSettings

  def load(config: Config, path: String)(default: RedisUriSslSettings): RedisUriSslSettings = RedisUriSslSettingsImpl(
    enabled = config.getOption(path / "ssl" / "enabled", _.getBoolean).getOrElse(default.enabled),
    verifyPeerMode = VerifyPeerMode.getOpt(config, path / "ssl" / "verify-peer-mode").getOrElse(default.verifyPeerMode),
  )

}
