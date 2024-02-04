package play.cache.redis;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Redis Lists are simply lists of strings, sorted by insertion order.
 * It is possible to add elements to a Redis List pushing new elements
 * on the head (on the left) or on the tail (on the right) of the list.
 *
 * @tparam Elem Data type of the inserted element
 * @since 2.5.0
 */
public interface AsyncRedisList<Elem> {

    /**
     * Insert all the specified values at the head of the list stored at key.
     * If key does not exist, it is created as empty list before performing
     * the push operations. When key holds a value that is not a list, an
     * error is returned.
     * <p>
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
    CompletionStage<AsyncRedisList<Elem>> prepend(Elem element);

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
    CompletionStage<AsyncRedisList<Elem>> append(Elem element);

    /**
     * Returns the element at index index in the list stored at key.
     * The index is zero-based, so 0 means the first element, 1 the
     * second element and so on. Negative indices can be used to designate
     * elements starting at the tail of the list. Here, -1 means
     * the last element, -2 means the penultimate and so forth. When
     * the value at key is not a list, an error is returned.
     * <p>
     * Time complexity: O(N) where N is the number of elements to traverse
     * to get to the element at index. This makes asking for the first or
     * the last element of the list O(1).
     *
     * @param index position of the element
     * @return element at the index or exception
     */
    CompletionStage<Elem> apply(long index);

    /**
     * Returns the element at index index in the list stored at key.
     * The index is zero-based, so 0 means the first element, 1 the
     * second element and so on. Negative indices can be used to designate
     * elements starting at the tail of the list. Here, -1 means
     * the last element, -2 means the penultimate and so forth. When
     * the value at key is not a list, an error is returned.
     * <p>
     * Time complexity: O(N) where N is the number of elements to traverse
     * to get to the element at index. This makes asking for the first or
     * the last element of the list O(1).
     *
     * @param index position of the element
     * @return Some(element) at the index, None if no element exists,
     * or exception when the value is not a list
     */
    CompletionStage<Optional<Elem>> get(long index);

    /**
     * @return first element of the collection or an exception
     */
    default CompletionStage<Elem> head() {
        return apply(0);
    }

    /**
     * @return first element of the collection or None
     */
    default CompletionStage<Optional<Elem>> headOption() {
        return get(0);
    }

    /**
     * Removes and returns the first element of the list stored at key.
     * <p>
     * Time complexity: O(1)
     *
     * @return head element if exists
     */
    CompletionStage<Optional<Elem>> headPop();

    /**
     * @return last element of the collection or an exception
     */
    default CompletionStage<Elem> last() {
        return apply(-1);
    }

    /**
     * @return last element of the collection or None
     */
    default CompletionStage<Optional<Elem>> lastOption() {
        return get(-1);
    }

    /**
     * @return Helper to this.view.all returning all object in the list
     */
    default CompletionStage<List<Elem>> toList() {
        return view().all();
    }

    /**
     * Inserts value in the list stored at key either before the
     * reference value pivot. When key does not exist, it is considered
     * an empty list and no operation is performed. An error is returned
     * when key exists but does not hold a list value.
     * <p>
     * Time complexity: O(N) where N is the number of elements to traverse
     * before seeing the value pivot. This means that inserting somewhere
     * on the left end on the list (head) can be considered O(1) and
     * inserting somewhere on the right end (tail) is O(N).
     *
     * @param pivot   insert before this value
     * @param element elements to be inserted
     * @return new size of the collection or None if pivot not found
     */
    CompletionStage<Optional<Long>> insertBefore(Elem pivot, Elem element);

    /**
     * Sets the list element at index to value. For more information on the
     * index argument, see LINDEX. An error is returned for out of range indexes.
     *
     * @param position position to insert at
     * @param element  elements to be inserted
     * @return this collection to chain commands
     */
    CompletionStage<AsyncRedisList<Elem>> set(long position, Elem element);

    /**
     * Removes first value equal to the given value from the list.
     * <p>
     * Note: If the value is serialized object, it is not proofed that serialized
     * strings will match and the value will be actually deleted. This function
     * is intended to be used with primitive types only.
     * <p>
     * Time complexity: O(N)
     *
     * @param element element to be removed from the list
     * @return this collection to chain commands
     */
    CompletionStage<AsyncRedisList<Elem>> remove(Elem element);

    /**
     * Removes first N values equal to the given value from the list.
     * <p>
     * Note: If the value is serialized object, it is not proofed that serialized
     * strings will match and the value will be actually deleted. This function
     * is intended to be used with primitive types only.
     * <p>
     * Time complexity: O(N)
     *
     * @param element element to be removed from the list
     * @param count   first N occurrences
     * @return this collection to chain commands
     */
    CompletionStage<AsyncRedisList<Elem>> remove(Elem element, long count);

    /**
     * Removes the element at the given position. If the index
     * is out of range, it throws an exception.
     * <p>
     * Time complexity: O(N)
     *
     * @param position element index to be removed
     * @return this collection to chain commands
     */
    CompletionStage<AsyncRedisList<Elem>> removeAt(long position);

    /**
     * @return read-only operations over the collection, does not modify data
     */
    AsyncRedisListView<Elem> view();

    /**
     * @return write-only operations over the collection, modify data
     */
    AsyncRedisListModification<Elem> modify();

    interface AsyncRedisListView<Elem> {

        /**
         * Helper method of slice. For more details see that method.
         *
         * @param n takes initial N elements
         * @return first N elements of the collection if exist
         */
        default CompletionStage<List<Elem>> take(long n) {
            return slice(0, n - 1);
        }

        /**
         * Helper method of slice. For more details see that method.
         *
         * @param n ignore initial N elements
         * @return rest of the collection ignoring initial N elements
         */
        default CompletionStage<List<Elem>> drop(long n) {
            return slice(n, -1);
        }

        /**
         * Helper method of slice. For more details see that method.
         *
         * @return whole collection
         */
        default CompletionStage<List<Elem>> all() {
            return slice(0, -1);
        }

        /**
         * Returns the specified elements of the list stored at key.
         * The offsets start and stop are zero-based indexes, with 0 being
         * the first element of the list (the head of the list), 1 being the
         * next element and so on. These offsets can also be negative numbers
         * indicating offsets starting at the end of the list.
         * For example, -1 is the last element of the list, -2 the penultimate,
         * and so on.
         * <p>
         * Time complexity: O(S+N) where S is the distance of start offset from HEAD
         * for small lists, from nearest end (HEAD or TAIL) for large lists;
         * and N is the number of elements in the specified range.
         * <p>
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
        CompletionStage<List<Elem>> slice(long from, long end);
    }

    interface AsyncRedisListModification<Elem> {

        /**
         * @return RedisList object with Redis API
         */
        AsyncRedisList<Elem> collection();

        /**
         * Helper method of slice. For more details see that method.
         *
         * @param n takes initial N elements
         * @return this object to chain commands
         */
        default CompletionStage<AsyncRedisListModification<Elem>> take(long n) {
            return slice(0, n - 1);
        }

        /**
         * Helper method of slice. For more details see that method.
         *
         * @param n ignore initial N elements
         * @return this object to chain commands
         */
        default CompletionStage<AsyncRedisListModification<Elem>> drop(long n) {
            return slice(n, -1);
        }

        /**
         * Helper method of slice. Wiping the whole collection
         *
         * @return this object to chain commands
         */
        CompletionStage<AsyncRedisListModification<Elem>> clear();

        /**
         * Trim an existing list so that it will contain only the specified range
         * of elements specified. Both start and stop are zero-based indexes, where
         * 0 is the first element of the list (the head), 1 the next element and so on.
         * <p>
         * For example: LTRIM foobar 0 2 will modify the list stored at foobar so
         * that only the first three elements of the list will remain.
         * <p>
         * start and end can also be negative numbers indicating offsets from the
         * end of the list, where -1 is the last element of the list, -2 the
         * penultimate element and so on.
         * <p>
         * Out of range indexes will not produce an error: if start is larger than
         * the end of the list, or start > end, the result will be an empty list
         * (which causes key to be removed). If end is larger than the end of the
         * list, Redis will treat it like the last element of the list.
         *
         * @param from initial index
         * @param end  index of the last element included
         * @return this object to chain commands
         */
        CompletionStage<AsyncRedisListModification<Elem>> slice(long from, long end);
    }
}
        