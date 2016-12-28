package play.api.cache.redis

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import akka.util.Timeout

/**
  * Internal non-blocking Redis API implementing REDIS protocol
  *
  * @see http://redis.io/commands
  * @author Karel Cemus
  */
private[ redis ] trait RedisConnector {

  /** implicit execution context */
  implicit def context: ExecutionContext

  /** implicit ask timeout */
  implicit def timeout: Timeout

  /** Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ]

  /** Determines whether value exists in cache.
    *
    * @param key cache storage key
    * @return record existence, true if exists, otherwise false
    */
  def exists( key: String ): Future[ Boolean ]

  /** Retrieves all keys matching the given pattern. This method invokes KEYS command
    *
    * '''Warning:''' complexity is O(n) where n are all keys in the database
    *
    * @param pattern valid KEYS pattern with wildcards
    * @return list of matching keys
    */
  def matching( pattern: String ): Future[ Set[ String ] ]

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key        cache storage key
    * @param value      value to store
    * @param expiration record duration in seconds
    * @return promise
    */
  def set( key: String, value: Any, expiration: Duration = Duration.Inf ): Future[ Unit ]

  /** Set a value into the cache, if the key is not used. Otherwise ignore.
    *
    * @param key   cache storage key
    * @param value value to set
    * @return true if set was successful, false if key was already defined
    */
  def setIfNotExists( key: String, value: Any ): Future[ Boolean ]

  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration
    *
    * @param key        cache storage key
    * @param expiration new expiration in seconds
    * @return promise
    */
  def expire( key: String, expiration: Duration ): Future[ Unit ]

  /** Removes all keys in arguments. The other remove methods are for syntax sugar
    *
    * @param keys cache storage keys
    * @return promise
    */
  def remove( keys: String* ): Future[ Unit ]

  /** Remove all keys in cache
    *
    * @return promise
    */
  def invalidate( ): Future[ Unit ]

  /** Sends PING command to REDIS and expects PONG in return
    *
    * @return promise
    */
  def ping( ): Future[ Unit ]

  /** Increments the stored string value representing 10-based signed integer
    * by given value.
    *
    * @param key cache storage key
    * @param by  size of increment
    * @return the value after the increment
    */
  def increment( key: String, by: Long ): Future[ Long ]

  /** If key already exists and is a string, this command appends the value at the
    * end of the string. If key does not exist it is created and set as an empty string,
    * so APPEND will be similar to SET in this special case.
    *
    * @param key   cache storage key
    * @param value value to be appended
    * @return number of characters of current value
    */
  def append( key: String, value: String ): Future[ Long ]

  /**
    * Insert (LPUSH) all the specified values at the head of the list stored at key.
    * If key does not exist, it is created as empty list before performing the push operations.
    * When key holds a value that is not a list, an error is returned.
    *
    * Time complexity: O(1)
    *
    * @param key   cache storage key
    * @param value prepended values
    * @return new length of the list
    */
  def listPrepend( key: String, value: Any* ): Future[ Long ]

  /**
    * Insert (RPUSH) all the specified values at the tail of the list stored at key. If key
    * does not exist, it is created as empty list before performing the push operation.
    * When key holds a value that is not a list, an error is returned.
    *
    * Time complexity: O(1)
    *
    * @param key   cache storage key
    * @param value appended values
    * @return new length of the list
    */
  def listAppend( key: String, value: Any* ): Future[ Long ]

  /**
    * Returns the length of the list stored at key (LLEN). If key does not exist, it is interpreted as an empty
    * list and 0 is returned. An error is returned when the value stored at key is not a list.
    *
    * Time complexity: O(1)
    *
    * @param key cache storage key
    * @return length of the list
    */
  def listSize( key: String ): Future[ Long ]

  /**
    * Sets the list element at index to value. For more information on the index argument, see LINDEX. An error is
    * returned for out of range indexes.
    *
    * Time complexity: O(N) where N is the length of the list. Setting either the first or the last element
    * of the list is O(1).
    *
    * @param key      cache storage key
    * @param position position to be overwritten
    * @param value    value to be set
    * @return promise
    */
  def listSetAt( key: String, position: Int, value: Any ): Future[ Unit ]

  /**
    * Removes and returns the first element of the list stored at key (LPOP).
    *
    * Time complexity: O(1)
    *
    * @param key cache storage key
    * @tparam T type of the value
    * @return head of the list, if existed
    */
  def listHeadPop[ T: ClassTag ]( key: String ): Future[ Option[ T ] ]

  /**
    * Returns the specified elements of the list stored at key (LRANGE). The offsets start and stop are zero-based
    * indexes, with 0 being the first element of the list (the head of the list), 1 being the next element and so on.
    *
    * These offsets can also be negative numbers indicating offsets starting at the end of the list. For example,
    * -1 is the last element of the list, -2 the penultimate, and so on.
    *
    * Time complexity: O(S+N) where S is the distance of start offset from HEAD for small lists, from nearest end
    * (HEAD or TAIL) for large lists; and N is the number of elements in the specified range.
    *
    * @param key   cache storage key
    * @param start initial index of the subset
    * @param end   last index of the subset (included)
    * @tparam T type of the values
    * @return subset of existing set
    */
  def listSlice[ T: ClassTag ]( key: String, start: Int, end: Int ): Future[ List[ T ] ]

  /**
    * Removes (LREM) the first count occurrences of elements equal to value from the list stored at key. The count
    * argument influences the operation in the following ways:
    * count > 0: Remove elements equal to value moving from head to tail.
    * count < 0: Remove elements equal to value moving from tail to head.
    * count = 0: Remove all elements equal to value.
    *
    * @param key   cache storage key
    * @param value value to be removed
    * @param count number of elements to be removed
    * @return number of removed elements
    */
  def listRemove( key: String, value: Any, count: Int ): Future[ Long ]

  /**
    * Trim an existing list so that it will contain only the specified range of elements specified. Both start and stop
    * are zero-based indexes, where 0 is the first element of the list (the head), 1 the next element and so on.
    *
    * For example: LTRIM foobar 0 2 will modify the list stored at foobar so that only the first three elements of
    * the list will remain. start and end can also be negative numbers indicating offsets from the end of the list,
    * where -1 is the last element of the list, -2 the penultimate element and so on.
    *
    * Time complexity: O(N) where N is the number of elements to be removed by the operation.
    *
    * @param key   cache storage key
    * @param start initial index of preserved subset
    * @param end   last index of preserved subset (included)
    * @return promise
    */
  def listTrim( key: String, start: Int, end: Int ): Future[ Unit ]
}
