package play.api.cache

/**
  * @author Karel Cemus
  */
package object redis extends AnyRef with util.Expiration {

  type SynchronousResult[ A ]  = A
  type AsynchronousResult[ A ] = scala.concurrent.Future[ A ]

  type Configuration = configuration.RedisConfiguration
  type RedisConnector = connector.RedisConnector
}
