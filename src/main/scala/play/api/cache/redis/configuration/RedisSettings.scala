package play.api.cache.redis.configuration

import play.api.ConfigLoader

import com.typesafe.config.Config

/**
  * Configures non-connection related settings of redis instance,
  * e.g., synchronization timeout, Akka dispatcher, and recovery policy.
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
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any) = equalsAsSettings(obj)
  /** trait-specific equals, invokable from children */
  protected def equalsAsSettings(obj: scala.Any) = obj match {
    case that: RedisSettings => Equals.check(this, that)(_.invocationContext, _.invocationPolicy, _.timeout, _.recovery, _.source, _.prefix)
    case _                   => false
  }
  // $COVERAGE-ON$
}

object RedisSettings extends ConfigLoader[RedisSettings] {
  import RedisConfigLoader._

  def load(config: Config, path: String) = apply(
    dispatcher = loadInvocationContext(config, path).get,
    invocationPolicy = loadInvocationPolicy(config, path).get,
    recovery = loadRecovery(config, path).get,
    timeout = loadTimeouts(config, path)(RedisTimeouts.requiredDefault),
    source = loadSource(config, path).get,
    prefix = loadPrefix(config, path)
  )

  def withFallback(fallback: RedisSettings) = new ConfigLoader[RedisSettings] {
    def load(config: Config, path: String) = apply(
      dispatcher = loadInvocationContext(config, path) getOrElse fallback.invocationContext,
      invocationPolicy = loadInvocationPolicy(config, path) getOrElse fallback.invocationPolicy,
      recovery = loadRecovery(config, path) getOrElse fallback.recovery,
      timeout = loadTimeouts(config, path)(fallback.timeout),
      source = loadSource(config, path) getOrElse fallback.source,
      prefix = loadPrefix(config, path) orElse fallback.prefix
    )
  }

  def apply(dispatcher: String, invocationPolicy: String, timeout: RedisTimeouts, recovery: String, source: String, prefix: Option[String] = None): RedisSettings =
    create(dispatcher, invocationPolicy, prefix, timeout, recovery, source)

  @inline
  private def create(_dispatcher: String, _invocation: String, _prefix: Option[String], _timeout: RedisTimeouts, _recovery: String, _source: String) = new RedisSettings {
    val invocationContext = _dispatcher
    val invocationPolicy = _invocation
    val prefix = _prefix
    val recovery = _recovery
    val timeout = _timeout
    val source = _source
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
}

/**
  * A helper trait delegating properties into the inner settings object
  */
trait RedisDelegatingSettings extends RedisSettings {
  def settings: RedisSettings
  def prefix = settings.prefix
  def source = settings.source
  def timeout = settings.timeout
  def recovery = settings.recovery
  def invocationContext = settings.invocationContext
  def invocationPolicy = settings.invocationPolicy
}
