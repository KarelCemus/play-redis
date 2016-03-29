package play.api.cache.redis

/**
  * Helper trait providing simplified and unified API to exception handling in play-redis
  *
  * @author Karel Cemus
  */
trait Exceptions {

  /** helper throwing UnsupportedOperationException */
  @throws[ UnsupportedOperationException ]
  def unsupported( message: String ): Nothing = throw new UnsupportedOperationException( message )

}
