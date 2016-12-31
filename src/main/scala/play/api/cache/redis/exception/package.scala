package play.api.cache.redis

/**
  * Helper trait providing simplified and unified API to exception handling in play-redis
  *
  * @author Karel Cemus
  */
package object exception {

  /** helper throwing UnsupportedOperationException */
  @throws[ UnsupportedOperationException ]
  def unsupported( message: String ): Nothing =
    throw new UnsupportedOperationException( message )

  /** helper indicating serialization failure, it throws an exception */
  @throws[ SerializationException ]
  def serializationFailed( key: String, message: String, cause: Throwable ) =
    throw SerializationException( key, message, cause )

  /** helper indicating  command execution timed out */
  @throws[ TimeoutException ]
  def timedOut( cause: Throwable ) =
    throw TimeoutException( cause )

  /** helper indicating the command execution returned unexpected exception */
  @throws[ UnexpectedResponseException ]
  def unexpected( key: Option[ String ], command: String ): Nothing =
    throw UnexpectedResponseException( key, command )

  /** helper indicating command execution failed with exception */
  @throws[ ExecutionFailedException ]
  def failed( key: Option[ String ], command: String, cause: Throwable ): Nothing =
    throw ExecutionFailedException( key, command, cause )

  /** helper indicating invalid configuration */
  @throws[ IllegalStateException ]
  def invalidConfiguration( message: String ): Nothing =
    throw new IllegalStateException( message )
}
