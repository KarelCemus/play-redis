package play.api.cache.redis

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
    /** converts future result produces by Redis to the result of desired type */
    def toResult[ T ]( value: Future[ T ] )( implicit configuration: Configuration ): Result[ T ]
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ Future ] {
    override def toResult[ T ]( future: Future[ T ] )( implicit configuration: Configuration ): Future[ T ] = future
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ Identity ] with Implicits {
    override def toResult[ T ]( future: Future[ T ] )( implicit configuration: Configuration ): Identity[ T ] = future.sync( configuration.timeout )
  }
}
