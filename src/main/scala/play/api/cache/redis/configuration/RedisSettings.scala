package play.api.cache.redis.configuration

import play.api.ConfigLoader

import com.typesafe.config.Config

/**
  * Configures non-connection related settings of redis instance,
  * e.g., synchronization timeout, Akka dispatcher, and recovery policy.
  *
  * @author Karel Cemus
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
  def prefix: Option[ String ]
  /** trait-specific equals */
  override def equals( obj: scala.Any ) = equalsAsSettings( obj )
  /** trait-specific equals, invokable from children */
  protected def equalsAsSettings( obj: scala.Any ) = obj match {
    case that: RedisSettings => this.invocationContext == that.invocationContext && this.timeout == that.timeout && this.recovery == that.recovery && this.source == that.source
    case _ => false
  }
}


object RedisSettings extends ConfigLoader[ RedisSettings ] {
  import RedisConfigLoader._

  def load( config: Config, path: String ) = apply(
    dispatcher = loadInvocationContext( config, path )( required ),
    invocationPolicy = loadInvocationPolicy( config, path )( required ),
    recovery = loadRecovery( config, path )( required ),
    timeout = loadTimeouts( config, path )( RedisTimeouts.requiredDefault( required ).asFallback ),
    source = loadSource( config, path )( "standalone".asFallback ),
    prefix = loadPrefix( config, path )( None.asFallback )
  )

  def withFallback( fallback: RedisSettings ) = new ConfigLoader[ RedisSettings ] {
    def load( config: Config, path: String ) = apply(
      dispatcher = loadInvocationContext( config, path )( fallback.invocationContext.asFallback ),
      invocationPolicy = loadInvocationPolicy( config, path )( fallback.invocationPolicy.asFallback ),
      recovery = loadRecovery( config, path )( fallback.recovery.asFallback ),
      timeout = loadTimeouts( config, path )( fallback.timeout.asFallback ),
      source = loadSource( config, path )( fallback.source.asFallback ),
      prefix = loadPrefix( config, path )( fallback.prefix.asFallback )
    )
  }

  def apply( dispatcher: String, invocationPolicy: String, timeout: RedisTimeouts, recovery: String, source: String, prefix: Option[ String ] = None ): RedisSettings =
    create( dispatcher, invocationPolicy, prefix, timeout, recovery, source )

  @inline
  private def create( _dispatcher: String, _invocation: String, _prefix: Option[ String ], _timeout: RedisTimeouts, _recovery: String, _source: String ) = new RedisSettings {
    val invocationContext = _dispatcher
    val invocationPolicy = _invocation
    val prefix = _prefix
    val recovery = _recovery
    val timeout = _timeout
    val source = _source
  }

  private def loadInvocationContext( config: Config, path: String )( default: String => String ): String =
    config.getOption( path / "dispatcher", _.getString ) getOrElse default( path / "dispatcher" )

  private def loadInvocationPolicy( config: Config, path: String )( default: String => String ): String =
    config.getOption( path / "invocation", _.getString ) getOrElse default( path / "invocation" )

  private def loadRecovery( config: Config, path: String )( default: String => String ): String =
    config.getOption( path / "recovery", _.getString ) getOrElse default( path / "recovery" )

  private def loadSource( config: Config, path: String )( default: String => String ): String =
    config.getOption( path / "source", _.getString ) getOrElse default( path / "source" )

  private def loadPrefix( config: Config, path: String )( default: String => Option[ String ] ): Option[ String ] =
    config.getOption( path / "prefix", _.getString ) orElse default( path / "prefix" )

  private def loadTimeouts( config: Config, path: String )( default: String => RedisTimeouts ): RedisTimeouts =
    RedisTimeouts.load( config, path )( default )
}


/**
  * A helper trait delegating properties into the inner settings object
  *
  * @author Karel Cemus
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
