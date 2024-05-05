package play.api.cache.redis.connector;

import io.lettuce.core.*;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.protocol.CommandType;

import java.util.List;

public abstract class AbstractRedisCommandsMock implements RedisClusterAsyncCommands<String, String> {

    @Override
    final public RedisFuture<String> set(String key, String value, SetArgs setArgs) {
        return setWithArgs(key, value, setArgs);
    }

    public abstract RedisFuture<String> setWithArgs(String key, String value, SetArgs setArgs);

    @Override
    final public RedisFuture<Boolean> expire(String key, long seconds) {
        return expireSeconds(key, seconds);
    }

    public abstract RedisFuture<Boolean> expireSeconds(String key, long seconds);

    @Override
    final public RedisFuture<List<String>> lrange(String key, long start, long stop) {
        return lrangeLong(key, start, stop);
    }

    public abstract RedisFuture<List<String>> lrangeLong(String key, long start, long stop);

    @Override
    final public RedisFuture<Boolean> hset(String key, String field, String value) {
        return hsetSimple(key, field, value);
    }

    public abstract RedisFuture<Boolean> hsetSimple(String key, String field, String value);

    @Override
    final public RedisFuture<Long> geoadd(String key, Object... lngLatMember) {
        return null;
    }

    @SafeVarargs
    @Override
    final public RedisFuture<Long> geoadd(String key, GeoValue<String>... values) {
        return null;
    }

    @Override
    final public RedisFuture<Long> geoadd(String key, GeoAddArgs args, Object... lngLatMember) {
        return null;
    }

    @SafeVarargs
    @Override
    final public RedisFuture<Long> geoadd(String key, GeoAddArgs args, GeoValue<String>... values) {
        return null;
    }

    @Override
    final public RedisFuture<List<Object>> commandInfo(String... commands) {
        return null;
    }

    @Override
    final public RedisFuture<List<Object>> commandInfo(CommandType... commands) {
        return null;
    }

    @Override
    final public RedisFuture<Long> zadd(String key, Object... scoresAndValues) {
        return null;
    }

    @SafeVarargs
    @Override
    final public RedisFuture<Long> zadd(String key, ScoredValue<String>... scoredValues) {
        return zaddMock(key, scoredValues);
    }

    public abstract RedisFuture<Long> zaddMock(String key, ScoredValue<String>[] scoredValues);

    @Override
    final public RedisFuture<Long> zadd(String key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        return null;
    }

    @SafeVarargs
    @Override
    final public RedisFuture<Long> zadd(String key, ZAddArgs zAddArgs, ScoredValue<String>... scoredValues) {
        return null;
    }

    @Override
    final public RedisFuture<Long> zrem(String key, String... members) {
        return zremMock(key, members);
    }

    public abstract RedisFuture<Long> zremMock(String key, String[] members);

    @Override
    final public RedisFuture<List<String>> zrange(String key, long start, long stop) {
        return zrangeMock(key, start, stop);
    }

    public abstract RedisFuture<List<String>> zrangeMock(String key, long start, long stop);

    @Override
    final public RedisFuture<List<String>> zrevrange(String key, long start, long stop) {
        return zrevrangeMock(key, start, stop);
    }

    public abstract RedisFuture<List<String>> zrevrangeMock(String key, long start, long stop);
}
