package play.api.cache.redis.configuration

import play.api.cache.redis.configuration.RedisSslSettings.VerifyPeerMode.{CA, NONE}
import play.api.cache.redis.configuration.RedisUriSslSettings.RedisUriSslSettingsImpl
import play.api.cache.redis.test.{Helpers, ImplicitOptionMaterialization, UnitSpec}

class RedisUriSslSettingsSpec extends UnitSpec with ImplicitOptionMaterialization {

  "ssl uri settings" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis.ssl {
        |  enabled: true
        |  verify-peer-mode: ca
        |}
      """.stripMargin
    }

    RedisUriSslSettings.load(configuration.underlying, "play.cache.redis")(RedisUriSslSettings.requiredDefault) mustEqual RedisUriSslSettingsImpl(
      enabled = true,
      verifyPeerMode = CA,
    )
  }

  "default settings" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis {}""".stripMargin
    }

    RedisUriSslSettings.load(configuration.underlying, "play.cache.redis")(RedisUriSslSettings.requiredDefault) mustEqual RedisUriSslSettingsImpl(
      enabled = false,
      verifyPeerMode = NONE,
    )
  }

}
