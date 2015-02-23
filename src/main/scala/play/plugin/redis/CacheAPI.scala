package play.plugin.redis

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

/**
 * <p>Non-blocking cache API, inspired by basic Play [[play.api.cache.CacheAPI]].</p>
 */
trait CacheAPI {

  /** Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  def get( key: String ): Future[ Option[ String ] ]

  /** Determines whether value exists in cache.
    *
    * @param key cache storage key
    * @return record existence, true if exists, otherwise false
    */
  def exists( key: String ): Future[ Boolean ]

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key cache storage key
    * @param value value to store
    * @param expiration record duration in seconds
    * @return operation success
    */
  def set( key: String, value: String, expiration: Int ): Future[ Try[ String ] ]

  /** Remove a value from the cache
    * @param key cache storage key
    * @return operation success
    */
  def remove( key: String ): Future[ Try[ String ] ]

  /** Remove all keys in cache
    *
    * @return operation success
    */
  def invalidate( ): Future[ Try[ String ] ]
}

/**
 * <p>Advanced non-blocking API. It extends basic [[play.api.cache.CacheAPI]] and adds additional functionality built
 * on its improved interface [[play.plugin.redis.CacheAPI]].</p>
 *
 * @author Karel Cemus
 */
trait ExtendedCacheAPI {

  /** Retrieve a value from the cache for the given type */
  def get[ T ]( key: String )( implicit classTag: ClassTag[ T ] ): Future[ Option[ T ] ]

  /** Retrieve a value from the cache, or set it from a default function. */
  def getOrElse[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: () => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ T ]

  /** Set a value into the cache.  */
  def set[ T ]( key: String, value: T, expiration: Option[ Int ] = None )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ]

  /** Retrieve a value from the cache, or set it from a default function. */
  def setIfNotExists[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: () => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ]

  /** remove key from cache */
  def remove( key: String ): Future[ Try[ String ] ]

  /** invalidate cache */
  def invalidate( ): Future[ Try[ String ] ]
}