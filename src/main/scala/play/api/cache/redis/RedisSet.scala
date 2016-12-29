package play.api.cache.redis

import scala.language.higherKinds

/**
  * Redis Sorted Sets are simply sets of objects, sorted by either their
  * score or lexically. It is possible to add elements to a Redis Sorted
  * Set by adding new elements into the collection.
  *
  * <strong>This simplified wrapper implements only unordered Sets.</strong>
  *
  * @tparam Elem Data type of the inserted element
  * @author Karel Cemus
  */
trait RedisSet[ Elem, Result[ _ ] ] extends RedisCollection[ Set[ Elem ] ] {

  /**
    * <p>Adds all the specified members with the specified scores to the sorted set stored at key. It is possible to
    * specify multiple score / member pairs. If a specified member is already a member of the sorted set, the score
    * is updated and the element reinserted at the right position to ensure the correct ordering.</p>
    *
    * <p>If key does not exist, a new sorted set with the specified members as sole members is created, like if the
    * sorted set was empty. If the key exists but does not hold a sorted set, an error is returned.</p>
    *
    * <p>The score values should be the string representation of a double precision floating point number.
    * +inf and -inf values are valid values as well.</p>
    *
    * <p><strong>Note:</strong> This implements only lexicographical ordering to simplify use</p>
    *
    * <p><strong>Time complexity:</strong> O(log(N)) for each item added, where N is the number of elements in
    * the sorted set.</p>
    *
    * @param element elements to be added
    * @return the set for chaining calls
    */
  def add( element: Elem* ): Result[ This ]

  /**
    * <p>Tests if the element is contained in the set. Returns true if exists, otherwise returns false</p>
    *
    * <p><strong>Time complexity:</strong> O(log(N))</p>
    *
    * @param element tested element
    * @return true if exists in the set, otherwise false
    */
  def contains( element: Elem ): Result[ Boolean ]

  /**
    * <p>Removes the specified members from the sorted set stored at key. Non existing members are ignored.
    * An error is returned when key exists and does not hold a sorted set.</p>
    *
    * <p><strong>Time complexity:</strong> O(M*log(N)) with N being the number of elements in the sorted set and M the number of elements to be removed.</p>
    *
    * @param element elements to be removed
    * @return the set for chaining calls
    */
  def remove( element: Elem* ): Result[ This ]

  /**
    * <p>Returns all elements in the set</p>
    *
    * <p>Returns the specified range of elements in the sorted set stored at key. The elements are considered to
    * be ordered from the lowest to the highest score. Lexicographical order is used for elements with equal score.</p>
    *
    * <p><strong>Time complexity:</strong> O(log(N)+M) with N being the number of elements in the sorted set and M
    * the number of elements returned.</p>
    *
    * @return all elements in the set
    */
  def toSet: Result[ Set[ Elem ] ]

  /**
    * <p>Returns the sorted set cardinality (number of elements) of the sorted set stored at key.</p>
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
