package play.api.cache.redis

/**
  * Redis Hashes are simply hash maps with strings as keys. It is possible to
  * add elements to a Redis Hashes by adding new elements into the collection.
  *
  * <strong>This simplified wrapper implements only unordered Maps.</strong>
  *
  * @tparam Elem
  *   Data type of the inserted element
  */
trait RedisMap[Elem, Result[_]] extends RedisCollection[Map[String, Elem], Result] {

  override type This = RedisMap[Elem, Result]

  /**
    * Insert the value at the given key into the map
    *
    * @param field
    *   key
    * @param value
    *   inserted value
    * @return
    *   the map for the chaining calls
    */
  def add(field: String, value: Elem): Result[This]

  /**
    * Returns the value at the given key into the map
    *
    * @param field
    *   key
    * @return
    *   Some if the value exists in the map, None otherwise
    */
  def get(field: String): Result[Option[Elem]]

  /**
    * Returns the values stored at given keys in the map. The collection or
    * results has same size as the collection of given fields, it preserves
    * ordering.
    *
    * @param fields
    *   keys to get
    * @return
    *   Some if the value exists in the map, None otherwise
    */
  def getFields(fields: String*): Result[Seq[Option[Elem]]] = getFields(fields)

  /**
    * Returns the values stored at given keys in the map. The collection or
    * results has same size as the collection of given fields, it preserves
    * ordering.
    *
    * @param fields
    *   keys to get
    * @return
    *   Some if the value exists in the map, None otherwise
    */
  def getFields(fields: Iterable[String]): Result[Seq[Option[Elem]]]

  /**
    * <p>Tests if the field is contained in the map. Returns true if exists,
    * otherwise returns false</p>
    *
    * @note
    *   <strong>Time complexity:</strong> O(1)
    * @param field
    *   tested field
    * @return
    *   true if exists in the map, otherwise false
    */
  def contains(field: String): Result[Boolean]

  /**
    * <p>Removes the specified members from the sorted map stored at key. Non
    * existing members are ignored. An error is returned when key exists and
    * does not hold a sorted map.</p>
    *
    * @note
    *   <strong>Time complexity:</strong> O(N) where N is the number of members
    *   to be removed.
    * @param field
    *   fields to be removed
    * @return
    *   the map for chaining calls
    */
  def remove(field: String*): Result[This]

  /**
    * Increment a value at the given key in the map
    *
    * @param field
    *   key
    * @param incrementBy
    *   increment by this
    * @return
    *   value after incrementation
    */
  def increment(field: String, incrementBy: Long = 1): Result[Long]

  /**
    * <p>Returns all elements in the map</p>
    *
    * @note
    *   <strong>Time complexity:</strong> O(N) where N is the map cardinality.
    * @return
    *   all elements in the map
    */
  def toMap: Result[Map[String, Elem]]

  /**
    * Returns all keys defined in the map
    *
    * @return
    *   all used keys
    */
  def keySet: Result[Set[String]]

  /**
    * Returns all values stored in the map
    *
    * @return
    *   all stored values
    */
  def values: Result[Set[Elem]]
}
