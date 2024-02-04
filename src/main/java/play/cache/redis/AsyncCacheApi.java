package play.cache.redis;

import org.apache.pekko.Done;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

/**
 * <p>Cache API inspired by basic Play play.api.cache.CacheApi. It implements all its
 * operations and in addition it declares couple more useful operations handful
 * with cache storage.</p>
 *
 * @since 2.5.0
 */
public interface AsyncCacheApi extends play.cache.AsyncCacheApi {

    /**
     * Retrieve the values of all specified keys from the cache.
     *
     * @param classTag class to be parsed from the redis
     * @param keys     cache storage keys
     * @return stored record, Some if exists, otherwise None
     */
    default <T> CompletionStage<List<Optional<T>>> getAll(Class<T> classTag, String... keys) {
        return getAll(classTag, Arrays.asList(keys));
    }

    /**
     * Retrieve the values of all specified keys from the cache.
     *
     * @param classTag class to be parsed from the redis
     * @param keys     cache storage keys
     * @return stored record, Some if exists, otherwise None
     */
    <T> CompletionStage<List<Optional<T>>> getAll(Class<T> classTag, List<String> keys);

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * @param <T>   the type of the value
     * @param key   Item key.
     * @param block block returning value to set if key does not exist
     * @return a CompletionStage containing the value
     */
    <T> CompletionStage<T> getOrElse(String key, Callable<T> block);

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * @param <T>        the type of the value
     * @param key        Item key.
     * @param block      block returning value to set if key does not exist
     * @param expiration expiration period in seconds.
     * @return a CompletionStage containing the value
     */
    <T> CompletionStage<T> getOrElse(String key, Callable<T> block, int expiration);

    /**
     * Determines whether value exists in cache.
     *
     * @param key cache storage key
     * @return record existence, true if exists, otherwise false
     */
    CompletionStage<Boolean> exists(String key);

    /**
     * Retrieves all keys matching the given pattern. This method invokes KEYS command
     * <p>
     * '''Warning:''' complexity is O(n) where n are all keys in the database
     *
     * @param pattern valid KEYS pattern with wildcards
     * @return list of matching keys
     */
    CompletionStage<List<String>> matching(String pattern);

    /**
     * Set a value into the cache if the given key is not already used, otherwise do nothing.
     * Expiration time in seconds (0 second means eternity).
     * <p>
     * Note: When expiration is defined, it is not an atomic operation. Redis does not
     * provide a command for store-if-not-exists with duration. First, it sets the value
     * if exists. Then, if the operation succeeded, it sets its expiration date.
     * <p>
     * Note: When recovery policy used, it recovers with TRUE to indicate
     * **"the lock was acquired"** despite actually **not storing** anything.
     *
     * @param key   cache storage key
     * @param value value to store
     * @return true if value was set, false if was ignored because it existed before
     */
    CompletionStage<Boolean> setIfNotExists(String key, Object value);

    /**
     * Set a value into the cache if the given key is not already used, otherwise do nothing.
     * Expiration time in seconds (0 second means eternity).
     * <p>
     * Note: When expiration is defined, it is not an atomic operation. Redis does not
     * provide a command for store-if-not-exists with duration. First, it sets the value
     * if exists. Then, if the operation succeeded, it sets its expiration date.
     * <p>
     * Note: When recovery policy used, it recovers with TRUE to indicate
     * **"the lock was acquired"** despite actually **not storing** anything.
     *
     * @param key        cache storage key
     * @param value      value to store
     * @param expiration record duration in seconds
     * @return true if value was set, false if was ignored because it existed before
     */
    CompletionStage<Boolean> setIfNotExists(String key, Object value, int expiration);

    /**
     * Sets the given keys to their respective values for eternity. If any value is null,
     * the particular key is excluded from the operation and removed from cache instead.
     * The operation is atomic when there are no nulls. It replaces existing values.
     *
     * @param keyValues cache storage key-value pairs to store
     * @return promise
     */
    CompletionStage<Done> setAll(KeyValue... keyValues);

    /**
     * Sets the given keys to their respective values for eternity. It sets all values if none of them
     * exist, if at least a single of them exists, it does not set any value, thus it is either all or none.
     * If any value is null, the particular key is excluded from the operation and removed from cache instead.
     * The operation is atomic when there are no nulls.
     *
     * @param keyValues cache storage key-value pairs to store
     * @return true if value was set, false if any value already existed before
     */
    CompletionStage<Boolean> setAllIfNotExist(KeyValue... keyValues);

    /**
     * If key already exists and is a string, this command appends the value at the end of
     * the string. If key does not exist it is created and set as an empty string, so APPEND
     * will be similar to SET in this special case.
     * <p>
     * If it sets new value, it subsequently calls EXPIRE to set required expiration time
     *
     * @param key   cache storage key
     * @param value value to append
     * @return promise
     */
    CompletionStage<Done> append(String key, String value);

    /**
     * If key already exists and is a string, this command appends the value at the end of
     * the string. If key does not exist it is created and set as an empty string, so APPEND
     * will be similar to SET in this special case.
     * <p>
     * If it sets new value, it subsequently calls EXPIRE to set required expiration time
     *
     * @param key        cache storage key
     * @param value      value to append
     * @param expiration record duration, applies only when appends to nothing
     * @return promise
     */
    CompletionStage<Done> append(String key, String value, int expiration);

    /**
     * refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration
     *
     * @param key        cache storage key
     * @param expiration new expiration in seconds
     * @return promise
     */
    CompletionStage<Done> expire(String key, int expiration);

    /**
     * Returns the remaining time to live of a key that has an expire set,
     * useful, e.g., when we want to check remaining session duration
     *
     * @param key cache storage key
     * @return the remaining time to live of a key. Some finite duration when
     * the value exists and the expiration is set, Some infinite duration
     * when the value exists but never expires, and None when the key does
     * not exist.
     */
    CompletionStage<Optional<Long>> expiresIn(String key);

    /**
     * Remove all values from the cache
     *
     * @param key1 cache storage key
     * @param key2 cache storage key
     * @param keys cache storage keys
     * @return promise
     */
    CompletionStage<Done> remove(String key1, String key2, String... keys);

    /**
     * Removes all keys in arguments. The other remove methods are for syntax sugar
     *
     * @param keys cache storage keys
     * @return promise
     */
    CompletionStage<Done> removeAllKeys(String... keys);

    /**
     * <p>Removes all keys matching the given pattern. This command has no direct support
     * in Redis, it is combination of KEYS and DEL commands.</p>
     *
     * <ol>
     * <li>`KEYS pattern` command finds all keys matching the given pattern</li>
     * <li>`DEL keys` expires all of them</li>
     * </ol>
     *
     * <p>This is usable in scenarios when multiple keys contains same part of the key, such as
     * record identification, user identification, etc. For example, we may have keys such
     * as 'page/&#36;id/header', 'page/&#36;id/body', 'page/&#36;id/footer' and we want to remove them
     * all when the page is changed. We use the benefit of the '''naming convention''' we use and
     * execute `removeAllMatching( s"page/&#36;id&#47;*" )`, which invalidates everything related to
     * the given page. The benefit is we do not need to maintain the list of all keys to invalidate,
     * we invalidate them all at once.</p>
     *
     * <p>* '''Warning:''' complexity is O(n) where n are all keys in the database</p>
     *
     * @param pattern this must be valid KEYS pattern
     * @return nothing
     */
    CompletionStage<Done> removeMatching(String pattern);

    /**
     * Increments the stored string value representing 10-based signed integer
     * by given value. By default, the value is incremented by 1.
     *
     * @param key cache storage key
     * @return the value after the increment
     */
    default CompletionStage<Long> increment(String key) {
        return increment(key, 1L);
    }

    /**
     * Increments the stored string value representing 10-based signed integer
     * by given value.
     *
     * @param key cache storage key
     * @param by  size of increment
     * @return the value after the increment
     */
    CompletionStage<Long> increment(String key, Long by);

    /**
     * Decrements the stored string value representing 10-based signed integer
     * by given value. By default, the value is decremented by 1.
     *
     * @param key cache storage key
     * @return the value after the decrement
     */
    default CompletionStage<Long> decrement(String key) {
        return decrement(key, 1L);
    }

    /**
     * Decrements the stored string value representing 10-based signed integer
     * by given value.
     *
     * @param key cache storage key
     * @param by  size of decrement
     * @return the value after the decrement
     */
    CompletionStage<Long> decrement(String key, Long by);

    /**
     * Scala wrapper around Redis list-related commands. This simplifies use of the lists.
     *
     * @param key      the key storing the list
     * @param classTag class to be parsed from the redis
     * @return Scala wrapper
     */
    <T> AsyncRedisList<T> list(String key, Class<T> classTag);

    /**
     * Scala wrapper around Redis set-related commands. This simplifies use of the sets.
     *
     * @param key      the key storing the set
     * @param classTag class to be parsed from the redis
     * @return Scala wrapper
     */
    <T> AsyncRedisSet<T> set(String key, Class<T> classTag);

    /**
     * Scala wrapper around Redis hash-related commands. This simplifies use of the hashes, i.e., maps.
     *
     * @param key      the key storing the map
     * @param classTag class to be parsed from the redis
     * @return Scala wrapper
     */
    <T> AsyncRedisMap<T> map(String key, Class<T> classTag);
}
