package play.api.cache.redis

import scala.collection.immutable.TreeSet

trait RedisSortedSet[Elem, Result[_]] extends RedisCollection[TreeSet[Elem], Result] {
  override type This = RedisSortedSet[Elem, Result]

  /**
    * Adds all the specified members with the specified scores to the sorted set
    * stored at key. It is possible to specify multiple score / member pairs. If
    * a specified member is already a member of the sorted set, the score is
    * updated and the element reinserted at the right position to ensure the
    * correct ordering.
    *
    * If key does not exist, a new sorted set with the specified members as sole
    * members is created, like if the sorted set was empty.
    *
    * @note
    *   If the key exists but does not hold a sorted set, an error is returned.
    * @note
    *   <strong>Time complexity:</strong> O(log(N)) for each item added, where N
    *   is the number of elements in the sorted set.
    * @param scoreValues
    *   values and corresponding scores to be added
    * @return
    *   the sorted set for chaining calls
    */
  def add(scoreValues: (Double, Elem)*): Result[This]

  /**
    * <p>Tests if the element is contained in the sorted set. Returns true if
    * exists, otherwise returns false</p>
    *
    * @note
    *   <strong>Time complexity:</strong> O(1)
    * @param element
    *   tested element
    * @return
    *   true if exists in the set, otherwise false
    */
  def contains(element: Elem): Result[Boolean]

  /**
    * <p>Removes the specified members from the sorted set stored at key. Non
    * existing members are ignored. An error is returned when key exists and
    * does not hold a sorted set.</p>
    *
    * @note
    *   <strong>Time complexity:</strong> O(M*log(N)) with N being the number of
    *   elements in the sorted set and M the number of elements to be removed.
    * @param element
    *   elements to be removed
    * @return
    *   the sorted set for chaining calls
    */
  def remove(element: Elem*): Result[This]

  /**
    * Returns the specified range of elements in the sorted set stored at key
    * which sorted in order specified by param `isReverse`.
    * @param start
    *   the start index of the range
    * @param stop
    *   the stop index of the range
    * @param isReverse
    *   whether sorted in descending order or not
    * @return
    */
  def range(start: Long, stop: Long, isReverse: Boolean = false): Result[Seq[Elem]]
}
