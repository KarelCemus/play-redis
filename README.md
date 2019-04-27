<div align="center">

  # Redis Cache module for Play framework

  **This version supports Play framework 2.7.x with JDK 8 and both Scala 2.11 and Scala 2.12.**<br/>
  **For previous versions see older releases.**

  [![Travis CI: Status](https://travis-ci.org/KarelCemus/play-redis.svg?branch=master)](https://travis-ci.org/KarelCemus/play-redis)
  [![Coverage Status](https://coveralls.io/repos/github/KarelCemus/play-redis/badge.svg?branch=master)](https://coveralls.io/github/KarelCemus/play-redis?branch=master)
  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.karelcemus/play-redis_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.karelcemus/play-redis_2.12)

</div>


## About the Project

[Play framework 2](http://playframework.com/) is delivered with
[SyncCacheApi and AsyncCacheApi](https://playframework.com/documentation/2.7.x/ScalaCache).
This module provides **implementation of a cache over Redis** server, i.e., key/value storage.

Besides the compatibility with all Play's cache APIs,
it introduces more evolved API providing lots of handful
operations. Besides the basic methods such as `get`, `set`
and `remove`, it provides more convenient methods such as
`expire`, `exists`, `invalidate` and much more.

The implementation builds on the top of Akka actor system,
it is **completely non-blocking and asynchronous** under
the hood, though it also provides blocking APIs to ease
the use. Furthermore, the library supports several configuration
providers to let you easily use `play-redis` on localhost, Heroku,
as well as on your premise.


## Features

- [synchronous and asynchronous APIs](#provided-apis)
- [implements standard APIs defined by Play's `cacheApi` project](#provided-apis)
- support of [named caches](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md#named-caches)
- [works with Guice](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/40-migration.md#runtime-time-dependency-injection) as well as [compile-time DI](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/40-migration.md#compile-time-dependency-injection)
- [getOrElse and getOrFuture operations](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/30-how-to-use.md#use-of-cacheapi) easing the use
- [wildcards in remove operation](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/30-how-to-use.md#use-of-cacheapi)
- support of collections: [sets](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/30-how-to-use.md#use-of-sets), [lists](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/30-how-to-use.md#use-of-lists), and [maps](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/30-how-to-use.md#use-of-maps)
- [increment and decrement operations](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/30-how-to-use.md#use-of-cacheapi)
- [eager and lazy invocation policies](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md#eager-and-lazy-invocation) waiting or not waiting for the result
- several [recovery policies](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md#recovery-policy) and possibility of further customization
- support of [several configuration sources](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md#running-in-different-environments)
    - static in the configuration file
    - from the connection string optionally in the environmental variable
    - custom implementation of the configuration provider
- support of [standalone, cluster,](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md#standalone-vs-cluster)
  and [sentinel modes](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md#sentinel)
- build on the top of Akka actors and serializers, [agnostic to the serialization mechanism](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md#limitation-of-data-serialization)
    - for simplicity, it uses deprecated Java serialization by default
    - it is recommended to use [Kryo library](https://github.com/romix/akka-kryo-serialization) or any other mechanism


## Provided APIs

This library delivers a single module with following implementations of the API. While the core
of the framework is **fully non-blocking**, most of the provided facades are **only blocking wrappers**.

<center>

|    | Trait                                | Language | Blocking     | Features |
| -- | ------------------------------------ | :------: | :----------: | :------: |
| 1. | `play.api.cache.redis.CacheApi`      | Scala    | *blocking*   | advanced |
| 1. | `play.api.cache.redis.CacheAsyncApi` | Scala    | non-blocking | advanced |
| 1. | `play.cache.redis.AsyncCacheApi`     | Java     | non-blocking | advanced |
| 1. | `play.api.cache.SyncCacheApi`        | Scala    | *blocking*   | basic    |
| 1. | `play.api.cache.AsyncCacheApi`       | Scala    | non-blocking | basic    |
| 1. | `play.cache.SyncCacheApi`            | Java     | *blocking*   | basic    |
| 1. | `play.cache.AsyncCacheApi`           | Java     | non-blocking | basic    |

</center>

First, the `CacheAsyncApi` provides extended API to work with Redis and enables **non-blocking**
connection providing results through `scala.concurrent.Future`.
Second, the `CacheApi` is a thin **blocking** wrapper around the asynchronous implementation.
Third, there are other implementations supporting contemporary versions of the `CacheApi`s
bundled within Play framework. Finally, `play-redis` also supports Java version of the API,
though it is primarily **designed for and more efficient with Scala**.


## Documentation and Getting Started

**[The full documentation](https://github.com/KarelCemus/play-redis/)**
is in the `doc` directory. **The documentation for a particular version**
is under [the particular tag in the Git history](https://github.com/KarelCemus/play-redis/releases)
or you can use shortcuts in the table below.

To use this module:

1. [Add this library into your project](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/10-integration.md) and expose APIs
1. See the [configuration options](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/20-configuration.md)
1. [Browse examples of use](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/30-how-to-use.md)

If you come from older version, you might check the [Migration Guide](https://github.com/KarelCemus/play-redis/blob/2.4.0/doc/40-migration.md)


## Samples

To ease the initial learning, there are
[several sample projects](https://github.com/KarelCemus/play-redis-samples)
intended to demonstrate the most common configurations. Feel free
to study, copy or fork them to better understand the `play-redis` use.


1. [**Getting Started**](https://github.com/KarelCemus/play-redis-samples/tree/master/hello_world) is a very basic example showing the
minimal configuration required to use the redis cache

1. [**Named Caches**](https://github.com/KarelCemus/play-redis-samples/tree/master/named_caches) is the advanced example with custom recovery policy and multiple named caches.

1. [**EhCache and Redis**](https://github.com/KarelCemus/play-redis-samples/tree/master/redis_and_ehcache) shows a combination of both caching provides used at once.
While the EhCache is bound to unqualified APIs, the Redis cache uses named APIs.


## How to add the module into the project

To your SBT `build.sbt` add the following lines:

```scala
// enable Play cache API (based on your Play version)
libraryDependencies += play.sbt.PlayImport.cacheApi
// include play-redis library
libraryDependencies += "com.github.karelcemus" %% "play-redis" % "2.4.0"
```


## Compatibility matrix

| play framework  | play-redis     | documentation    |
|-----------------|---------------:|-----------------:|
| 2.7.x           | <!-- Play 2.7 -->2.4.0<!-- / -->          | [see here](https://github.com/KarelCemus/play-redis/blob/2.4.0/README.md) |
| 2.6.x           | <!-- Play 2.6 -->2.3.0<!-- / -->          | [see here](https://github.com/KarelCemus/play-redis/blob/2.3.0/README.md) ([Migration Guide](https://github.com/KarelCemus/play-redis/blob/2.3.0/doc/40-migration.md)) |
| 2.5.x           | <!-- Play 2.5 -->1.4.2<!-- / -->          | [see here](https://github.com/KarelCemus/play-redis/blob/1.4.2/README.md) |
| 2.4.x           | <!-- Play 2.4 -->1.0.0<!-- / -->          | [see here](https://github.com/KarelCemus/play-redis/blob/1.0.0/README.md) |
| 2.3.x           | <!-- Play 2.3 -->0.2.1<!-- / -->          | [see here](https://github.com/KarelCemus/play-redis/blob/0.2.1/README.md) |


## Contribution

If you encounter any issue, have a feature request, or just
like this library, please feel free to report it or contact me.


## Changelog

For the list of changes and migration guide please see
[the Changelog](https://github.com/KarelCemus/play-redis/blob/2.4.0/CHANGELOG.md).


## Caveat

The library **does not enable** the redis module by default. It is to avoid conflict with Play's default EhCache
and let the user define when use Redis. This allows you to use EhCache in your *dev* environment and
Redis in *production*. You can also combine the modules using named caches.
