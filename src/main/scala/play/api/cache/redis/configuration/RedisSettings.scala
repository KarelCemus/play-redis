package play.api.cache.redis.configuration

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

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
  /** timeout of cache commands */
  def timeout: FiniteDuration
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
    case that: RedisSettings if this.invocationContext == that.invocationContext && this.timeout == that.timeout && this.recovery == that.recovery && this.source == that.source => true
    case _ => false
  }
}


object RedisSettings extends ConfigLoader[ RedisSettings ] {
  import RedisConfigLoader._

  def load( config: Config, path: String ) = apply(
    dispatcher = loadInvocationContext( config, path )( required ),
    recovery = loadRecovery( config, path )( required ),
    timeout = loadTimeout( config, path )( required ),
    source = loadSource( config, path )( "standalone".asFallback ),
    prefix = loadPrefix( config, path )( None.asFallback )
  )

  def withFallback( fallback: RedisSettings ) = new ConfigLoader[ RedisSettings ] {
    def load( config: Config, path: String ) = apply(
      dispatcher = RedisSettings.loadInvocationContext( config, path )( fallback.invocationContext.asFallback ),
      recovery = RedisSettings.loadRecovery( config, path )( fallback.recovery.asFallback ),
      timeout = RedisSettings.loadTimeout( config, path )( fallback.timeout.asFallback ),
      source = loadSource( config, path )( fallback.source.asFallback ),
      prefix = RedisSettings.loadPrefix( config, path )( fallback.prefix.asFallback )
    )
  }

  def apply( dispatcher: String, timeout: FiniteDuration, recovery: String, source: String, prefix: Option[ String ] = None ): RedisSettings =
    create( dispatcher, prefix, timeout, recovery, source )

  @inline
  private def create( _dispatcher: String, _prefix: Option[ String ], _timeout: FiniteDuration, _recovery: String, _source: String ) = new RedisSettings {
    val invocationContext = _dispatcher
    val prefix = _prefix
    val recovery = _recovery
    val timeout = _timeout
    val source = _source
  }

  private def loadInvocationContext( config: Config, path: String )( default: String => String ): String =
    config.getOption( path / "dispatcher", _.getString ) getOrElse default( path / "dispatcher" )

  private def loadRecovery( config: Config, path: String )( default: String => String ): String =
    config.getOption( path / "recovery", _.getString ) getOrElse default( path / "recovery" )

  private def loadSource( config: Config, path: String )( default: String => String ): String =
    config.getOption( path / "source", _.getString ) getOrElse default( path / "source" )

  private def loadPrefix( config: Config, path: String )( default: String => Option[ String ] ): Option[ String ] =
    config.getOption( path / "prefix", _.getString ) orElse default( path / "prefix" )

  private def loadTimeout( config: Config, path: String )( default: String => FiniteDuration ): FiniteDuration =
    config.getOption( path / "timeout", _.getDuration ).map {
      duration => FiniteDuration( duration.getSeconds, TimeUnit.SECONDS )
    } getOrElse default( path / "timeout" )
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
}
