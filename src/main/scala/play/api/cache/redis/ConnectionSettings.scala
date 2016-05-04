package play.api.cache.redis

import javax.inject.{Inject, Provider, Singleton}

import scala.concurrent.ExecutionContext

import play.api.cache.redis.connector.RedisConnectorImpl

import akka.actor.ActorSystem

/**
  * Connection settings encapsulates host, port, and other settings considered by [[RedisConnectorImpl]].
  *
  * @author Karel Cemus
  */
@deprecated( "Removed due to simplification. Use Configuration instead.", "1.3.0" ) // todo remove this class
case class ConnectionSettings
(
  /** host with redis server */
  host: String,

  /** port redis listens on */
  port: Int,

  /** redis database to work with */
  database: Int,

  /** password for authentication with redis */
  password: Option[ String ]
)
( implicit

  /** default invocation context of all cache commands */
  val invocationContext: ExecutionContext,

  /** timeout of cache requests */
  val timeout: akka.util.Timeout
)


/**
  * Converts [[Configuration]] instance into simple immutable [[ConnectionSettings]]
  *
  * @author Karel Cemus
  */
@Singleton
class ConnectionSettingsProvider @Inject( )( configuration: Configuration, system: ActorSystem ) extends Provider[ ConnectionSettings ] {

  override def get( ): ConnectionSettings =
    configuration.connectionSettings( system )
}
