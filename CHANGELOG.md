
## Changelog

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
