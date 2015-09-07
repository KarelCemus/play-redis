package play.cache.api

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

/**
 * <p>Advanced non-blocking API. It extends basic [[play.cache.api.CacheAPI]] and adds additional functionality built
 * on its improved interface [[play.cache.api.CacheAPI]].</p>
 */
trait CacheAPI20 {

  /** Retrieve a value from the cache for the given type */
  def get[ T ]( key: String )( implicit classTag: ClassTag[ T ] ): Future[ Option[ T ] ]

  /** Retrieve a value from the cache, or set it from a default function. */
  def getOrElse[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ T ]

  /** Set a value into the cache.  */
  def set[ T ]( key: String, value: T, expiration: Option[ Int ] = None )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ]

  /** Retrieve a value from the cache, or set it from a default function. */
  def setIfNotExists[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ]

  /** remove key from cache */
  def remove( key: String ): Future[ Try[ String ] ]

  /** invalidate cache */
  def invalidate( ): Future[ Try[ String ] ]

  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration */
  def expire( key: String, expiration: Int )
}
