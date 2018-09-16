package play.api.cache.redis.configuration

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigOrigin}

trait RedisInstanceResolver {
  def resolve: PartialFunction[String, RedisInstance]
}

sealed trait RedisInstanceProvider extends Any {
  def name: String
  def resolved(implicit resolver: RedisInstanceResolver): RedisInstance
}

class ResolvedRedisInstance(val instance: RedisInstance) extends RedisInstanceProvider {
  def name: String = instance.name
  def resolved(implicit resolver: RedisInstanceResolver) = instance

  // $COVERAGE-OFF$
  override def equals(obj: scala.Any) = obj match {
    case that: ResolvedRedisInstance => this.name == that.name && this.instance == that.instance
    case _                           => false
  }

  override def hashCode() = name.hashCode

  override def toString = s"ResolvedRedisInstance($name@$instance)"
  // $COVERAGE-ON$
}

class UnresolvedRedisInstance(val name: String) extends RedisInstanceProvider {
  def resolved(implicit resolver: RedisInstanceResolver) = resolver resolve name

  // $COVERAGE-OFF$
  override def equals(obj: scala.Any) = obj match {
    case that: UnresolvedRedisInstance => this.name == that.name
    case _                             => false
  }

  override def hashCode() = name.hashCode

  override def toString = s"UnresolvedRedisInstance($name)"
  // $COVERAGE-ON$
}

private[configuration] object RedisInstanceProvider extends RedisConfigInstanceLoader[RedisInstanceProvider] {
  import RedisConfigLoader._

  def load(config: Config, path: String, name: String)(implicit defaults: RedisSettings) = {
    config.getOption(path / "source", _.getString) getOrElse defaults.source match {
      // required static configuration of the standalone instance using application.conf
      case "standalone"        => RedisInstanceStandalone
      // required static configuration of the cluster using application.conf
      case "cluster" => RedisInstanceCluster
      // required static configuration of the sentinel using application.conf
      case "sentinel" => RedisInstanceSentinel
      // required possibly environmental configuration of the standalone instance
      case "connection-string" => RedisInstanceEnvironmental
      // supplied custom configuration
      case "custom"            => RedisInstanceCustom
      // found but unrecognized
      case other               => invalidConfiguration(config.getValue(path / "source").origin(), other)
    }
  }.load(config, path, name)

  /** helper indicating invalid configuration */
  @throws[IllegalStateException]
  private def invalidConfiguration(source: ConfigOrigin, value: String): Nothing = throw new IllegalStateException(
    s"""
       |Unrecognized configuration provider '$value' in ${source.filename()} at ${source.lineNumber()}.
       |Expected values are 'standalone', 'cluster', 'connection-string', and 'custom'.
    """.stripMargin
  )
}

/**
  * Statically configured single standalone redis instance
  */
private[configuration] object RedisInstanceStandalone extends RedisConfigInstanceLoader[RedisInstanceProvider] {
  def load(config: Config, path: String, instanceName: String)(implicit defaults: RedisSettings) = new ResolvedRedisInstance(
    RedisStandalone.apply(
      name = instanceName,
      host = RedisHost.load(config, path),
      settings = RedisSettings.withFallback(defaults).load(config, path)
    )
  )
}

/**
  * Statically configured redis cluster
  */
private[configuration] object RedisInstanceCluster extends RedisConfigInstanceLoader[RedisInstanceProvider] {
  import RedisConfigLoader._

  def load(config: Config, path: String, instanceName: String)(implicit defaults: RedisSettings) = new ResolvedRedisInstance(
    RedisCluster.apply(
      name = instanceName,
      nodes = config.getConfigList(path / "cluster").asScala.map(config => RedisHost.load(config)).toList,
      settings = RedisSettings.withFallback(defaults).load(config, path)
    )
  )
}

/**
  * Reads a configuration from the connection string, possibly from an environmental variable.
  * This instance configuration is designed to work in PaaS environments such as Heroku.
  */
private[configuration] object RedisInstanceEnvironmental extends RedisConfigInstanceLoader[RedisInstanceProvider] {
  import RedisConfigLoader._

  def load(config: Config, path: String, instanceName: String)(implicit defaults: RedisSettings) = new ResolvedRedisInstance(
    RedisStandalone.apply(
      name = instanceName,
      host = RedisHost.fromConnectionString(config getString path./("connection-string")),
      settings = RedisSettings.withFallback(defaults).load(config, path)
    )
  )
}

/**
  * @author Tomas Cerny (tmscer@gmail.com)
  */
private[ configuration ] object RedisInstanceSentinel extends RedisConfigInstanceLoader[ RedisInstanceProvider ] {
  import RedisConfigLoader._

  def load( config: Config, path: String, instanceName: String )( implicit defaults: RedisSettings ) = new ResolvedRedisInstance(
    RedisSentinel.apply(
      name = instanceName,
      sentinels = config.getConfigList( path / "sentinels" ).asScala.map( config => RedisHost.load( config ) ).toList,
      masterGroup = config.getString(path / "master-group"),
      password = config.getOption(path / "password", _.getString ),
      database = config.getOption(path / "database", _.getInt ),
      settings = RedisSettings.withFallback( defaults ).load( config, path )
    )
  )
}

/**
  * This binder indicates that the user provides his own configuration of this named cache.
  */
private[configuration] object RedisInstanceCustom extends RedisConfigInstanceLoader[RedisInstanceProvider] {
  def load(config: Config, path: String, instanceName: String)(implicit defaults: RedisSettings) = new UnresolvedRedisInstance(
    name = instanceName
  )
}
