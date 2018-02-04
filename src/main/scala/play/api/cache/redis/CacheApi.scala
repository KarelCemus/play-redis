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

  /** Retrieve the values of all specified keys from the cache.
    *
    * @param key cache storage keys
    * @return stored record, Some if exists, otherwise None
    */
  def getAll[ T: ClassTag ]( key: String* ): Result[ Seq[ Option[ T ] ] ]

  /** Retrieve a value from the cache. If is missing, set default value with
    * given expiration and return the value.
    *
    * Lazy invocation (default): The method **does wait** for the result of
    * `set` and when it fails, it applies the recovery policy.
    *
    * Eager invocation: The method **does not wait** for the result of `set`
    * if the value is not cached and also does not apply recovery policy if
    * the `set` fails.
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
    * Lazy invocation (default): The method **does wait** for the result of
    * `set` and when it fails, it applies the recovery policy.
    *
    * Eager invocation: The method **does not wait** for the result of `set`
    * if the value is not cached and also does not apply recovery policy if
    * the `set` fails.
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
  def matching( pattern: String ): Result[ Seq[ String ] ]

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key        cache storage key
    * @param value      value to store
    * @param expiration record duration in seconds
    * @return promise
    */
  def set( key: String, value: Any, expiration: Duration = Duration.Inf ): Result[ Done ]

  /** Set a value into the cache if the given key is not already used, otherwise do nothing.
    * Expiration time in seconds (0 second means eternity).
    *
    * Note: When expiration is defined, it is not an atomic operation. Redis does not
    * provide a command for store-if-not-exists with duration. First, it sets the value
    * if exists. Then, if the operation succeeded, it sets its expiration date.
    *
    * Note: When recovery policy used, it recovers with TRUE to indicate
    * **"the lock was acquired"** despite actually **not storing** anything.
    *
    * @param key        cache storage key
    * @param value      value to store
    * @param expiration record duration in seconds
    * @return true if value was set, false if was ignored because it existed before
    */
  def setIfNotExists( key: String, value: Any, expiration: Duration = Duration.Inf ): Result[ Boolean ]

  /** Sets the given keys to their respective values for eternity. If any value is null,
    * the particular key is excluded from the operation and removed from cache instead.
    * The operation is atomic when there are no nulls. It replaces existing values.
    *
    * @param keyValues cache storage key-value pairs to store
    * @return promise
    */
  def setAll( keyValues: (String, Any)* ): Result[ Done ]

  /** Sets the given keys to their respective values for eternity. It sets all values if none of them
    * exist, if at least a single of them exists, it does not set any value, thus it is either all or none.
    * If any value is null, the particular key is excluded from the operation and removed from cache instead.
    * The operation is atomic when there are no nulls.
    *
    * @param keyValues cache storage key-value pairs to store
    * @return true if value was set, false if any value already existed before
    */
  def setAllIfNotExist( keyValues: (String, Any)* ): Result[ Boolean ]

  /** If key already exists and is a string, this command appends the value at the end of
    * the string. If key does not exist it is created and set as an empty string, so APPEND
    * will be similar to SET in this special case.
    *
    * If it sets new value, it subsequently calls EXPIRE to set required expiration time
    *
    * @param key        cache storage key
    * @param value      value to append
    * @param expiration record duration, applies only when appends to nothing
    * @return promise
    */
  def append( key: String, value: String, expiration: Duration = Duration.Inf ): Result[ Done ]

  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration
    *
    * @param key        cache storage key
    * @param expiration new expiration in seconds
    * @return promise
    */
  def expire( key: String, expiration: Duration ): Result[ Done ]

  /** Remove a value under the given key from the cache
    *
    * @param key cache storage key
    * @return promise
    */
  def remove( key: String ): Result[ Done ]

  /** Remove all values from the cache
    *
    * @param key1 cache storage key
    * @param key2 cache storage key
    * @param keys cache storage keys
    * @return promise
    */
  def remove( key1: String, key2: String, keys: String* ): Result[ Done ]

  /** Removes all keys in arguments. The other remove methods are for syntax sugar
    *
    * @param keys cache storage keys
    * @return promise
    */
  def removeAll( keys: String* ): Result[ Done ]

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
  def removeMatching( pattern: String ): Result[ Done ]

  /** Remove all keys in cache
    *
    * @return promise
    */
  def invalidate( ): Result[ Done ]

  /** Increments the stored string value representing 10-based signed integer
    * by given value.
    *
    * @param key cache storage key
    * @param by  size of increment
    * @return the value after the increment
    * @since 1.3.0
    */
  def increment( key: String, by: Long = 1 ): Result[ Long ]


  /** Decrements the stored string value representing 10-based signed integer
    * by given value.
    *
    * @param key cache storage key
    * @param by  size of decrement
    * @return the value after the decrement
    * @since 1.3.0
    */
  def decrement( key: String, by: Long = 1 ): Result[ Long ]

  /**
    * Scala wrapper around Redis list-related commands. This simplifies use of the lists.
    *
    * @param key the key storing the list
    * @tparam T type of elements within the list
    * @return Scala wrapper
    */
  def list[ T: ClassTag ]( key: String ): RedisList[ T, Result ]

  /**
    * Scala wrapper around Redis set-related commands. This simplifies use of the sets.
    *
    * @param key the key storing the set
    * @tparam T type of elements within the set
    * @return Scala wrapper
    */
  def set[ T: ClassTag ]( key: String ): RedisSet[ T, Result ]

  /**
    * Scala wrapper around Redis hash-related commands. This simplifies use of the hashes, i.e., maps.
    *
    * @param key the key storing the map
    * @tparam T type of elements within the map
    * @return Scala wrapper
    */
  def map[ T: ClassTag ]( key: String ): RedisMap[ T, Result ]
}

/** Synchronous and blocking implementation of the connection to the redis database */
trait CacheApi extends AbstractCacheApi[ SynchronousResult ]

/** Asynchronous non-blocking implementation of the connection to the redis database */
trait CacheAsyncApi extends AbstractCacheApi[ AsynchronousResult ]
