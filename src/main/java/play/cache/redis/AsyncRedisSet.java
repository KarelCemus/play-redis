package play.cache.redis;

import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Redis Sets are simply unsorted sets of objects. It is possible to add
 * elements to a Redis Set by adding new elements into the collection.
 *
 * <strong>This simplified wrapper implements only unordered Sets.</strong>
 *
 * @since 2.5.0
 */
public interface AsyncRedisSet<Elem> {

    /**
     * <p>Add the specified members to the set stored at key. Specified members that are already a member of this
     * set are ignored. If key does not exist, a new set is created before adding the specified members.</p>
     *
     * @param element elements to be added
     * @return the set for chaining calls
     * @note An error is returned when the value stored at key is not a set.
     * @note <strong>Time complexity:</strong>  O(1) for each element added, so O(N) to add N elements when the
     * command is called with multiple arguments.
     */
    @SuppressWarnings("unchecked")
    CompletionStage<AsyncRedisSet<Elem>> add(Elem... element);

    /**
     * <p>Tests if the element is contained in the set. Returns true if exists, otherwise returns false</p>
     *
     * @param element tested element
     * @return true if exists in the set, otherwise false
     * @note <strong>Time complexity:</strong> O(1)
     */
    CompletionStage<Boolean> contains(Elem element);

    /**
     * <p>Removes the specified members from the sorted set stored at key. Non existing members are ignored.
     * An error is returned when key exists and does not hold a sorted set.</p>
     *
     * @param element elements to be removed
     * @return the set for chaining calls
     * @note <strong>Time complexity:</strong> O(N) where N is the number of members to be removed.
     */
    @SuppressWarnings("unchecked")
    CompletionStage<AsyncRedisSet<Elem>> remove(Elem... element);

    /**
     * <p>Returns all elements in the set</p>
     *
     * @return all elements in the set
     * @note <strong>Time complexity:</strong> O(N) where N is the set cardinality.
     */
    CompletionStage<Set<Elem>> toSet();
}
