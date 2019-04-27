package play.cache.redis;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Redis Hashes are simply hash maps with strings as keys. It is possible to add
 * elements to a Redis Hashes by adding new elements into the collection.
 *
 * <strong>This simplified wrapper implements only unordered Maps.</strong>
 *
 * @tparam Elem Data type of the inserted element
 * @since 2.5.0
 */
public interface AsyncRedisMap<Elem> {

    /**
     * Insert the value at the given key into the map
     *
     * @param field key
     * @param value inserted value
     * @return the map for the chaining calls
     */
    CompletionStage<AsyncRedisMap<Elem>> add(String field, Elem value);

    /**
     * Returns the value at the given key into the map
     *
     * @param field key
     * @return some value if exists in the map, None otherwise
     */
    CompletionStage<Optional<Elem>> get(String field);

    /**
     * <p>Tests if the field is contained in the map. Returns true if exists, otherwise returns false</p>
     *
     * @param field tested field
     * @return true if exists in the map, otherwise false
     * @note <strong>Time complexity:</strong> O(1)
     */
    CompletionStage<Boolean> contains(String field);

    /**
     * <p>Removes the specified members from the sorted map stored at key. Non existing members are ignored.
     * An error is returned when key exists and does not hold a sorted map.</p>
     *
     * @param field fields to be removed
     * @return the map for chaining calls
     * @note <strong>Time complexity:</strong> O(N) where N is the number of members to be removed.
     */
    CompletionStage<AsyncRedisMap<Elem>> remove(String... field);

    /**
     * Increment a value at the given key in the map by 1
     *
     * @param field key
     * @return value after incrementation
     */
    CompletionStage<Long> increment(String field);

    /**
     * Increment a value at the given key in the map
     *
     * @param field       key
     * @param incrementBy increment by this
     * @return value after incrementation
     */
    CompletionStage<Long> increment(String field, Long incrementBy);

    /**
     * <p>Returns all elements in the map</p>
     *
     * @return all elements in the map
     * @note <strong>Time complexity:</strong> O(N) where N is the map cardinality.
     */
    CompletionStage<Map<String, Elem>> toMap();

    /**
     * Returns all keys defined in the map
     *
     * @return all used keys
     */
    CompletionStage<Set<String>> keySet();

    /**
     * Returns all values stored in the map
     *
     * @return all stored values
     */
    CompletionStage<Set<Elem>> values();
}
