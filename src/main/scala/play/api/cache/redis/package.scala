package play.api.cache

/**
  * @author Karel Cemus
  */
package object redis extends AnyRef with ExpirationImplicits with ExceptionImplicits {

  @inline type Done = akka.Done
  @inline private[ redis ] val Done: Done = akka.Done

  type SynchronousResult[ A ]  = A
  type AsynchronousResult[ A ] = scala.concurrent.Future[ A ]

  private[ redis ] type RedisInstance = configuration.RedisInstance
  private[ redis ] type RedisConnector = connector.RedisConnector
  private[ redis ] type RedisInstanceProvider = configuration.RedisInstanceProvider
  private[ redis ] type RedisCaches = impl.RedisCaches
}
