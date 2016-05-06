package play.api.cache.redis.impl

import javax.inject.Inject

import scala.concurrent.Future

import play.api.Logger
import play.api.cache.redis.exception._

/**
  * Recovery policy triggers when a request fails. Based on the implementation,
  * it may try it again, recover with a default value or just simply log the
  * failure. Either way, it is up to user to define what to do on failure.
  *
  * @author Karel Cemus
  */
trait RecoveryPolicy {

  /**
    * When a failure occurs, this method handles it. It may re-run it, return default value,
    * log it or propagate the exception.
    *
    * @param rerun   failed request (cache operation)
    * @param default default value neutral to the operation
    * @param failure incident report
    * @tparam T expected result type
    * @return failure recovery or exception
    */
  def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ): Future[ T ]

}

/**
  * Abstract recovery policy provides a general helpers for
  * failure reporting. These might be usable when implementing
  * own recovery policy.
  *
  * @author Karel Cemus
  */
trait AbstractPolicy extends RecoveryPolicy {

  /** logger instance */
  protected val log = Logger( "play.api.cache.redis" )

  /** reports a failure into logs */
  protected def report( failure: RedisException ) = {
    // create a failure report
    val report = failure match {
      case TimeoutException( cause ) => s"Command execution timed out."
      case SerializationException( key, message, cause ) => s"$message for key '$key'."
      case ExecutionFailedException( Some( key ), command, cause ) => s"Command $command for key '$key' failed."
      case ExecutionFailedException( None, command, cause ) => s"Command $command failed."
      case UnexpectedResponseException( Some( key ), command ) => s"Command $command for key '$key' returned unexpected response."
      case UnexpectedResponseException( None, command ) => s"Command $command returned unexpected response."
    }
    // report a failure
    if ( failure.getCause == null ) log.error( report ) else log.error( report, failure.getCause )
  }
}

/**
  * When the command fails, it logs the failure and fails the whole operation.
  *
  * @author Karel Cemus
  */
private[ impl ] class LogAndFailPolicy @Inject( )( ) extends AbstractPolicy {

  /**
    * When a failure occurs, this method handles it. It may re-run it, return default value,
    * log it or propagate the exception.
    *
    * @param rerun   failed request (cache operation)
    * @param default default value neutral to the operation
    * @param failure incident report
    * @tparam T expected result type
    * @return failure recovery or exception
    */
  override def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ): Future[ T ] = {
    report( failure )
    // fail through
    throw failure
  }
}

/**
  * When the command fails, it logs the failure and returns default value
  * to prevent application failure. The returned value is neutral to the
  * operation and it should behave like there is no cache
  *
  * @author Karel Cemus
  */
private[ impl ] class LogAndDefaultPolicy @Inject( )( ) extends AbstractPolicy {

  /**
    * When a failure occurs, this method handles it. It may re-run it, return default value,
    * log it or propagate the exception.
    *
    * @param rerun   failed request (cache operation)
    * @param default default value neutral to the operation
    * @param failure incident report
    * @tparam T expected result type
    * @return failure recovery or exception
    */
  override def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ): Future[ T ] = {
    report( failure )
    // fail through
    default
  }
}
