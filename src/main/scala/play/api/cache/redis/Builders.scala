package play.api.cache.redis

import scala.concurrent.Future
import scala.language.higherKinds

/**
 * Transforms future result produces by redis implementation to the result of the desired type
 *
 * @author Karel Cemus
 */
object Builders {

  type Identity[ A ] = A
  type Sync[ A ] = Identity[ A ]
  type Async[ A ] = Future[ A ]
  type Builder = ResultBuilder[ _ ]

  trait ResultBuilder[ Result[ _ ] ] {
    /** converts future result produces by Redis to the result of desired type */
    def toResult[ T ]( value: Future[ T ] ): Result[ T ]
  }

  /** returns the future itself without any transformation */
  object AsynchronousBuilder extends ResultBuilder[ Async ] {
    override def toResult[ T ]( future: Future[ T ] ): Async[ T ] = future
  }

  /** converts the future into the value */
  object SynchronousBuilder extends ResultBuilder[ Sync ] with Config {
    override def toResult[ T ]( future: Future[ T ] ): Sync[ T ] = future.sync
  }
}
