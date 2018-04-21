# Migration Guide

This document extends the [CHANGELOG](https://github.com/KarelCemus/play-redis/blob/master/CHANGELOG.md) summing up the most of the changes. There are discussed only the complex changes to provide tips on migration.

Play-redis is a matter of regular development since we try to upkeep it in the best
possible way. To fulfill requirements of our users, we make our solution to be up-to-date
and increase its capabilities with every increment. This time, it required from us to broke the
backward compatibility in a few places. This Migration Guide allows you to adjust your usage
in the smoothest possible way. We believe that removing technical debt as soon as possible
is the only way of keeping this project in a good shape.

## Migration from 2.0.x to 2.1.x

The most changes are under the hood, however, there are couple
changes in public API, which needs your code to be updated.

### Revamped Invocation Policy

The invocation policy in `2.0.x` was used as an implicit parameter. Since
`2.1.x` it is a static configurable property inside the instance configuration.
See the [updated documentation for more details](https://github.com/KarelCemus/play-redis/blob/2.1.0/doc/20-configuration.md#eager-and-lazy-invocation).

### Named caches uses @NamedCache instead of @Named

Up to `2.0.x`, play-redis bound named caches with `@Named` annotation. Since
`2.1.0`, this binding was deprecated any replaced by `@NamedCache` to be consistent
with Play framework implementation. All deprecations will be removed in `2.2.0`.

### Renamed `timeout` to `sync-timeout`

Since `2.1.0`, there is a new `redis-timeout` property. To avoid
ambiguity, the original `timeout` property was renamed to `sync-redis`.
The `timeout` property was deprecated any will be removed in `2.2.0`.

See the updated [documentation for more details](https://github.com/KarelCemus/play-redis/blob/2.1.0/doc/20-configuration.md#eager-and-lazy-invocation).


## Migration from 1.6.x to 2.0.x

The major goal of the 2.0.x version was to support named caches and clean up the code to ease maintenance. In the consequence, there was major redesign of the configuration, several properties were removed or replaced and some other were introduced instead. However, for simple cases and probably most setups, the configuration can be left intact.

### Configuration changes

- default database was changed from 1 to 0 (redis default) to remove the inconsistency between play-redis and the redis itself
- the cache instance defined directly under the `play.cache.redis` is a default instance and is named with a default name
- introduced `instances` property to support named caches. See [documentation](https://github.com/KarelCemus/play-redis/wiki/Configuration#named-caches) for more details.
- `configuration` property was redesigned and renamed to `source`. Valid values are now `standalone`, `cluster`, `connection-string`, and `custom`. See [documentation](https://github.com/KarelCemus/play-redis/wiki/Configuration#standalone-vs-cluster) for more details.
- `connection-string-variable` was replaced by the `connection-string` property defining the connection string itself. The value is now passed directly into the property through, e.g., `${REDIS_URL}`, which HOCON correctly resolves. This applies when combined with `source: connection-string`.
- `wait` property renamed to `timeout`
- `source`, `timeout`, `dispatcher`, and `recovery` define defaults and may be locally overridden within each named cache configuration.
- introduced `bind-default` property (default is true) indicating whether to bind the default instance to unqualified APIs.
- introduced `default-cache` property (default is `play`) defining the name of the default instance

### API changes

Removed implementation of deprecated `play.cache.CacheApi` and `play.api.cache.CacheApi`.

`RecoveryPolicy` was moved from `play.api.cache.redis.impl` to `play.api.cache.redis`.

Joda time implicit helpers for expiration computation in the `ExpirationImplicits` was deprecated as Play 2.6 deprecated joda-time library. There were introduced implicits for `java.time.LocalDateTime` instead.

#### Runtime DI (Guice)

Major redesign of `RedisCacheModule`, however, no changes in use are expected. Internal components are no longer registered into DI container.

#### Compile-time DI

Major redesign of `RedisCacheComponents`. See the [trait](https://github.com/KarelCemus/play-redis/blob/master/src/main/scala/play/api/cache/redis/RedisCacheComponents.scala#L14) for details. To create
new API, call `cacheApi( instance )`, where the instance is either a String with the instance name or `RedisInstance` object with a custom configuration. This returns a `RedisCaches` object encapsulating all available APIs. In case of using multiple different APIs, it is suggested to **reuse this object** to prevent the duplicate creation of instances of the same cache connector. To provide a custom instance configuration, either override `redisInstanceResolver` mapping names to the objects or pass the `RedisInstance` object directly to the `cacheApi` call. For custom recovery policy override `recoveryPolicyResolver`.

### Implementation changes

There are many changes under the hood to simplify the code, improve readability and ease the maintenance. The configuration loaders are fully rewritten as consequence of the configuration redesign. Some files we moved to other packages to better fit their purpose. However, there are basically **no changes to the cache implementation itself**. All the changes are related to the supporting classes and object, there was major refactoring. For full details, please consult the Git history.
