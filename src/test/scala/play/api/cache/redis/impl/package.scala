package play.api.cache.redis

import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.{higherKinds, implicitConversions}

import play.api.cache.redis.exception.ExecutionFailedException

import org.specs2.matcher._

/**
  * @author Karel Cemus
  */
package object impl extends LowPriorityImplicits {

  val FailingConnector = connector.FailingConnector

  val FailThrough = new impl.FailThrough {
    override def name = "FailThrough"
  }

  val RecoverWithDefault = new impl.RecoverWithDefault {
    override def name = "RecoverWithDefault"
  }

  trait Expectation extends RedisMatcher {

    import ExceptionMatchers._

    def expectsNow[ T ]( success: => Matcher[ T ] ): Matcher[ T ] =
      expectsNow( success, success )

    def expectsNow[ T ]( success: => Matcher[ T ], default: => Matcher[ T ] ): Matcher[ T ] =
      expectsNow( success, default, throwA[ ExecutionFailedException ] )

    def expectsNow[ T ]( success: => Matcher[ T ], default: => Matcher[ T ], exception: => Matcher[ T ] ): Matcher[ T ]

    def expects[ T ]( success: => Matcher[ T ], default: => Matcher[ T ], exception: => Matcher[ T ] ): Matcher[ AsynchronousResult[ T ] ] =
      expectsNow( success, default, exception )

    def expects[ T ]( success: => Matcher[ T ], default: => Matcher[ T ] ): Matcher[ AsynchronousResult[ T ] ] =
      expects( success, default, throwA[ ExecutionFailedException ] )

    def expects[ T ]( successAndDefault: => Matcher[ T ] ): Matcher[ AsynchronousResult[ T ] ] =
      expects( successAndDefault, successAndDefault )
  }

  object AlwaysDefault extends Expectation {
    override def expectsNow[ T ]( success: => Matcher[ T ], default: => Matcher[ T ], exception: => Matcher[ T ] ): Matcher[ T ] = default
  }

  object AlwaysException extends Expectation {
    override def expectsNow[ T ]( success: => Matcher[ T ], default: => Matcher[ T ], exception: => Matcher[ T ] ): Matcher[ T ] = exception
  }

  object AlwaysSuccess extends Expectation {
    override def expectsNow[ T ]( success: => Matcher[ T ], default: => Matcher[ T ], exception: => Matcher[ T ] ): Matcher[ T ] = success
  }

  object SuccessOrDefault extends Expectation {
    override def expectsNow[ T ]( success: => Matcher[ T ], default: => Matcher[ T ], exception: => Matcher[ T ] ): Matcher[ T ] = success or default
  }

  object beUnit extends Matcher[ Any ] {
    def apply[ S <: Any ]( value: Expectable[ S ] ): MatchResult[ S ] = result( test = true, value.description + " is Unit", value.description + " is not Unit", value.evaluate )
  }

  implicit class JavaAccumulatorCache( val cache: play.cache.SyncCacheApi ) extends AnyVal {
    private type Accumulator = AtomicInteger

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrElseCounting( key: String )( accumulator: Accumulator ) = cache.getOrElseUpdate[ String ]( key, new Callable[ String ] {
      override def call( ): String = {
        // increment miss counter
        accumulator.incrementAndGet()
        // return the value to store into the cache
        "value"
      }
    } )
  }

  implicit def expected2matcher[T]( expected: => T ): Matcher[ T ] = Matchers.beEqualTo(expected)
}

trait LowPriorityImplicits {

  implicit class AccumulatorCache[ Result[ _ ] ]( cache: AbstractCacheApi[ Result ] ) {
    private type Accumulator = AtomicInteger

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrElseCounting( key: String )( accumulator: Accumulator ) = cache.getOrElse( key ) {
      // increment miss counter
      accumulator.incrementAndGet()
      // return the value to store into the cache
      "value"
    }

    /** invokes internal getOrElse but it accumulate invocations of orElse clause in the accumulator */
    def getOrFutureCounting( key: String )( accumulator: Accumulator ) = cache.getOrFuture[ String ]( key ) {
      Future.successful {
        // increment miss counter
        accumulator.incrementAndGet()
        // return the value to store into the cache
        "value"
      }
    }
  }

}
