
## Changelog

### [:link: 2.5.0](https://github.com/KarelCemus/play-redis/tree/2.5.0)

Added `expiresIn(key: String): Option[Duration]` implementing PTTL 
command to get expiration of the given key. [#204](https://github.com/KarelCemus/play-redis/pull/204)

Introduced asynchronous implementation of advanced Java API for redis cache. The API
wraps the Scala version thus provides slightly worse performance and deals with
the lack of `classTag` in Play's API design. **This API implementation is experimental 
and may change in future.** Feedback will be welcome. [#206](https://github.com/KarelCemus/play-redis/issues/206)

Added `getFields(fields: String*)` and `getFields(fields: Iterable[String])` into `RedisMap` API
implementing HMGET command. [#207](https://github.com/KarelCemus/play-redis/issues/207)

### [:link: 2.4.0](https://github.com/KarelCemus/play-redis/tree/2.4.0)

Update to Play `2.7.0` [#202](https://github.com/KarelCemus/play-redis/pull/202)

Added `getAll[T: ClassTag](keys: Iterable[String]): Result[Seq[Option[T]]]` into `AbstractCacheApi` 
in order to also accept collections aside vararg. [#194](https://github.com/KarelCemus/play-redis/pull/194)

Fixed `getOrElse` method in Synchronous API with non-empty cache prefix. [#196](https://github.com/KarelCemus/play-redis/pull/196)

### [:link: 2.3.0](https://github.com/KarelCemus/play-redis/tree/2.3.0)

Support of Redis Sentinel [#181](https://github.com/KarelCemus/play-redis/pull/181)

Fixed operation `matching` with prefixed a instance.
Returned keys are automatically unprefixed. [#184](https://github.com/KarelCemus/play-redis/pull/184)

### [:link: 2.2.0](https://github.com/KarelCemus/play-redis/tree/2.2.0)

Support of plain arrays in JavaRedis [#176](https://github.com/KarelCemus/play-redis/pull/176).

Connection timeout introduced in [#147](https://github.com/KarelCemus/play-redis/issues/147) 
is now configurable and can be disabled [#174](https://github.com/KarelCemus/play-redis/pull/174).

Removed deprecations introduced in [2.0.0](https://github.com/KarelCemus/play-redis/tree/2.0.0)
and [2.1.0](https://github.com/KarelCemus/play-redis/tree/2.1.0).

### [:link: 2.1.2](https://github.com/KarelCemus/play-redis/tree/2.1.2)

JDK 10 compatibility. Replace deprecated com.sun.misc.BASE64* usages with jdk8 java.util.Base64 [#170](https://github.com/KarelCemus/play-redis/pull/170).

Customized release process to automatically update versions in README and documentation [#173](https://github.com/KarelCemus/play-redis/pull/173).

### [:link: 2.1.1](https://github.com/KarelCemus/play-redis/tree/2.1.1)

Scope of the Mockito dependency is set to Test, was Compile [#168](https://github.com/KarelCemus/play-redis/issues/168).

### [:link: 2.1.0](https://github.com/KarelCemus/play-redis/tree/2.1.0)

Published snapshots no longer depends on scoverage runtime [#143](https://github.com/KarelCemus/play-redis/issues/143).

`play.cache.AsyncCacheApi` is bound to `JavaRedis` instead of `DefaultAsyncCacheApi`
 to fixed value deserialization and support Java HTTP context [#140](https://github.com/KarelCemus/play-redis/issues/140).

Standalone client now fails eagerly when the connection to redis is not
established. This is to avoid long timeout while the rediscala is trying
to reconnect. [#147](https://github.com/KarelCemus/play-redis/issues/147)

Replaced `SETNX` and `SETEX` by `SET` operation with `EX` and `NX` parameters to
implement the set operation atomically. In consequence, slightly changed `RedisConnector` API.
[#156](https://github.com/KarelCemus/play-redis/issues/156)

Deprecated `timeout` property and replaced by `sync-timeout` with the identical
meaning and use. Will be removed by 2.2.0. [#154](https://github.com/KarelCemus/play-redis/issues/154)

Introduced **optional** `redis-timeout` property indicating timeout on redis queries.
This is the workaround as the rediscala has no timeout on the requests and they might
be never completed. However, to avoid performance issues, the timeout is **disabled by default**.
See [the configuration]() for more details. [#154](https://github.com/KarelCemus/play-redis/issues/154)

Rediscala bumped to 1.8.3 and subsequently Akka bumped to 2.5.6 [#150](https://github.com/KarelCemus/play-redis/issues/150).

Revamped tests, reduced their number but increased value and code coverage [#108](https://github.com/KarelCemus/play-redis/issues/108)

#### Removal of `@Named` and introduction of `@NamedCache`

Named caches now uses `@NamedCache` instead of `@Named` to be consistent with Play's EhCache and
enable interchangeability of the implementation.

**Migration**: Change the annotation where necessary.

**Backward compatibility**: In simple scenarios, there should
be no breaking changes. Use of `@Named` was deprecated and should emit warning in logs, but the
binding should still work. The warnings can be disabled through the logger configuration, though
the support will be fully removed in the `2.2.0`. The complex scenarios with the custom
`RedisInstance` or `RedisCaches` have to be migrated right away, there is no fallback binding.

**Note**: `RecoveryPolicy` still uses `@Named` as it neither is nor relates to any particular cache.

#### Revamped Invocation Policy

Dropped implicit `InvocationPolicy` from Scala version of the API,
replaced by the instance configuration through the config file.
Introduced configuration property `invocation`. It works also with JavaRedis.
For more details, see [the updated documentation](https://github.com/KarelCemus/play-redis/wiki/Configuration#invocation-policy)
 [#147](https://github.com/KarelCemus/play-redis/issues/147).


### [:link: 2.0.2](https://github.com/KarelCemus/play-redis/tree/2.0.2)

`play.cache.AsyncCacheApi` is bound to `JavaRedis` instead of `DefaultAsyncCacheApi`
 to fixed value deserialization and support Java HTTP context [#140](https://github.com/KarelCemus/play-redis/issues/140).

### [:link: 2.0.1](https://github.com/KarelCemus/play-redis/tree/2.0.1)

Fixed missing binding of `play.api.cache.AsyncCacheApi` [#135](https://github.com/KarelCemus/play-redis/issues/135).

### [:link: 2.0.0](https://github.com/KarelCemus/play-redis/tree/2.0.0)

Introduced support of Redis Cluster in [#84](https://github.com/KarelCemus/play-redis/issues/84).

Implemented increment in maps [#112](https://github.com/KarelCemus/play-redis/issues/112).

Support of named caches [#114](https://github.com/KarelCemus/play-redis/pull/114)

Introduced `prefix` configuration property to apply a namespace on all keys in
the cache instance [#118](https://github.com/KarelCemus/play-redis/pull/118).

Simplified `RedisCacheModule` and `RedisCacheComponents`. Internal components are no longer
published, removed `Binder` class, introduced `RedisInstanceProvider` instead. Introduced `RedisInstanceResolver`
and `RecoveryPolicyResolver` to provide custom implementations. Introduced `RedisCaches` handler
encapsulating all APIs to a single named cache. [#122](https://github.com/KarelCemus/play-redis/pull/122)

Created [Wiki](https://github.com/KarelCemus/play-redis/wiki) with more detailed and structured documentation.

Revamped configuration as a consequence of named cache integration
[#114](https://github.com/KarelCemus/play-redis/pull/114). Some
properties were removed, instance configuration is now more direct,
named caches are supported. See
[Migration Guide](https://github.com/KarelCemus/play-redis/wiki/Migration-Guide#migration-from-16x-to-20x)
for more details.

Introduced `InvocationPolicy` implementing `Eager` and `Lazy` invocation mechanism handling waiting
for the result of the `set` operation. `Lazy` policy (default) does wait for the result, `Eager` does
not wait and ignores it instead [#98](https://github.com/KarelCemus/play-redis/pull/98).

### [:link: 1.6.1](https://github.com/KarelCemus/play-redis/tree/1.6.1)

JavaRedis preserves `Http.Context` [#130](https://github.com/KarelCemus/play-redis/issues/130).

### [:link: 1.6.0](https://github.com/KarelCemus/play-redis/tree/1.6.0)

Introduced support of Redis Cluster in [#84](https://github.com/KarelCemus/play-redis/issues/84).

Implemented increment in maps [#112](https://github.com/KarelCemus/play-redis/issues/112).

### [:link: 1.5.1](https://github.com/KarelCemus/play-redis/tree/1.5.1)

Fixed [#102](https://github.com/KarelCemus/play-redis/issues/102), preserved original
exception if extends `RedisException` and fixed wrong parameters in error messages

### [:link: 1.5.0](https://github.com/KarelCemus/play-redis/tree/1.5.0)

**[Scredis](https://github.com/scredis/scredis) connector replaced
by [Rediscala](https://github.com/etaty/rediscala) implementation**
due to repository inactivity, no release management, missing support of cluster,
and unreleased support of Scala 2.12.

Due to changes in the connector, there are slightly relaxed constraints
of return values in both `CacheApi` and `CacheAsyncApi`. For example,
some operations instead of `List` return `Seq` and instead of `Set` also
return `Seq`. This change was introduced to avoid possibly unnecessary
collection conversion inside the `play-redis`.

Changed return type `Unit` in `AbstractCacheApi` to `akka.Done` with the same meaning but
but better signalizing the intention.

Cross-compiled for Scala 2.11 and Scala 2.12.

### [:link: 1.4.2](https://github.com/KarelCemus/play-redis/tree/1.4.2)

Fixed [#102](https://github.com/KarelCemus/play-redis/issues/102), preserved original
exception if extends `RedisException` and fixed wrong parameters in error messages

### [:link: 1.4.1](https://github.com/KarelCemus/play-redis/tree/1.4.1)

Fixed minor issues [#83](https://github.com/KarelCemus/play-redis/issues/83) and [#85](https://github.com/KarelCemus/play-redis/issues/85).

### [:link: 1.4.0](https://github.com/KarelCemus/play-redis/tree/1.4.0)

Implemented `RedisCacheComponents` to support [compile-time DI](#using-with-compile-time-di)

Implemented [MGET](https://redis.io/commands/mget), [MSET](https://redis.io/commands/mset) and [MSETNX](https://redis.io/commands/msetnx) redis commands.

### [:link: 1.3.1](https://github.com/KarelCemus/play-redis/tree/1.3.1)

Exposed `RedisConnector`, it is publicaly available for injection now.

### [:link: 1.3.0](https://github.com/KarelCemus/play-redis/tree/1.3.0) (Possibly breaking)

Major internal code refactoring, library has been modularized into several packages.
However, **public API remained unchanged**, although its implementation significantly
changed.

Implemented [Scala wrapper](src/main/scala/play/api/cache/redis/RedisList.scala) over List API to use [Redis Lists](https://redis.io/topics/data-types#lists).

Implemented [Scala wrapper](src/main/scala/play/api/cache/redis/RedisSet.scala) over Set API to use [Redis Sets](https://redis.io/topics/data-types#sets).

Implemented [Scala wrapper](src/main/scala/play/api/cache/redis/RedisMap.scala) over Map API to use [Redis Hashes](https://redis.io/topics/data-types#hashes).

Added `heroku` and `heroku-cloud` configuration profiles simplifying [running on Heroku](#running-on-heroku).

Introduced [`RecoveryPolicy`](#recovery-policy) defining behavior when execution fails. Default
policy is `log-and-default`. To re-enable previous *fail-on-error* behavior, set `log-and-fail`.
See the [`RecoveryPolicy`](#recovery-policy) for more details.

**[Brando](https://github.com/chrisdinn/brando) connector replaced by [scredis](https://github.com/scredis/scredis) implementation** due to Brando repository inactivity
and major issues ([#44](https://github.com/KarelCemus/play-redis/issues/44)). Scredis seems to be efficient, build over Akka and should not
contain any major issues as they are not reported.


### [:link: 1.2.0](https://github.com/KarelCemus/play-redis/tree/1.2.0)

Play-redis provides native serialization support to basic data types such as String, Int, etc.
However, for other objects including collections, it used to use default `JavaSerializer` serializer.
Since Akka 2.4.1, default `JavaSerializer` is [officially considered inefficient for production use](https://github.com/akka/akka/pull/18552).
Nevertheless, to keep things simple, play-redis **still uses this inefficient serializer NOT to enforce** any serialization
library to end users. Although, it recommends [kryo serializer](https://github.com/romix/akka-kryo-serialization) claiming
great performance and small output stream. Any serialization library can be smoothly connected through Akka
configuration, see the [official Akka documentation](http://doc.akka.io/docs/akka/current/scala/serialization.html).

This release is focused on library refactoring. While **public API remained unchanged**, there are several significant
changes to their implementations. Those are consequences of refactoring some functionality into self-standing
units. For example, there has been extracted `RedisConnector` implementing the [Redis protocol](http://redis.io/commands)
and `RedisCache` implementing cache API over that. Before, it was tangled together. As consequence, the library has
now layered architecture (facades -> cache implementation -> protocol implementation) with several public facades.

### [:link: 1.1.0](https://github.com/KarelCemus/play-redis/tree/1.1.0)

Update to Play 2.5, no significant changes

### [:link: 1.0.0](https://github.com/KarelCemus/play-redis/tree/1.0.0)

Redesigned the library from scratch to support Play 2.4.x API and use DI.
