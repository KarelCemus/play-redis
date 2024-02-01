package play.api.cache.redis

private[redis] trait RedisCollection[Collection, Result[_]] {

  type This >: this.type

  /**
    * Returns the length of the collection stored at key. If key does not exist,
    * it is interpreted as an empty collection and 0 is returned. An error is
    * returned when the value stored at key is not a proper collection.
    *
    * Time complexity: O(1)
    *
    * @return size of the list
    */
  def size: Result[Long]

  def isEmpty: Result[Boolean]

  def nonEmpty: Result[Boolean]
}
