package play.api.cache.redis.configuration

import com.typesafe.config.Config
import play.api.cache.redis.configuration.RedisConfigLoader.{ConfigOption, ConfigPath}
import play.api.cache.redis.configuration.RedisSslSettings.VerifyPeerMode

trait RedisUriSslSettings {
  def enabled: Boolean
  def verifyPeerMode: VerifyPeerMode
}

object RedisUriSslSettings {

  def requiredDefault: RedisUriSslSettings = new RedisUriSslSettings {
    override val enabled: Boolean = false
    override val verifyPeerMode: VerifyPeerMode = VerifyPeerMode.NONE
  }

  @inline def apply(_enabled: Boolean, _verifyPeerMode: VerifyPeerMode): RedisUriSslSettings =
    new RedisUriSslSettings {
      override val enabled: Boolean = _enabled
      override val verifyPeerMode: VerifyPeerMode = _verifyPeerMode
    }

  final case class RedisUriSslSettingsImpl(enabled: Boolean, verifyPeerMode: VerifyPeerMode) extends RedisUriSslSettings

  def load(config: Config, path: String)(default: RedisUriSslSettings): RedisUriSslSettings = RedisUriSslSettingsImpl(
    enabled = config.getOption(path / "ssl" / "enabled", _.getBoolean).getOrElse(default.enabled),
    verifyPeerMode = VerifyPeerMode.getOpt(config, path / "ssl" / "verify-peer-mode").getOrElse(default.verifyPeerMode),
  )

}
