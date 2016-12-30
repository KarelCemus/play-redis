package play.api.cache.redis

import scala.language.higherKinds

/**
  * Redis Sets are simply unsorted sets of objects. It is possible to add
  * elements to a Redis Set by adding new elements into the collection.
  *
  * <strong>This simplified wrapper implements only unordered Sets.</strong>
  *
  * @tparam Elem Data type of the inserted element
  * @author Karel Cemus
  */
trait RedisSet[ Elem, Result[ _ ] ] extends RedisCollection[ Set[ Elem ] ] {

  /**
    * <p>Add the specified members to the set stored at key. Specified members that are already a member of this
    * set are ignored. If key does not exist, a new set is created before adding the specified members.</p>
    *
    * @note An error is returned when the value stored at key is not a set.
    * @note <strong>Time complexity:</strong>  O(1) for each element added, so O(N) to add N elements when the
    *       command is called with multiple arguments.
    * @param element elements to be added
    * @return the set for chaining calls
    */
  def add( element: Elem* ): Result[ This ]

  /**
    * <p>Tests if the element is contained in the set. Returns true if exists, otherwise returns false</p>
    *
    * @note <strong>Time complexity:</strong> O(1)
    * @param element tested element
    * @return true if exists in the set, otherwise false
    */
  def contains( element: Elem ): Result[ Boolean ]

  /**
    * <p>Removes the specified members from the sorted set stored at key. Non existing members are ignored.
    * An error is returned when key exists and does not hold a sorted set.</p>
    *
    * @note <strong>Time complexity:</strong> O(N) where N is the number of members to be removed.
    * @param element elements to be removed
    * @return the set for chaining calls
    */
  def remove( element: Elem* ): Result[ This ]

  /**
    * <p>Returns all elements in the set</p>
    *
    * @note <strong>Time complexity:</strong> O(N) where N is the set cardinality.
    * @return all elements in the set
    */
  def toSet: Result[ Set[ Elem ] ]

  /**
    * <p>Returns the set cardinality (number of elements) of the set stored at key.</p>
    *
    * <p><strong>Time complexity:</strong> O(1)</p>
    *
    * @return size of the set or 0 if does not exists
    */
  def size: Result[ Long ]

  /**
    * <p><strong>Time complexity:</strong> O(1)</p>
    *
    * @return returns true if the set is empty or key is not used
    */
  def isEmpty: Result[ Boolean ]

  /**
    * <p><strong>Time complexity:</strong> O(1)</p>
    *
    * @return returns true if the set exists and is not empty
    */
  def nonEmpty: Result[ Boolean ]
}
