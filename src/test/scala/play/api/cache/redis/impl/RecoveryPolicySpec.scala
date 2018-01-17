package play.api.cache.redis.impl

import scala.concurrent.Future
import scala.reflect.ClassTag

import play.api.cache.redis._

import akka.pattern.AskTimeoutException
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification

/**
  * <p>Behavior of recovery policy.</p>
  */
class RecoveryPolicySpec extends Specification {

  val futureDefault = Future.successful { }

  new PolicySpecs(
    "LogAndFail",
    new LogAndFailPolicy,
    throw new IllegalStateException( "Default should not trigger" ),
    new Expectation {
      override def expects[ T <: Throwable : ClassTag ]: Matcher[ Any ] = throwA[ T ]
    }
  )

  new PolicySpecs(
    "LogAndDefault",
    new LogAndDefaultPolicy,
    futureDefault,
    new Expectation {
      override def expects[ T <: Throwable : ClassTag ]: Matcher[ Any ] = beEqualTo( futureDefault )
    }
  )

  trait Expectation {
    def expects[ T <: Throwable : ClassTag ]: Matcher[ Any ]
  }

  class PolicySpecs( name: String, policy: RecoveryPolicy, default: => Future[ Unit ], expectation: Expectation ) {

    import expectation._

    def rerun: Future[ Unit ] =
      throw new IllegalStateException( "Rerun should not trigger" )

    val reason =
      new IllegalStateException( "Failure reason" )

    s"$name policy" should "recover from" >> {

      "TimeoutException with a key" in {
        policy.recoverFrom( rerun, default, new TimeoutException( new AskTimeoutException( "Simulated execution timeout" ) ) ) must expects[ TimeoutException ]
      }

      "SerializationException" in {
        policy.recoverFrom( rerun, default, new SerializationException( "key", "Serialization failed", reason ) ) must expects[ SerializationException ]
      }

      "ExecutionFailedException with a key" in {
        policy.recoverFrom( rerun, default, new ExecutionFailedException( Some( "key" ), "GET", reason ) ) must expects[ ExecutionFailedException ]
      }

      "ExecutionFailedException without a key" in {
        policy.recoverFrom( rerun, default, new ExecutionFailedException( None, "GET", reason ) ) must expects[ ExecutionFailedException ]
      }

      "UnexpectedResponseException with a key" in {
        policy.recoverFrom( rerun, default, new UnexpectedResponseException( Some( "key" ), "GET" ) ) must expects[ UnexpectedResponseException ]
      }

      "UnexpectedResponseException without a key" in {
        policy.recoverFrom( rerun, default, new UnexpectedResponseException( None, "GET" ) ) must expects[ UnexpectedResponseException ]
      }
    }

  }

}
