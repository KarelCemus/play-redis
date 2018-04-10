package play.api.cache.redis

import scala.concurrent.Future

import play.api.Logger

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class RecoveryPolicySpecs( implicit ee: ExecutionEnv ) extends Specification with Mockito {

  class BasicPolicy extends RecoveryPolicy {
    def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ) = default
  }

  val rerun = Future.successful( 10 )
  val default = Future.successful( 0 )

  object ex {
    private val internal = new IllegalArgumentException( "Internal cause" )
    val unexpectedAny = UnexpectedResponseException( None, "TEST-CMD" )
    val unexpectedKey = UnexpectedResponseException( Some( "some key" ), "TEST-CMD" )
    val failedAny = ExecutionFailedException( None, "TEST-CMD", "TEST-CMD", internal )
    val failedKey = ExecutionFailedException( Some( "key" ), "TEST-CMD", "TEST-CMD key value", internal )
    val timeout = TimeoutException( internal )
    val serialization = SerializationException( "some key", "TEST-CMD", internal )
    def any = unexpectedAny
  }

  "Recovery Policy" should {

    "log detailed report" in {
      val policy = new BasicPolicy with DetailedReports {
        override val log = mock[ Logger ]
      }

      // note: there should be tested a logger and the message
      policy.recoverFrom( rerun, default, ex.unexpectedAny ) mustEqual default
      policy.recoverFrom( rerun, default, ex.unexpectedKey ) mustEqual default
      policy.recoverFrom( rerun, default, ex.failedAny ) mustEqual default
      policy.recoverFrom( rerun, default, ex.failedKey ) mustEqual default
      policy.recoverFrom( rerun, default, ex.timeout ) mustEqual default
      policy.recoverFrom( rerun, default, ex.serialization ) mustEqual default
    }

    "log condensed report" in {
      val policy = new BasicPolicy with CondensedReports {
        override val log = mock[ Logger ]
      }

      // note: there should be tested a logger and the message
      policy.recoverFrom( rerun, default, ex.unexpectedAny ) mustEqual default
      policy.recoverFrom( rerun, default, ex.unexpectedKey ) mustEqual default
      policy.recoverFrom( rerun, default, ex.failedAny ) mustEqual default
      policy.recoverFrom( rerun, default, ex.failedKey ) mustEqual default
      policy.recoverFrom( rerun, default, ex.timeout ) mustEqual default
      policy.recoverFrom( rerun, default, ex.serialization ) mustEqual default
    }

    "fail through" in {
      val policy = new BasicPolicy with FailThrough

      policy.recoverFrom( rerun, default, ex.any ) must throwA( ex.any ).await
    }

    "recover with default" in {
      val policy = new BasicPolicy with RecoverWithDefault

      policy.recoverFrom( rerun, default, ex.any ) mustEqual default
    }
  }
}
