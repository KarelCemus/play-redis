package play.api.cache.redis.configuration

import javax.inject.Singleton

import play.api.Configuration

/**
 * Environment configuration expects the configuration to be injected through environment variable containing
 * the connection string. This configuration is often used by PaaS environments.
 */
@Singleton
class ConnectionString(

  /** host with redis server */
  override val host: String,

  /** port redis listens on */
  override val port: Int,

  /** authentication password */
  override val password: Option[ String ]

)( implicit configuration: Configuration ) extends ConfigurationFile
