package play.api.cache.redis

import play.api.Logger
import play.api.cache.redis.test._

import scala.concurrent.Future
import scala.util.control.NoStackTrace

class RecoveryPolicySpec extends AsyncUnitSpec {

  private val rerun = Future.successful(10)
  private val default = Future.successful(0)

  private object ex {
    private val internal = new IllegalArgumentException("Simulated Internal cause") with NoStackTrace
    val unexpectedAny: UnexpectedResponseException = UnexpectedResponseException(None, "TEST-CMD")
    val unexpectedKey: UnexpectedResponseException = UnexpectedResponseException(Some("some key"), "TEST-CMD")
    val failedAny: ExecutionFailedException = ExecutionFailedException(None, "TEST-CMD", "TEST-CMD", internal)
    val failedKey: ExecutionFailedException = ExecutionFailedException(Some("key"), "TEST-CMD", "TEST-CMD key value", internal)
    val timeout: TimeoutException = TimeoutException(internal)
    val serialization: SerializationException = SerializationException("some key", "TEST-CMD", internal)
    def any: UnexpectedResponseException = unexpectedAny
  }

  "Recovery Policy" should {

    "log detailed report" in {
      val policy = new RecoverWithDefault with DetailedReports {
        override val log: Logger = Logger(getClass)
      }

      // note: there should be tested a logger and the message
      policy.recoverFrom(rerun, default, ex.unexpectedAny) mustEqual default
      policy.recoverFrom(rerun, default, ex.unexpectedKey) mustEqual default
      policy.recoverFrom(rerun, default, ex.failedAny) mustEqual default
      policy.recoverFrom(rerun, default, ex.failedKey) mustEqual default
      policy.recoverFrom(rerun, default, ex.timeout) mustEqual default
      policy.recoverFrom(rerun, default, ex.serialization) mustEqual default
    }

    "log condensed report" in {
      val policy = new RecoverWithDefault with CondensedReports {
        override val log: Logger = Logger(getClass)
      }

      // note: there should be tested a logger and the message
      policy.recoverFrom(rerun, default, ex.unexpectedAny) mustEqual default
      policy.recoverFrom(rerun, default, ex.unexpectedKey) mustEqual default
      policy.recoverFrom(rerun, default, ex.failedAny) mustEqual default
      policy.recoverFrom(rerun, default, ex.failedKey) mustEqual default
      policy.recoverFrom(rerun, default, ex.timeout) mustEqual default
      policy.recoverFrom(rerun, default, ex.serialization) mustEqual default
    }

    "fail through" in {
      val policy = new FailThrough {}
      policy.recoverFrom(rerun, default, ex.any).assertingFailure(ex.any)
    }

    "recover with default" in {
      val policy = new RecoverWithDefault {}
      policy.recoverFrom(rerun, default, ex.any) mustEqual default
    }
  }

}
