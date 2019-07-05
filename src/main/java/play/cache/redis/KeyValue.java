package play.cache.redis;

/**
 * Java-equivalent of Scala-tuple
 */
public class KeyValue {

    public final String key;
    public final Object value;

    public KeyValue(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}
