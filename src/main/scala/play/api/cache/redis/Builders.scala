package play.api.cache.redis

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  * Transforms future result produced by redis implementation to the result of the desired type
  *
  * @author Karel Cemus
  */
object Builders {

  type Identity[ A ] = A
  type Future[ A ] = scala.concurrent.Future[ A ]

  trait ResultBuilder[ Result[ _ ] ] {
    self =>
    /** converts future result produced by Redis to the result of desired type */
    def toResult[ T ]( value: Future[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): Result[ T ]

    /** chains calls and passes the output of the first as the input of the subsequent */
    def andThen[ A, B ]( source: => Result[ A ] )( andThen: A => Result[ B ] )( implicit context: ExecutionContext ): Result[ B ]

    /** chains calls and passes the output of the first as the input of the subsequent expecting the future result */
    def andFuture[ A, B ]( source: => Result[ A ] )( andThen: A => Future[ B ] )( implicit context: ExecutionContext ): Future[ B ]

    /** Chain results */
    implicit class ResultChainer[ A ]( source: => Result[ A ] ) {

      /** chains calls and passes the output of the first as the input of the subsequent */
      def andThen[ B ]( andThen: A => Result[ B ] )( implicit context: ExecutionContext ) = self.andThen( source )( andThen )

      /** chains calls and passes the output of the first as the input of the subsequent expecting the future result */
      def andFuture[ B ]( andThen: A => Future[ B ] )( implicit context: ExecutionContext ) = self.andFuture( source )( andThen )
    }

  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ Future ] {
    override def toResult[ T ]( future: Future[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): Future[ T ] = future

    override def andThen[ A, B ]( source: => Future[ A ] )( andThen: ( A ) => Future[ B ] )( implicit context: ExecutionContext ) = source.flatMap( andThen )

    override def andFuture[ A, B ]( source: => Future[ A ] )( andThen: ( A ) => Future[ B ] )( implicit context: ExecutionContext ) = source.flatMap( andThen )
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ Identity ] with Implicits {
    override def toResult[ T ]( future: Future[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): Identity[ T ] = future.sync( timeout.duration )

    override def andThen[ A, B ]( source: => Identity[ A ] )( andThen: ( A ) => Identity[ B ] )( implicit context: ExecutionContext ) = andThen( source )

    override def andFuture[ A, B ]( source: => Identity[ A ] )( andThen: ( A ) => Future[ B ] )( implicit context: ExecutionContext ) = andThen( source )
  }

}
