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

  trait ResultBuilder[ Result[ X ] ] {
    /** converts future result produced by Redis to the result of desired type */
    def toResult[ T ]( value: Future[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): Result[ T ]
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ Future ] {
    override def toResult[ T ]( future: Future[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): Future[ T ] =
      future
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ Identity ] {
    import scala.concurrent.Await
    override def toResult[ T ]( future: Future[ T ] )( implicit context: ExecutionContext, timeout: akka.util.Timeout ): Identity[ T ] =
      Await.result( future, timeout.duration )
  }

}
