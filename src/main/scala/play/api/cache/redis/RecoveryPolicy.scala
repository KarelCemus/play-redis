package play.api.cache.redis

import javax.inject.Inject

import scala.concurrent.Future

import play.api.Logger
import play.api.inject._

/** Recovery policy triggers when a request fails. Based on the implementation,
  * it may try it again, recover with a default value or just simply log the
  * failure. Either way, it is up to user to define what to do on failure.
  *
  * @since 1.3.0
  * @author Karel Cemus
  */
trait RecoveryPolicy {

  /** name of the policy used for internal purposes */
  def name: String = this.getClass.getSimpleName

  /** When a failure occurs, this method handles it. It may re-run it, return default value,
    * log it or propagate the exception.
    *
    * @param rerun   failed request (cache operation)
    * @param default default value neutral to the operation
    * @param failure incident report
    * @tparam T expected result type
    * @return failure recovery or exception
    */
  def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ): Future[ T ]

  override def toString = s"RecoveryPolicy($name)"
}

/** Abstract recovery policy provides a general helpers for
  * failure reporting. These might be usable when implementing
  * own recovery policy.
  *
  * @author Karel Cemus
  */
trait Reports extends RecoveryPolicy {

  /** logger instance */
  protected val log = Logger( "play.api.cache.redis" )

  protected def message( failure: RedisException ): String = failure match {
    case TimeoutException( cause ) => s"Command execution timed out."
    case SerializationException( key, message, cause ) => s"$message for key '$key'."
    case ExecutionFailedException( Some( key ), command, cause ) => s"Command $command for key '$key' failed."
    case ExecutionFailedException( None, command, cause ) => s"Command $command failed."
    case UnexpectedResponseException( Some( key ), command ) => s"Command $command for key '$key' returned unexpected response."
    case UnexpectedResponseException( None, command ) => s"Command $command returned unexpected response."
  }

  protected def doLog( message: String, cause: Option[ Throwable ] ): Unit

  /** reports a failure into logs */
  protected def report( failure: RedisException ) = {
    // create a failure report and report a failure
    doLog( message( failure ), Option( failure.getCause ) )
  }

  abstract override def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ): Future[ T ] = {
    // log it and let through
    report( failure )
    // dive into
    super.recoverFrom( rerun, default, failure )
  }
}

/** Detailed reports policy produces logs with failure causes
  *
  * @author Karel Cemus
  */
trait DetailedReports extends Reports {

  protected def doLog( message: String, cause: Option[ Throwable ] ): Unit =
    cause.fold( log.error( message ) )( log.error( message, _ ) )
}

/** Condensed reports policy produces logs without causes, i.e., logs
  * are shorter but less informative.
  *
  * @author Karel Cemus
  */
trait CondensedReports extends Reports {

  protected def doLog( message: String, cause: Option[ Throwable ] ): Unit =
    log.error( message )
}

/** It fails on failure, i.e., propages the exception to upper layers
  *
  * @author Karel Cemus
  */
trait FailThrough extends RecoveryPolicy {

  override def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ): Future[ T ] = {
    // fail through
    throw failure
  }
}

/** Recovers with a default value instead of failing through
  *
  * @author Karel Cemus
  */
trait RecoverWithDefault extends RecoveryPolicy {

  override def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ): Future[ T ] = {
    // return default value
    default
  }
}

/** When the command fails, it logs the failure and fails the whole operation.
  *
  * @author Karel Cemus
  */
private[ redis ] class LogAndFailPolicy @Inject( )( )
  extends FailThrough with DetailedReports

/** When the command fails, it logs the failure and returns default value
  * to prevent application failure. The returned value is neutral to the
  * operation and it should behave like there is no cache
  *
  * @author Karel Cemus
  */
private[ redis ] class LogAndDefaultPolicy @Inject( )( )
  extends RecoverWithDefault with DetailedReports


/** When the command fails, it logs the failure and returns default value
  * to prevent application failure. The returned value is neutral to the
  * operation and it should behave like there is no cache
  *
  * LogCondensed produces condensed messages without a stacktrace to avoid
  * extensive logs
  *
  * @author Karel Cemus
  */
private[ redis ] class LogCondensedAndDefaultPolicy @Inject( )( )
  extends RecoverWithDefault with CondensedReports


/** When the command fails, it logs the failure and fails the whole operation.
  *
  * LogCondensed produces condensed messages without a stacktrace to avoid
  * extensive logs
  *
  * @author Karel Cemus
  */
private[ redis ] class LogCondensedAndFailPolicy @Inject( )( )
  extends FailThrough with CondensedReports

/**
  * This resolver represents an abstraction over translation
  * of the policy name into the instance. It has two subclasses,
  * one for guice and the other for compile-time injection.
  */
trait RecoveryPolicyResolver {
  def resolve: PartialFunction[ String, RecoveryPolicy ]
}

class RecoveryPolicyResolverImpl extends RecoveryPolicyResolver {
  val resolve: PartialFunction[ String, RecoveryPolicy ] = {
    case "log-and-fail" => new LogAndFailPolicy
    case "log-and-default" => new LogAndDefaultPolicy
    case "log-condensed-and-fail" => new LogCondensedAndFailPolicy
    case "log-condensed-and-default" => new LogCondensedAndDefaultPolicy
  }
}

object RecoveryPolicyResolver {

  def bindings = Seq(
    bind[ RecoveryPolicy ].qualifiedWith( "log-and-fail" ).to[ LogAndFailPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-and-default" ).to[ LogAndDefaultPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-condensed-and-fail" ).to[ LogCondensedAndFailPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-condensed-and-default" ).to[ LogCondensedAndDefaultPolicy ],
    // finally bind the resolver
    bind[ RecoveryPolicyResolver ].to[ RecoveryPolicyResolverGuice ]
  )
}

/** resolves a policies with guice enabled */
class RecoveryPolicyResolverGuice @Inject( )( injector: Injector ) extends RecoveryPolicyResolver {

  def resolve = {
    case name => injector instanceOf bind[ RecoveryPolicy ].qualifiedWith( name )
  }
}
