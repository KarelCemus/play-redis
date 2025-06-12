package play.api.cache.redis.configuration

import com.typesafe.config.Config
import io.lettuce.core.{SslOptions, SslVerifyMode}
import play.api.ConfigLoader
import play.api.cache.redis.configuration.RedisSslSettings.{KeyManagerDefinition, KeyStoreDefinition, TrustManagerDefinition, TrustStoreDefinition, VerifyPeerMode}

import java.io.File
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.net.ssl.{KeyManagerFactory, TrustManagerFactory}
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.CollectionHasAsScala

trait RedisSslSettings {
  def protocols: List[String]
  def trustStore: TrustStoreDefinition

  def keyStoreType: Option[String]
  def cipherSuites: Option[List[String]]
  def handShakeTimeout: Option[FiniteDuration]
  def keyStore: Option[KeyStoreDefinition]
  def keyManager: Option[KeyManagerDefinition]
  def trustManager: Option[TrustManagerDefinition]
  def verifyPeerMode: Option[VerifyPeerMode]

  def toOptions: SslOptions = {
    val modifiers: List[Option[SslOptions.Builder => SslOptions.Builder]] = {
      List(
        Some(_.protocols(protocols: _*)),
        Some(trustStore.apply),
        keyStore.map(k => k.apply),
        keyManager.map(k => k.apply),
        trustManager.map(t => t.apply),
        keyStoreType.map(k => (b: SslOptions.Builder) => b.keyStoreType(k)),
        cipherSuites.map(c => (b: SslOptions.Builder) => b.cipherSuites(c: _*)),
        handShakeTimeout.map(h => (b: SslOptions.Builder) => b.handshakeTimeout(Duration.ofMillis(h.toMillis)))
      )
    }

    modifiers
      .collect { case Some(m) => m }
      .foldRight(SslOptions.builder())(_(_)).build()
  }
}

object RedisSslSettings extends ConfigLoader[RedisSslSettings]{
  import RedisConfigLoader._

  final case class RedisSslSettingsImpl(
     protocols: List[String],
     trustStore: TrustStoreDefinition,
     keyStoreType: Option[String],
     cipherSuites: Option[List[String]],
     handShakeTimeout: Option[FiniteDuration],
     keyStore: Option[KeyStoreDefinition],
     keyManager: Option[KeyManagerDefinition],
     trustManager: Option[TrustManagerDefinition],
     verifyPeerMode: Option[VerifyPeerMode]
  ) extends RedisSslSettings

  def getOpt(config: Config, path: String): Option[RedisSslSettings] = {
    val pathToObject = path / "ssl-settings"

    if (config.hasPath(pathToObject)) {
      Some(
        RedisSslSettingsImpl(
          protocols = config.getStringList(pathToObject / "protocols").asScala.toList,
          trustStore = TrustStoreDefinition.get(config, pathToObject / "trust-store"),
          keyStoreType = config.getOption(pathToObject / "key-store-type", _.getString),
          cipherSuites = config.getOption(pathToObject / "cipher-suites", _.getStringList).map(_.asScala.toList),
          handShakeTimeout = config.getOption(pathToObject / "hand-shake-timeout", _.getDuration).map(d => FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS)),
          keyStore = KeyStoreDefinition.getOpt(config, pathToObject / "key-store"),
          keyManager = KeyManagerDefinition.getOpt(config, pathToObject / "key-manager"),
          trustManager = TrustManagerDefinition.getOpt(config, pathToObject / "trust-manager"),
          verifyPeerMode = VerifyPeerMode.getOpt(config, pathToObject / "verify-peer-mode")
        )
      )
    } else {
      None
    }
  }

  private def getPassword(config: Config, path: String): String =
    config.getString(path / "password")

  private def getPasswordOpt(config: Config, path: String): Option[String] =
    config.getOption(path / "password", _.getString)

  sealed abstract class VerifyPeerMode(val name: String, val value: SslVerifyMode) extends Product with Serializable
  object VerifyPeerMode {
    final case object NONE extends VerifyPeerMode("none", SslVerifyMode.NONE)
    final case object FULL extends VerifyPeerMode("full", SslVerifyMode.FULL)
    final case object CA extends VerifyPeerMode("ca", SslVerifyMode.CA)

    def none: VerifyPeerMode = NONE
    def full: VerifyPeerMode = FULL
    def ca: VerifyPeerMode = CA

    def getOpt(config: Config, path: String): Option[VerifyPeerMode] =
      config.getOption(path, _.getString) match {
        case Some(NONE.name) => Some(none)
        case Some(FULL.name) => Some(full)
        case Some(CA.name) => Some(ca)
        case _ => None
      }
  }

  sealed trait SslResource extends Product with Serializable {
    def path: String
  }
  object SslResource {
    final case class FileResource(path: String) extends SslResource {
      def toResource: File = new File(path)
    }
    object FileResource {
      def get(config: Config, path: String): FileResource =
        FileResource(path = getPath(config, path / "file"))
    }

    final case class UrlResource(path: String) extends SslResource {
      def toResource: URL = java.net.URI.create(path).toURL
    }
    object UrlResource {
      def get(config: Config, path: String): UrlResource =
        UrlResource(path = getPath(config, path / "url"))
    }

    private def getPath(config: Config, path: String): String =
      config.getString(path / "path")

    def getOpt(config: Config, path: String): Option[SslResource] = {
      if (config.hasPath(path / "file"))
        Some(FileResource.get(config, path))
      else if (config.hasPath(path / "url"))
        Some(UrlResource.get(config, path))
      else
        None
    }

    def get(config: Config, path: String): SslResource =
      getOpt(config, path).getOrElse(throw new IllegalArgumentException(s"Unknown resource. Only `file` and `url` are supported."))
  }

  final case class FactoryDefinition(algorithm: String, provider: Option[String]) {
    def toKeyManagerFactory: KeyManagerFactory = provider match {
      case Some(p) => KeyManagerFactory.getInstance(algorithm, p)
      case None => KeyManagerFactory.getInstance(algorithm)
    }

    def toTrustManagerFactory: TrustManagerFactory = provider match {
      case Some(p) => TrustManagerFactory.getInstance(algorithm, p)
      case None => TrustManagerFactory.getInstance(algorithm)
    }
  }
  object FactoryDefinition {
    private def getAlgorithm(config: Config, path: String): String =
      config.getString(path / "algorithm")
    private def getProvider(config: Config, path: String): Option[String] =
      config.getOption(path / "provider", _.getString)

    def get(config: Config, path: String): FactoryDefinition =
      FactoryDefinition(
        algorithm = getAlgorithm(config, path / "factory"),
        provider = getProvider(config, path / "factory")
      )
  }

  final case class KeyStoreDefinition(resource: SslResource, password: Option[String]) {
    def apply(builder: SslOptions.Builder): SslOptions.Builder = resource match {
      case r: SslResource.FileResource => password.map(p => builder.keystore(r.toResource, p.toCharArray)).getOrElse(builder.keystore(r.toResource))
      case r: SslResource.UrlResource => password.map(p => builder.keystore(r.toResource, p.toCharArray)).getOrElse(builder.keystore(r.toResource))
    }
  }
  object KeyStoreDefinition {
    def getOpt(config: Config, path: String): Option[KeyStoreDefinition] = {
      if (config.hasPath(path)) {
       Some(
         KeyStoreDefinition(
           resource = SslResource.get(config, path),
           password = getPasswordOpt(config, path)
         )
       )
      } else {
        None
      }
    }
  }

  final case class TrustStoreDefinition(resource: SslResource, password: Option[String]) {
    def apply(builder: SslOptions.Builder): SslOptions.Builder = resource match {
      case r: SslResource.FileResource => password.map(p => builder.truststore(r.toResource, p)).getOrElse(builder.truststore(r.toResource))
      case r: SslResource.UrlResource => password.map(p => builder.truststore(r.toResource, p)).getOrElse(builder.truststore(r.toResource))
    }
  }
  object TrustStoreDefinition {
    def get(config: Config, path: String): TrustStoreDefinition =
      TrustStoreDefinition(
        resource = SslResource.get(config, path),
        password = getPasswordOpt(config, path)
      )
  }

  final case class KeyManagerDefinition(value: Either[FactoryDefinition, KeyManagerDefinition.Resource], password: String) {
    def apply(builder: SslOptions.Builder): SslOptions.Builder = value match {
      case Left(factory) => builder.keyManager(factory.toKeyManagerFactory)
      case Right(fileResource) => builder.keyManager(fileResource.keyCertChain.toResource, fileResource.key.toResource, password.toCharArray)
    }
  }
  object KeyManagerDefinition {
    final case class Resource(keyCertChain: SslResource.FileResource, key: SslResource.FileResource)
    object Resource {
      def get(config: Config, path: String): Resource =
        Resource(
          keyCertChain = SslResource.FileResource.get(config, path / "resource" / "key-cert-chain"),
          key = SslResource.FileResource.get(config, path / "resource" / "key")
        )
    }

    def getOpt(config: Config, path: String): Option[KeyManagerDefinition] = {
      if (config.hasPath(path / "factory"))
        Some(
          KeyManagerDefinition(
            value = Left(FactoryDefinition.get(config, path)),
            password = getPassword(config, path)
          )
        )
      else if (config.hasPath(path / "resource"))
        Some(
          KeyManagerDefinition(
            value = Right(Resource.get(config, path)),
            password = getPassword(config, path)
          )
        )
      else
        None
    }

    def get(config: Config, path: String): KeyManagerDefinition =
      getOpt(config, path).getOrElse(throw new IllegalArgumentException(s"Unknown key manager definition. Only `factory` and `file` are supported."))
  }

  final case class TrustManagerDefinition(value: Either[FactoryDefinition, SslResource.FileResource]) {
    def apply(builder: SslOptions.Builder): SslOptions.Builder = value match {
      case Left(factory) => builder.trustManager(factory.toTrustManagerFactory)
      case Right(fileResource) => builder.trustManager(fileResource.toResource)
    }
  }
  object TrustManagerDefinition {
    def getOpt(config: Config, path: String): Option[TrustManagerDefinition] =
      if (config.hasPath(path / "factory")) {
        Some(
          TrustManagerDefinition(
            value = Left(FactoryDefinition.get(config, path))
          )
        )
      } else if (config.hasPath(path / "file")) {
        Some(
          TrustManagerDefinition(
            value = Right(SslResource.FileResource.get(config, path))
          )
        )
      } else {
        None
      }

    def get(config: Config, path: String): TrustManagerDefinition =
      getOpt(config, path).getOrElse(throw new IllegalArgumentException(s"Unknown trust manager definition. Only `factory` and `file` are supported."))
  }

  override def load(config: Config, path: String): RedisSslSettings =
    getOpt(config, path).getOrElse(throw new IllegalArgumentException("ssl-settings is not defined."))
}