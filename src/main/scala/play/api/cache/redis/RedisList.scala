package play.api.cache.redis

/**
  * Redis Lists are simply lists of strings, sorted by insertion order.
  * It is possible to add elements to a Redis List pushing new elements
  * on the head (on the left) or on the tail (on the right) of the list.
  *
  * @tparam Elem Data type of the inserted element
  */
trait RedisList[Elem, Result[_]] extends RedisCollection[List[Elem], Result] {

  override type This = RedisList[Elem, Result]

  /**
    * Insert all the specified values at the head of the list stored at key.
    * If key does not exist, it is created as empty list before performing
    * the push operations. When key holds a value that is not a list, an
    * error is returned.
    *
    * It is possible to push multiple elements using a single command call
    * just specifying multiple arguments at the end of the command. Elements
    * are inserted one after the other to the head of the list, from the
    * leftmost element to the rightmost element. So for instance the command
    * LPUSH mylist a b c will result into a list containing c as first element,
    * b as second element and a as third element.
    *
    * @param element element to be prepended
    * @return this collection to chain commands
    */
  def prepend(element: Elem): Result[This]

  /**
    * Insert all the specified values at the tail of the list stored at key.
    * If key does not exist, it is created as empty list before performing
    * the push operation. When key holds a value that is not a list, an error
    * is returned.
    * *
    * It is possible to push multiple elements using a single command call
    * just specifying multiple arguments at the end of the command. Elements
    * are inserted one after the other to the tail of the list, from the
    * leftmost element to the rightmost element. So for instance the command
    * RPUSH mylist a b c will result into a list containing a as first element,
    * b as second element and c as third element.
    *
    * @param element to be apended
    * @return this collection to chain commands
    */
  def append(element: Elem): Result[This]

  /**
    * Insert all the specified values at the head of the list stored at key.
    * If key does not exist, it is created as empty list before performing
    * the push operations. When key holds a value that is not a list, an
    * error is returned.
    *
    * It is possible to push multiple elements using a single command call
    * just specifying multiple arguments at the end of the command. Elements
    * are inserted one after the other to the head of the list, from the
    * leftmost element to the rightmost element. So for instance the command
    * LPUSH mylist a b c will result into a list containing c as first element,
    * b as second element and a as third element.
    *
    * @param element element to be prepended
    * @return this collection to chain commands
    */
  def +:(element: Elem): Result[This]

  /**
    * Insert all the specified values at the tail of the list stored at key.
    * If key does not exist, it is created as empty list before performing
    * the push operation. When key holds a value that is not a list, an error
    * is returned.
    * *
    * It is possible to push multiple elements using a single command call
    * just specifying multiple arguments at the end of the command. Elements
    * are inserted one after the other to the tail of the list, from the
    * leftmost element to the rightmost element. So for instance the command
    * RPUSH mylist a b c will result into a list containing a as first element,
    * b as second element and c as third element.
    *
    * @param element to be apended
    * @return this collection to chain commands
    */
  def :+(element: Elem): Result[This]

  /**
    * Insert all the specified values at the head of the list stored at key.
    * If key does not exist, it is created as empty list before performing
    * the push operations. When key holds a value that is not a list, an
    * error is returned.
    *
    * It is possible to push multiple elements using a single command call
    * just specifying multiple arguments at the end of the command. Elements
    * are inserted one after the other to the head of the list, from the
    * leftmost element to the rightmost element. So for instance the command
    * LPUSH mylist a b c will result into a list containing c as first element,
    * b as second element and a as third element.
    *
    * @param elements element to be prepended
    * @return this collection to chain commands
    */
  def ++:(elements: Iterable[Elem]): Result[This]

  /**
    * Insert all the specified values at the tail of the list stored at key.
    * If key does not exist, it is created as empty list before performing
    * the push operation. When key holds a value that is not a list, an error
    * is returned.
    * *
    * It is possible to push multiple elements using a single command call
    * just specifying multiple arguments at the end of the command. Elements
    * are inserted one after the other to the tail of the list, from the
    * leftmost element to the rightmost element. So for instance the command
    * RPUSH mylist a b c will result into a list containing a as first element,
    * b as second element and c as third element.
    *
    * @param elements to be apended
    * @return this collection to chain commands
    */
  def :++(elements: Iterable[Elem]): Result[This]

  /**
    * Returns the element at index index in the list stored at key.
    * The index is zero-based, so 0 means the first element, 1 the
    * second element and so on. Negative indices can be used to designate
    * elements starting at the tail of the list. Here, -1 means
    * the last element, -2 means the penultimate and so forth. When
    * the value at key is not a list, an error is returned.
    *
    * Time complexity: O(N) where N is the number of elements to traverse
    * to get to the element at index. This makes asking for the first or
    * the last element of the list O(1).
    *
    * @param index position of the element
    * @return element at the index or exception
    *
    */
  def apply(index: Int): Result[Elem]

  /**
    * Returns the element at index index in the list stored at key.
    * The index is zero-based, so 0 means the first element, 1 the
    * second element and so on. Negative indices can be used to designate
    * elements starting at the tail of the list. Here, -1 means
    * the last element, -2 means the penultimate and so forth. When
    * the value at key is not a list, an error is returned.
    *
    * Time complexity: O(N) where N is the number of elements to traverse
    * to get to the element at index. This makes asking for the first or
    * the last element of the list O(1).
    *
    * @param index position of the element
    * @return Some( element ) at the index, None if no element exists,
    *         or exception when the value is not a list
    */
  def get(index: Int): Result[Option[Elem]]

  /**
    * @return first element of the collection or an exception
    */
  def head: Result[Elem] = apply(0)

  /**
    * @return first element of the collection or None
    */
  def headOption: Result[Option[Elem]] = get(0)

  /**
    * Removes and returns the first element of the list stored at key.
    *
    * Time complexity: O(1)
    *
    * @return head element if exists
    */
  def headPop: Result[Option[Elem]]

  /**
    * @return last element of the collection or an exception
    */
  def last: Result[Elem] = apply(-1)

  /**
    * @return last element of the collection or None
    */
  def lastOption: Result[Option[Elem]] = get(-1)

  /**
    * @return Helper to this.view.all returning all object in the list
    */
  def toList: Result[Seq[Elem]] = view.all

  /**
    * Inserts value in the list stored at key either before the
    * reference value pivot. When key does not exist, it is considered
    * an empty list and no operation is performed. An error is returned
    * when key exists but does not hold a list value.
    *
    * Time complexity: O(N) where N is the number of elements to traverse
    * before seeing the value pivot. This means that inserting somewhere
    * on the left end on the list (head) can be considered O(1) and
    * inserting somewhere on the right end (tail) is O(N).
    *
    * @param pivot   insert before this value
    * @param element elements to be inserted
    * @return new size of the collection or None if pivot not found
    */
  def insertBefore(pivot: Elem, element: Elem): Result[Option[Long]]

  /**
    * Sets the list element at index to value. For more information on the
    * index argument, see LINDEX. An error is returned for out of range indexes.
    *
    * @param position position to insert at
    * @param element  elements to be inserted
    * @return this collection to chain commands
    */
  def set(position: Int, element: Elem): Result[This]

  /**
    * Removes first N values equal to the given value from the list.
    *
    * Note: If the value is serialized object, it is not proofed that serialized
    * strings will match and the value will be actually deleted. This function
    * is intended to be used with primitive types only.
    *
    * Time complexity: O(N)
    *
    * @param element element to be removed from the list
    * @param count   first N occurrences
    * @return this collection to chain commands
    */
  def remove(element: Elem, count: Int = 1): Result[This]

  /**
    * Removes the element at the given position. If the index
    * is out of range, it throws an exception.
    *
    * Time complexity: O(N)
    *
    * @param position element index to be removed
    * @return this collection to chain commands
    */
  def removeAt(position: Int): Result[This]

  /**
    * @return read-only operations over the collection, does not modify data
    */
  def view: RedisListView

  /**
    * @return write-only operations over the collection, modify data
    */
  def modify: RedisListModification

  trait RedisListView {

    /**
      * Helper method of slice. For more details see that method.
      *
      * @param n takes initial N elements
      * @return first N elements of the collection if exist
      */
    def take(n: Int): Result[Seq[Elem]] = slice(0, n - 1)

    /**
      * Helper method of slice. For more details see that method.
      *
      * @param n ignore initial N elements
      * @return rest of the collection ignoring initial N elements
      */
    def drop(n: Int): Result[Seq[Elem]] = slice(n, -1)

    /**
      * Helper method of slice. For more details see that method.
      *
      * @return whole collection
      */
    def all: Result[Seq[Elem]] = slice(0, -1)

    /**
      * Returns the specified elements of the list stored at key.
      * The offsets start and stop are zero-based indexes, with 0 being
      * the first element of the list (the head of the list), 1 being the
      * next element and so on. These offsets can also be negative numbers
      * indicating offsets starting at the end of the list.
      * For example, -1 is the last element of the list, -2 the penultimate,
      * and so on.
      *
      * Time complexity: O(S+N) where S is the distance of start offset from HEAD
      * for small lists, from nearest end (HEAD or TAIL) for large lists;
      * and N is the number of elements in the specified range.
      *
      * Out-of-range indexes
      * Out of range indexes will not produce an error. If start is larger than
      * the end of the list, an empty list is returned. If stop is larger than
      * the actual end of the list, Redis will treat it like the last element
      * of the list.
      *
      * @param from initial index
      * @param end  index of the last element included
      * @return collection at the specified range
      */
    def slice(from: Int, end: Int): Result[Seq[Elem]]
  }

  trait RedisListModification {

    /**
      * @return RedisList object with Redis API
      */
    def collection: This

    /**
      * Helper method of slice. For more details see that method.
      *
      * @param n takes initial N elements
      * @return this object to chain commands
      */
    def take(n: Int): Result[RedisListModification] = slice(0, n - 1)

    /**
      * Helper method of slice. For more details see that method.
      *
      * @param n ignore initial N elements
      * @return this object to chain commands
      */
    def drop(n: Int): Result[RedisListModification] = slice(n, -1)

    /**
      * Helper method of slice. Wiping the whole collection
      *
      * @return this object to chain commands
      */
    def clear(): Result[RedisListModification]

    /**
      * Trim an existing list so that it will contain only the specified range
      * of elements specified. Both start and stop are zero-based indexes, where
      * 0 is the first element of the list (the head), 1 the next element and so on.
      *
      * For example: LTRIM foobar 0 2 will modify the list stored at foobar so
      * that only the first three elements of the list will remain.
      *
      * start and end can also be negative numbers indicating offsets from the
      * end of the list, where -1 is the last element of the list, -2 the
      * penultimate element and so on.
      *
      * Out of range indexes will not produce an error: if start is larger than
      * the end of the list, or start > end, the result will be an empty list
      * (which causes key to be removed). If end is larger than the end of the
      * list, Redis will treat it like the last element of the list.
      *
      * @param from initial index
      * @param end  index of the last element included
      * @return this object to chain commands
      */
    def slice(from: Int, end: Int): Result[RedisListModification]
  }

}
