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
    * Inserts value in the list stored at key either before or after the reference value pivot.
    * When key does not exist, it is considered an empty list and no operation is performed.
    * An error is returned when key exists but does not hold a list value.
    *
    * Time complexity: O(N) where N is the number of elements to traverse before seeing the value pivot.
    *
    * @param key   cache storage key
    * @param pivot value used as markup
    * @param value value to be inserted
    * @return the length of the list after the insert operation, or None when the value pivot was not found.
    */
  def listInsert( key: String, pivot: Any, value: Any ): Future[ Option[ Long ] ]

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

  /**
    * Adds a member to a sorted set, or update its score if it already exists.
    *
    * @note If a specified member is already a member of the sorted set, the score is updated and
    *       the element reinserted at the right position to ensure the correct ordering.
    * @note The score values should be the string representation of a double precision floating point number.
    *       +inf and -inf values are valid values as well.
    * @note Time complexity: O(log(N)) for each item added, where N is the number of elements in the sorted set.
    * @param key   cache storage key
    * @param value values to be added with their score
    * @return number of inserted elements ignoring already existing. If the INCR option is specified, it returns 0
    */
  def setAdd( key: String, value: (Any, Double)* ): Future[ Long ]

  /**
    * Returns the sorted set cardinality (number of elements) of the sorted set stored at key.
    *
    * Time complexity: O(1)
    *
    * @param key cache storage key
    * @return the cardinality (number of elements) of the sorted set, or 0 if key does not exist.
    */
  def setSize( key: String ): Future[ Long ]

  /**
    * Returns the specified range of elements in the sorted set stored at key. The elements are considered to be
    * ordered from the lowest to the highest score. Lexicographical order is used for elements with equal score.
    *
    * See ZREVRANGE when you need the elements ordered from highest to lowest score (and descending lexicographical
    * order for elements with equal score).
    *
    * Both start and stop are zero-based indexes, where 0 is the first element, 1 is the next element and so on.
    * They can also be negative numbers indicating offsets from the end of the sorted set, with -1 being the last
    * element of the sorted set, -2 the penultimate element and so on.
    *
    * start and stop are inclusive ranges, so for example ZRANGE myzset 0 1 will return both the first and the
    * second element of the sorted set.
    *
    * Out of range indexes will not produce an error. If start is larger than the largest index in the sorted set,
    * or start > stop, an empty list is returned. If stop is larger than the end of the sorted set Redis will treat
    * it like it is the last element of the sorted set.
    *
    * Time complexity: O(log(N)+M) with N being the number of elements in the sorted set and M the number of
    * elements returned.
    *
    * @param key   cache storage key
    * @param start start index of the subset
    * @param end   end index of the subset
    * @tparam T expected type of the elements
    * @return the subset
    */
  def setSlice[ T: ClassTag ]( key: String, start: Long, end: Long ): Future[ Set[ T ] ]

  /**
    * Returns the rank of member in the sorted set stored at key, with the scores ordered from low to high. The
    * rank (or index) is 0-based, which means that the member with the lowest score has rank 0.
    *
    * Use ZREVRANK to get the rank of an element with the scores ordered from high to low.
    *
    * @param key   cache storage key
    * @param value tested element
    * @return index of the element if exists, None otherwise
    */
  def setRank( key: String, value: Any ): Future[ Option[ Long ] ]

  /**
    * Removes the specified members from the sorted set stored at key. Non existing members are ignored.
    *
    * An error is returned when key exists and does not hold a sorted set.
    *
    * Time complexity: O(M*log(N)) with N being the number of elements in the sorted set and M the number of elements to be removed.
    *
    * @param key   cache storage key
    * @param value values to be removed
    * @return total number of removed values, non existing are ignored
    */
  def setRemove( key: String, value: Any* ): Future[ Long ]
}
