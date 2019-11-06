package play.api.cache.redis.connector

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
  * Internal non-blocking Redis API implementing REDIS protocol
  *
  * Subset of REDIS commands, basic commands.
  *
  * @see https://redis.io/commands
  */
private[redis] trait CoreCommands {

  /**
    * Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  def get[T: ClassTag](key: String): Future[Option[T]]

  /**
    * Retrieve a values from the cache.
    *
    * @param keys cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  def mGet[T: ClassTag](keys: String*): Future[Seq[Option[T]]]

  /**
    * Determines whether value exists in cache.
    *
    * @param key cache storage key
    * @return record existence, true if exists, otherwise false
    */
  def exists(key: String): Future[Boolean]

  /**
    * Retrieves all keys matching the given pattern. This method invokes KEYS command
    *
    * '''Warning:''' complexity is O(n) where n are all keys in the database
    *
    * @param pattern valid KEYS pattern with wildcards
    * @return list of matching keys
    */
  def matching(pattern: String): Future[Seq[String]]

  /**
    * Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key         cache storage key
    * @param value       value to store
    * @param expiration  record duration in seconds
    * @param ifNotExists set only if the key does not exist
    * @return promise
    */
  def set(key: String, value: Any, expiration: Duration = Duration.Inf, ifNotExists: Boolean = false): Future[Boolean]

  /**
    * Set a value into the cache. Expiration time is the eternity.
    *
    * @param keyValues cache storage key-value pairs to store
    * @return promise
    */
  def mSet(keyValues: (String, Any)*): Future[Unit]

  /**
    * Set a value into the cache. Expiration time is the eternity.
    * It either set all values or it sets none if any of them already exists.
    *
    * @param keyValues cache storage key-value pairs to store
    * @return promise
    */
  def mSetIfNotExist(keyValues: (String, Any)*): Future[Boolean]

  /**
    * refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration
    *
    * @param key        cache storage key
    * @param expiration new expiration in seconds
    * @return promise
    */
  def expire(key: String, expiration: Duration): Future[Unit]

  /**
    * returns the remaining time to live of a key that has an expire set,
    * useful, e.g., when we want to check remaining session duration
    *
    * @param key cache storage key
    * @return the remaining time to live of a key in milliseconds
    */
  def expiresIn(key: String): Future[Option[Duration]]

  /**
    * Removes all keys in arguments. The other remove methods are for syntax sugar
    *
    * @param keys cache storage keys
    * @return promise
    */
  def remove(keys: String*): Future[Unit]

  /**
    * Remove all keys in cache
    *
    * @return promise
    */
  def invalidate(): Future[Unit]

  /**
    * Sends PING command to REDIS and expects PONG in return
    *
    * @return promise
    */
  def ping(): Future[Unit]

  /**
    * Increments the stored string value representing 10-based signed integer
    * by given value.
    *
    * @param key cache storage key
    * @param by  size of increment
    * @return the value after the increment
    */
  def increment(key: String, by: Long): Future[Long]

  /**
    * If key already exists and is a string, this command appends the value at the
    * end of the string. If key does not exist it is created and set as an empty string,
    * so APPEND will be similar to SET in this special case.
    *
    * @param key   cache storage key
    * @param value value to be appended
    * @return number of characters of current value
    */
  def append(key: String, value: String): Future[Long]
}

/**
  * Internal non-blocking Redis API implementing REDIS protocol
  *
  * Subset of REDIS commands, Hash-related commands.
  *
  * @see https://redis.io/commands
  */
private[redis] trait HashCommands {

  /**
    * Removes the specified fields from the hash stored at key. Specified fields that do not exist within this
    * hash are ignored. If key does not exist, it is treated as an empty hash and this command returns 0.
    *
    * Time complexity: O(N) where N is the number of fields to be removed.
    *
    * @param key   cache storage key
    * @param field fields to be removed
    * @return the number of fields that were removed from the hash, not including specified but non existing fields.
    */
  def hashRemove(key: String, field: String*): Future[Long]

  /**
    * Returns if field is an existing field in the hash stored at key.
    *
    * Time complexity: O(1)
    *
    * @param key   cache storage key
    * @param field tested field name
    * @return true if the field exists, false otherwise
    */
  def hashExists(key: String, field: String): Future[Boolean]

  /**
    * Returns the value associated with field in the hash stored at key.
    *
    * Time complexity: O(1)
    *
    * @param key   cache storage key
    * @param field accessed field
    * @return Some value if the field exists, otherwise None
    */
  def hashGet[T: ClassTag](key: String, field: String): Future[Option[T]]

  /**
    * Returns the values associated with fields in the hash stored at given keys.
    *
    * Time complexity: O(n), where n is number of fields
    *
    * @param key    cache storage key
    * @param fields accessed fields to get
    * @return Some value if the field exists, otherwise None
    */
  def hashGet[T: ClassTag](key: String, fields: Seq[String]): Future[Seq[Option[T]]]

  /**
    * Returns all fields and values of the hash stored at key. In the returned value, every field name is followed
    * by its value, so the length of the reply is twice the size of the hash.
    *
    * Time complexity: O(N) where N is the size of the hash.
    *
    * @param key cache storage key
    * @tparam T expected type of the elements
    * @return the stored map
    */
  def hashGetAll[T: ClassTag](key: String): Future[Map[String, T]]

  /**
    * Increment a value at the given key in the map
    *
    * @param key cache storage key
    * @param field key
    * @param incrementBy increment by this
    * @return value after incrementation
    */
  def hashIncrement(key: String, field: String, incrementBy: Long): Future[Long]

  /**
    * Returns the number of fields contained in the hash stored at key.
    *
    * Time complexity: O(1)
    *
    * @param key cache storage key
    * @return size of the hash
    */
  def hashSize(key: String): Future[Long]

  /**
    * Returns all field names in the hash stored at key.
    *
    * Time complexity: O(N) where N is the size of the hash.
    *
    * @param key cache storage key
    * @return set of field names
    */
  def hashKeys(key: String): Future[Set[String]]

  /**
    * Sets field in the hash stored at key to value. If key does not exist, a new key holding a hash is created. If field already exists in the hash, it is overwritten.
    *
    * Time complexity: O(1)
    *
    * @param key   cache storage key
    * @param field field to be set
    * @param value value to be set
    * @return true if the field was newly set, false if was updated
    */
  def hashSet(key: String, field: String, value: Any): Future[Boolean]

  /**
    * Returns all values in the hash stored at key.
    *
    * Time complexity: O(N) where N is the size of the hash.
    *
    * @param key cache storage key
    * @return all values in the hash object
    */
  def hashValues[T: ClassTag](key: String): Future[Set[T]]
}

/**
  * Internal non-blocking Redis API implementing REDIS protocol
  *
  * Subset of REDIS commands, List-related commands.
  *
  * @see https://redis.io/commands
  */
private[redis] trait ListCommands {

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
  def listPrepend(key: String, value: Any*): Future[Long]

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
  def listAppend(key: String, value: Any*): Future[Long]

  /**
    * Returns the length of the list stored at key (LLEN). If key does not exist, it is interpreted as an empty
    * list and 0 is returned. An error is returned when the value stored at key is not a list.
    *
    * Time complexity: O(1)
    *
    * @param key cache storage key
    * @return length of the list
    */
  def listSize(key: String): Future[Long]

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
  def listInsert(key: String, pivot: Any, value: Any): Future[Option[Long]]

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
  def listSetAt(key: String, position: Int, value: Any): Future[Unit]

  /**
    * Removes and returns the first element of the list stored at key (LPOP).
    *
    * Time complexity: O(1)
    *
    * @param key cache storage key
    * @tparam T type of the value
    * @return head of the list, if existed
    */
  def listHeadPop[T: ClassTag](key: String): Future[Option[T]]

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
  def listSlice[T: ClassTag](key: String, start: Int, end: Int): Future[Seq[T]]

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
  def listRemove(key: String, value: Any, count: Int): Future[Long]

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
  def listTrim(key: String, start: Int, end: Int): Future[Unit]
}

/**
  * Internal non-blocking Redis API implementing REDIS protocol
  *
  * Subset of REDIS commands, unordered Set-related commands.
  *
  * @see https://redis.io/commands
  */
private[redis] trait SetCommands {

  /**
    * Add the specified members to the set stored at key. Specified members that are already a member of this set
    * are ignored. If key does not exist, a new set is created before adding the specified members.
    *
    * An error is returned when the value stored at key is not a set.
    *
    * @note Time complexity: O(1) for each element added, so O(N) to add N elements when the command is called
    *       with multiple arguments.
    * @param key   cache storage key
    * @param value values to be added
    * @return number of inserted elements ignoring already existing
    */
  def setAdd(key: String, value: Any*): Future[Long]

  /**
    * Returns the set cardinality (number of elements) of the set stored at key.
    *
    * Time complexity: O(1)
    *
    * @param key cache storage key
    * @return the cardinality (number of elements) of the set, or 0 if key does not exist.
    */
  def setSize(key: String): Future[Long]

  /**
    * Returns all the members of the set value stored at key.
    *
    * This has the same effect as running SINTER with one argument key.
    *
    * Time complexity: O(N) where N is the set cardinality.
    *
    * @param key cache storage key
    * @tparam T expected type of the elements
    * @return the subset
    */
  def setMembers[T: ClassTag](key: String): Future[Set[T]]

  /**
    * Returns if member is a member of the set stored at key.
    *
    * Time complexity: O(1)
    *
    * @param key   cache storage key
    * @param value tested element
    * @return true if the element exists in the set, otherwise false
    */
  def setIsMember(key: String, value: Any): Future[Boolean]

  /**
    * Remove the specified members from the set stored at key. Specified members that are not a member of this set
    * are ignored. If key does not exist, it is treated as an empty set and this command returns 0.
    *
    * An error is returned when the value stored at key is not a set.
    *
    * Time complexity: O(N) where N is the number of members to be removed.
    *
    * @param key   cache storage key
    * @param value values to be removed
    * @return total number of removed values, non existing are ignored
    */
  def setRemove(key: String, value: Any*): Future[Long]
}

/**
  * Internal non-blocking Redis API implementing REDIS protocol
  *
  * @see https://redis.io/commands
  */
trait RedisConnector extends AnyRef
  with CoreCommands
  with ListCommands
  with SetCommands
  with HashCommands
