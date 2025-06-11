package play.api.cache.redis.configuration

import com.typesafe.config.Config
import play.api.ConfigLoader

/**
  * Configures non-connection related settings of redis instance, e.g.,
  * synchronization timeout, Pekko dispatcher, and recovery policy.
  */
trait RedisSettings {

  /** the name of the invocation context executing all commands to Redis */
  def invocationContext: String

  /** the name of the invocation policy used in getOrElse methods */
  def invocationPolicy: String

  /** timeout configuration */
  def timeout: RedisTimeouts

  /** recovery policy used with the instance */
  def recovery: String

  /** configuration source */
  def source: String

  /** instance prefix */
  def prefix: Option[String]

  /** threadpool */
  def threadPool: RedisThreadPools

  /** ssl settings */
  def sslSettings: Option[RedisSslSettings]

  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any): Boolean = equalsAsSettings(obj)

  /** trait-specific equals, invokable from children */
  protected def equalsAsSettings(obj: scala.Any): Boolean = obj match {
    case that: RedisSettings => Equals.check(this, that)(_.invocationContext, _.invocationPolicy, _.timeout, _.recovery, _.source, _.prefix, _.sslSettings)
    case _                   => false
  }
  // $COVERAGE-ON$

}

object RedisSettings extends ConfigLoader[RedisSettings] {
  import RedisConfigLoader._

  override def load(config: Config, path: String): RedisSettings = apply(
    dispatcher = loadInvocationContext(config, path).get,
    invocationPolicy = loadInvocationPolicy(config, path).get,
    recovery = loadRecovery(config, path).get,
    timeout = loadTimeouts(config, path)(RedisTimeouts.requiredDefault),
    source = loadSource(config, path).get,
    prefix = loadPrefix(config, path),
    threadPool = loadThreadPool(config, path)(RedisThreadPools.requiredDefault),
    sslSettings = loadSslSettings(config, path)
  )

  def withFallback(fallback: RedisSettings): ConfigLoader[RedisSettings] =
    (config: Config, path: String) =>
      apply(
        dispatcher = loadInvocationContext(config, path) getOrElse fallback.invocationContext,
        invocationPolicy = loadInvocationPolicy(config, path) getOrElse fallback.invocationPolicy,
        recovery = loadRecovery(config, path) getOrElse fallback.recovery,
        timeout = loadTimeouts(config, path)(fallback.timeout),
        source = loadSource(config, path) getOrElse fallback.source,
        prefix = loadPrefix(config, path) orElse fallback.prefix,
        threadPool = loadThreadPool(config, path)(fallback.threadPool),
        sslSettings = loadSslSettings(config, path) orElse fallback.sslSettings
      )

  def apply(dispatcher: String, invocationPolicy: String, timeout: RedisTimeouts, recovery: String, source: String, prefix: Option[String] = None, threadPool: RedisThreadPools, sslSettings: Option[RedisSslSettings] = None): RedisSettings =
    create(dispatcher, invocationPolicy, prefix, timeout, recovery, source, threadPool, sslSettings)

  @inline
  private def create(_dispatcher: String, _invocation: String, _prefix: Option[String], _timeout: RedisTimeouts, _recovery: String, _source: String, _threadpool: RedisThreadPools, _sslSettings: Option[RedisSslSettings]) = new RedisSettings {
    override val invocationContext: String = _dispatcher
    override val invocationPolicy: String = _invocation
    override val prefix: Option[String] = _prefix
    override val recovery: String = _recovery
    override val timeout: RedisTimeouts = _timeout
    override val source: String = _source
    override val threadPool: RedisThreadPools = _threadpool
    override val sslSettings: Option[RedisSslSettings] = _sslSettings
  }

  private def loadInvocationContext(config: Config, path: String): Option[String] =
    config.getOption(path / "dispatcher", _.getString)

  private def loadInvocationPolicy(config: Config, path: String): Option[String] =
    config.getOption(path / "invocation", _.getString)

  private def loadRecovery(config: Config, path: String): Option[String] =
    config.getOption(path / "recovery", _.getString)

  private def loadSource(config: Config, path: String): Option[String] =
    config.getOption(path / "source", _.getString)

  private def loadPrefix(config: Config, path: String): Option[String] =
    config.getOption(path / "prefix", _.getString)

  private def loadTimeouts(config: Config, path: String)(defaults: RedisTimeouts): RedisTimeouts =
    RedisTimeouts.load(config, path)(defaults)

  private def loadThreadPool(config: Config, path: String)(defaults: RedisThreadPools): RedisThreadPools =
    RedisThreadPools.load(config, path)(defaults)

  private def loadSslSettings(config: Config, path: String): Option[RedisSslSettings] =
    RedisSslSettings.getOpt(config, path)

}

/** A helper trait delegating properties into the inner settings object */
trait RedisDelegatingSettings extends RedisSettings {
  def settings: RedisSettings
  override def prefix: Option[String] = settings.prefix
  override def source: String = settings.source
  override def timeout: RedisTimeouts = settings.timeout
  override def recovery: String = settings.recovery
  override def invocationContext: String = settings.invocationContext
  override def invocationPolicy: String = settings.invocationPolicy
  override def threadPool: RedisThreadPools = settings.threadPool
  override def sslSettings: Option[RedisSslSettings] = settings.sslSettings
}
