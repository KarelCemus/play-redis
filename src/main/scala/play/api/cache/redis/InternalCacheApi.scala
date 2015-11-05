package play.api.cache.redis

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scala.reflect.ClassTag

/**
 * <p>Cache API inspired by basic Play play.api.cache.CacheApi. It implements all its
 * operations and in addition it declares couple more useful operations handful
 * with cache storage. Furthermore, due to its parametrization it allows to decide
 * whether it produces blocking results or non-blocking promises.</p>
 */
trait InternalCacheApi[ Result[ _ ] ] {

  /** Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  def get[ T: ClassTag ]( key: String ): Result[ Option[ T ] ]

  /** Retrieve a value from the cache. If is missing, set default value with
    * given expiration and return the value.
    *
    * @param key cache storage key
    * @param expiration expiration period in seconds.
    * @param orElse The default function to invoke if the value was not found in cache.
    * @return stored or default record, Some if exists, otherwise None
    */
  def getOrElse[ T: ClassTag ]( key: String, expiration: Duration = Duration.Inf )( orElse: => T ): Result[ T ]

  /** Retrieve a value from the cache. If is missing, set default value with
    * given expiration and return the value.
    *
    * @param key cache storage key
    * @param expiration expiration period in seconds.
    * @param orElse The default function to invoke if the value was not found in cache.
    * @return stored or default record, Some if exists, otherwise None
    */
  def getOrFuture[ T: ClassTag ]( key: String, expiration: Duration = Duration.Inf )( orElse: => Future[ T ] ): Future[ T ]

  /** Determines whether value exists in cache.
    *
    * @param key cache storage key
    * @return record existence, true if exists, otherwise false
    */
  def exists( key: String ): Result[ Boolean ]

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key cache storage key
    * @param value value to store
    * @param expiration record duration in seconds
    * @return promise
    */
  def set( key: String, value: Any, expiration: Duration = Duration.Inf ): Result[ Unit ]

  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration
    * @param key cache storage key
    * @param expiration new expiration in seconds
    * @return promise
    */
  def expire( key: String, expiration: Duration ): Result[ Unit ]

  /** Remove a value under the given key from the cache
    * @param key cache storage key
    * @return promise
    */
  def remove( key: String ): Result[ Unit ]

  /** Remove all values from the cache
    * @param key1 cache storage key
    * @param key2 cache storage key
    * @param keys cache storage keys
    * @return promise
    */
  def remove( key1: String, key2: String, keys: String* ): Result[ Unit ]

  /** Remove all keys in cache
    *
    * @return promise
    */
  def invalidate( ): Result[ Unit ]
}
