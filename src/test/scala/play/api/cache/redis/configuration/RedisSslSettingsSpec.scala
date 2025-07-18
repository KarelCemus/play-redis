package play.api.cache.redis.configuration

import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.ConfigLoader
import play.api.cache.redis.configuration.RedisSslSettings.SslResource.FileResource
import play.api.cache.redis.configuration.RedisSslSettings.{FactoryDefinition, KeyManagerDefinition, KeyStoreDefinition, RedisSslSettingsImpl, SslResource, TrustManagerDefinition, TrustStoreDefinition}
import play.api.cache.redis.test._

class RedisSslSettingsSpec extends UnitSpec with ImplicitOptionMaterialization {
  implicit private val loader: ConfigLoader[RedisSslSettings] = RedisSslSettings

  "ssl settings only with required fields with file trust-store" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis.ssl-settings {
        |  protocols  : ["TLSv1.2", "TLSv1.3"]
        |  trust-store: {
        |    file: {
        |      path: /abc/def
        |    }
        |    password: 1234
        |  }
        |}
      """.stripMargin
    }

    configuration.get[RedisSslSettings]("play.cache.redis") mustEqual RedisSslSettingsImpl(
      protocols = List("TLSv1.2", "TLSv1.3"),
      trustStore = TrustStoreDefinition(
        resource = SslResource.FileResource("/abc/def"),
        password = Some("1234"),
      ),
      keyStoreType = None,
      cipherSuites = None,
      handShakeTimeout = None,
      keyStore = None,
      keyManager = None,
      trustManager = None,
      verifyPeerMode = None,
    )
  }

  "ssl settings only with required fields with url trust-store" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis.ssl-settings {
        |  protocols  : ["TLSv1.2", "TLSv1.3"]
        |  trust-store: {
        |    url: {
        |      path: /abc/def
        |    }
        |    password: 1234
        |  }
        |}
      """.stripMargin
    }

    configuration.get[RedisSslSettings]("play.cache.redis") mustEqual RedisSslSettingsImpl(
      protocols = List("TLSv1.2", "TLSv1.3"),
      trustStore = TrustStoreDefinition(
        resource = SslResource.UrlResource("/abc/def"),
        password = Some("1234"),
      ),
      keyStoreType = None,
      cipherSuites = None,
      handShakeTimeout = None,
      keyStore = None,
      keyManager = None,
      trustManager = None,
      verifyPeerMode = None,
    )
  }

  "ssl settings only with required fields, but without password" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis.ssl-settings {
        |  protocols  : ["TLSv1.2", "TLSv1.3"]
        |  trust-store: {
        |    file: {
        |      path: /abc/def
        |    }
        |  }
        |}
      """.stripMargin
    }

    configuration.get[RedisSslSettings]("play.cache.redis") mustEqual RedisSslSettingsImpl(
      protocols = List("TLSv1.2", "TLSv1.3"),
      trustStore = TrustStoreDefinition(
        resource = SslResource.FileResource("/abc/def"),
        password = None,
      ),
      keyStoreType = None,
      cipherSuites = None,
      handShakeTimeout = None,
      keyStore = None,
      keyManager = None,
      trustManager = None,
      verifyPeerMode = None,
    )
  }

  "ssl-settings with file-resource for key-manager" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis.ssl-settings {
        |  protocols  : ["TLSv1.2", "TLSv1.3"]
        |  trust-store: {
        |    file: {
        |      path: /abc/def
        |    }
        |  }
        |  key-manager {
        |    resource {
        |      key-cert-chain {
        |        file {
        |          path: pathToKeyCertChainFile
        |        }
        |      }
        |      key {
        |        file {
        |          path: pathToKeyFile
        |        }
        |      }
        |    }
        |    password: keyManagerPassword
        |  }
        |}
      """.stripMargin
    }

    configuration.get[RedisSslSettings]("play.cache.redis") mustEqual RedisSslSettingsImpl(
      protocols = List("TLSv1.2", "TLSv1.3"),
      trustStore = TrustStoreDefinition(
        resource = SslResource.FileResource("/abc/def"),
        password = None,
      ),
      keyStoreType = None,
      cipherSuites = None,
      handShakeTimeout = None,
      keyStore = None,
      keyManager = Some(
        KeyManagerDefinition(
          Right(
            KeyManagerDefinition.Resource(
              keyCertChain = SslResource.FileResource("pathToKeyCertChainFile"),
              key = SslResource.FileResource("pathToKeyFile"),
            ),
          ),
          "keyManagerPassword",
        ),
      ),
      trustManager = None,
      verifyPeerMode = None,
    )
  }

  "ssl-settings with file-resource for trust-manager" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis.ssl-settings {
        |  protocols  : ["TLSv1.2", "TLSv1.3"]
        |  trust-store: {
        |    file: {
        |      path: /abc/def
        |    }
        |  }
        |  trust-manager {
        |    file {
        |      path: pathToTrustManagerFile
        |    }
        |  }
        |}
      """.stripMargin
    }

    configuration.get[RedisSslSettings]("play.cache.redis") mustEqual RedisSslSettingsImpl(
      protocols = List("TLSv1.2", "TLSv1.3"),
      trustStore = TrustStoreDefinition(
        resource = SslResource.FileResource("/abc/def"),
        password = None,
      ),
      keyStoreType = None,
      cipherSuites = None,
      handShakeTimeout = None,
      keyStore = None,
      keyManager = None,
      trustManager = Some(
        TrustManagerDefinition(
          Right(
            SslResource.FileResource("pathToTrustManagerFile"),
          ),
        ),
      ),
      verifyPeerMode = None,
    )
  }

  "ssl-settings with all fields" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis.ssl-settings {
        |  protocols  : ["TLSv1.2", "TLSv1.3"]
        |  trust-store: {
        |    file: {
        |      path: /abc/def
        |    }
        |    password: 1234
        |  }
        |  key-store-type: defaultKeyStoreType
        |  cipher-suites : [firstCipherSuite, secondCipherSuite]
        |  hand-shake-timeout: 100 millis
        |  key-store {
        |    file {
        |      path: keyStoreFile
        |    }
        |    password: keyStorePassword
        |  }
        |  key-manager {
        |    factory {
        |      algorithm: keyManagerAlgorithm
        |      provider: keyManagerProvider
        |    }
        |    password: keyManagerPassword
        |  }
        |  trust-manager {
        |    factory {
        |      algorithm: trustManagerAlgorithm
        |      provider: trustManagerProvider
        |    }
        |  },
        |  verify-peer-mode: ca
        |}
      """.stripMargin
    }

    configuration.get[RedisSslSettings]("play.cache.redis") mustEqual RedisSslSettingsImpl(
      protocols = List("TLSv1.2", "TLSv1.3"),
      trustStore = TrustStoreDefinition(
        resource = SslResource.FileResource("/abc/def"),
        password = Some("1234"),
      ),
      keyStoreType = Some("defaultKeyStoreType"),
      cipherSuites = Some(List("firstCipherSuite", "secondCipherSuite")),
      handShakeTimeout = Some(100.millis),
      keyStore = Some(
        KeyStoreDefinition(
          FileResource("keyStoreFile"),
          Some("keyStorePassword"),
        ),
      ),
      keyManager = Some(
        KeyManagerDefinition(
          Left(
            FactoryDefinition("keyManagerAlgorithm", Some("keyManagerProvider")),
          ),
          "keyManagerPassword",
        ),
      ),
      trustManager = Some(
        TrustManagerDefinition(
          Left(
            FactoryDefinition("trustManagerAlgorithm", Some("trustManagerProvider")),
          ),
        ),
      ),
      // filling deprecated value with None
      verifyPeerMode = None,
    )
  }

}
