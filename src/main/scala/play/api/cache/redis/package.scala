package play.api.cache

/**
  * @author Karel Cemus
  */
package object redis extends AnyRef with util.Expiration {

  type Done = akka.Done
  private[ redis ] val Done: Done = akka.Done

  type SynchronousResult[ A ]  = A
  type AsynchronousResult[ A ] = scala.concurrent.Future[ A ]

  private[ redis ] type RedisInstance = configuration.RedisInstance
  private[ redis ] type RedisConnector = connector.RedisConnector
}
