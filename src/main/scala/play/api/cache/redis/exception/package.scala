package play.api.cache.redis

/**
  * Helper trait providing simplified and unified API to exception handling in play-redis
  *
  * @author Karel Cemus
  */
package object exception {

  // todo clean up during recovery policy implementation

  /** helper throwing UnsupportedOperationException */
  @throws[ UnsupportedOperationException ]
  def unsupported( message: String ): Nothing =
    throw new UnsupportedOperationException( message )

  /** helper indicating serialization failure, it throws an exception */
  @throws[ SerializationException ]
  def serializationFailed( key: String, message: String, cause: Throwable ) =
    throw new SerializationException( key, message, cause )

  //  def timedOut( key: String ) = throw new TimeoutException( key )

  def unexpected( key: Option[ String ], command: String ): Nothing =
    throw new UnexpectedResponseException( key, command )

  def failed( key: Option[ String ], command: String, cause: Throwable ): Nothing =
    throw new ExecutionFailedException( key, command, cause )

  def invalidConfiguration( message: String ): Nothing =
    throw new IllegalStateException( message )
}
