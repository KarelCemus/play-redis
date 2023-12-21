package play.api.cache

package object redis extends AnyRef with ExpirationImplicits with ExceptionImplicits {

  @inline type Done = org.apache.pekko.Done
  @inline private[redis] val Done: Done = org.apache.pekko.Done

  type SynchronousResult[A] = A
  type AsynchronousResult[A] = scala.concurrent.Future[A]

  type RedisConnector = connector.RedisConnector
  type RedisInstanceResolver = configuration.RedisInstanceResolver
  type RedisCaches = impl.RedisCaches

  private[redis] type RedisInstance = configuration.RedisInstance
  private[redis] type RedisInstanceProvider = configuration.RedisInstanceProvider
}
