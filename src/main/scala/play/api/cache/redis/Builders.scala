package play.api.cache.redis

import scala.language.higherKinds

/**
 * Transforms future result produces by redis implementation to the result of the desired type
 *
 * @author Karel Cemus
 */
object Builders {

  type Identity[ A ] = A
  type Future[ A ] = scala.concurrent.Future[ A ]

  trait ResultBuilder[ Result[ _ ] ] {
    /** converts future result produces by Redis to the result of desired type */
    def toResult[ T ]( value: Future[ T ] ): Result[ T ]
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ Future ] {
    override def toResult[ T ]( future: Future[ T ] ): Future[ T ] = future
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ Identity ] with Config {
    override def toResult[ T ]( future: Future[ T ] ): Identity[ T ] = future.sync
  }

}
