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
  *
  * @author Karel Cemus
  */
private[ redis ] trait AbstractCacheApi[ Result[ _ ] ] {

  /** Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  def get[ T: ClassTag ]( key: String ): Result[ Option[ T ] ]

  /** Retrieve a value from the cache. If is missing, set default value with
    * given expiration and return the value.
    *
    * @param key        cache storage key
    * @param expiration expiration period in seconds.
    * @param orElse     The default function to invoke if the value was not found in cache.
    * @return stored or default record, Some if exists, otherwise None
    */
  def getOrElse[ T: ClassTag ]( key: String, expiration: Duration = Duration.Inf )( orElse: => T ): Result[ T ]

  /** Retrieve a value from the cache. If is missing, set default value with
    * given expiration and return the value.
    *
    * @param key        cache storage key
    * @param expiration expiration period in seconds.
    * @param orElse     The default function to invoke if the value was not found in cache.
    * @return stored or default record, Some if exists, otherwise None
    */
  def getOrFuture[ T: ClassTag ]( key: String, expiration: Duration = Duration.Inf )( orElse: => Future[ T ] ): Future[ T ]

  /** Determines whether value exists in cache.
    *
    * @param key cache storage key
    * @return record existence, true if exists, otherwise false
    */
  def exists( key: String ): Result[ Boolean ]

  /** Retrieves all keys matching the given pattern. This method invokes KEYS command
    *
    * '''Warning:''' complexity is O(n) where n are all keys in the database
    *
    * @param pattern valid KEYS pattern with wildcards
    * @return list of matching keys
    */
  def matching( pattern: String ): Result[ Set[ String ] ]

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key        cache storage key
    * @param value      value to store
    * @param expiration record duration in seconds
    * @return promise
    */
  def set( key: String, value: Any, expiration: Duration = Duration.Inf ): Result[ Unit ]

  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration
    *
    * @param key        cache storage key
    * @param expiration new expiration in seconds
    * @return promise
    */
  def expire( key: String, expiration: Duration ): Result[ Unit ]

  /** Remove a value under the given key from the cache
    *
    * @param key cache storage key
    * @return promise
    */
  def remove( key: String ): Result[ Unit ]

  /** Remove all values from the cache
    *
    * @param key1 cache storage key
    * @param key2 cache storage key
    * @param keys cache storage keys
    * @return promise
    */
  def remove( key1: String, key2: String, keys: String* ): Result[ Unit ]

  /** Removes all keys in arguments. The other remove methods are for syntax sugar
    *
    * @param keys cache storage keys
    * @return promise
    */
  def removeAll( keys: String* ): Result[ Unit ]

  /** <p>Removes all keys matching the given pattern. This command has no direct support
    * in Redis, it is combination of KEYS and DEL commands.</p>
    *
    * <ol>
    * <li>`KEYS pattern` command finds all keys matching the given pattern</li>
    * <li>`DEL keys` expires all of them</li>
    * </ol>
    *
    * <p>This is usable in scenarios when multiple keys contains same part of the key, such as
    * record identification, user identification, etc. For example, we may have keys such
    * as 'page/&#36;id/header', 'page/&#36;id/body', 'page/&#36;id/footer' and we want to remove them
    * all when the page is changed. We use the benefit of the '''naming convention''' we use and
    * execute `removeAllMatching( s"page/&#36;id&#47;*" )`, which invalidates everything related to
    * the given page. The benefit is we do not need to maintain the list of all keys to invalidate,
    * we invalidate them all at once.</p>
    *
    * <p>* '''Warning:''' complexity is O(n) where n are all keys in the database</p>
    *
    * @param pattern this must be valid KEYS pattern
    * @return nothing
    */
  def removeMatching( pattern: String ): Result[ Unit ]

  /** Remove all keys in cache
    *
    * @return promise
    */
  def invalidate( ): Result[ Unit ]
}

/** Synchronous and blocking implementation of the connection to the redis database */
trait CacheApi extends AbstractCacheApi[ SynchronousResult ]

/** Asynchronous non-blocking implementation of the connection to the redis database */
trait CacheAsyncApi extends AbstractCacheApi[ AsynchronousResult ]
