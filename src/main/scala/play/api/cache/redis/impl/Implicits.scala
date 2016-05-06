package play.api.cache.redis.impl

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

import play.api.cache.redis.exception.RedisException

/** Implicit helpers used within the redis cache implementation. These
  * handful tools simplifies code readability but has no major function.
  *
  * @author Karel Cemus
  */
private[ impl ] trait Implicits {

  /** enriches any ref by toFuture converting a value to Future.successful */
  protected implicit class RichFuture[ T ]( any: T ) {
    def toFuture( implicit context: ExecutionContext ) = Future( any )
  }

  /** helper function enabling us to recover from command execution */
  implicit class RecoveryFuture[ T ]( future: => Future[ T ] ) {

    /** Transforms the promise into desired builder results, possibly recovers with provided default value */
    def recoverWithDefault[ Result[ X ] ]( default: => T )( implicit builder: Builders.ResultBuilder[ Result ], policy: RecoveryPolicy, context: ExecutionContext, timeout: akka.util.Timeout ): Result[ T ] =
      builder.toResult( future, Future.successful( default ) )

    /** recovers from the execution but returns future, not Result */
    def recoverWithFuture( default: => Future[ T ] )( implicit policy: RecoveryPolicy, context: ExecutionContext ): Future[ T ] =
      future recoverWith {
        // recover from known exceptions
        case failure: RedisException => policy.recoverFrom( future, default, failure )
      }
  }

  /** helper function enabling us to recover from command execution */
  implicit class RecoveryUnitFuture( future: => Future[ Unit ] ) {
    /** Transforms the promise into desired builder results, possibly recovers with provided default value */
    def recoverWithUnit[ Result[ X ] ]( implicit builder: Builders.ResultBuilder[ Result ], policy: RecoveryPolicy, context: ExecutionContext, timeout: akka.util.Timeout ): Result[ Unit ] =
      builder.toResult( future, Future.successful( Unit ) )
  }

}
