<h1 align="center">Redis Cache module for Play framework</h1>

<p align="center"><strong>Note: This version supports Play framework 2.6.x with JDK 8 and both Scala 2.11 and Scala 2.12.<br/>For previous versions see older releases.</strong></p>

<p align="center">
  <a href='https://travis-ci.org/KarelCemus/play-redis'><img src='https://travis-ci.org/KarelCemus/play-redis.svg?branch=master'></a>
</p>

By default, [Play framework 2](http://playframework.com/) is delivered with EHCache module implementing
[SyncCacheApi and AsyncCacheApi](https://playframework.com/documentation/2.6.x/ScalaCache).
This module enables use of the **redis-server**, i.e., key/value cache, within the
Play framework 2. Besides the backward compatibility with all Play's cache APIs,
it introduces more evolved API providing various handful operations. Besides the basic methods such as
`get`, `set` and `remove`, it provides more convenient methods such as `expire`, `exists`, `invalidate` and much more.
As the cache implementation uses Akka actor system, it is **completely non-blocking and asynchronous**.
Furthermore, we deliver the library with several configuration providers to let you easily use
play-redis on Heroku as well as on your premise.

## Provided APIs

This library delivers a single module with following implementations of the API. While the core
of the framework is fully non-blocking, most of provided facades are *blocking wrappers*.

 1. `play.api.cache.redis.CacheApi` (*blocking* Scala implementation)
 2. `play.api.cache.redis.CacheAsyncApi` (non-blocking Scala implementation)
 3. ~~`play.api.cache.CacheApi`~~ (Play's *blocking* API for Scala) *(deprecated since Play 2.6.x)*
 4. `play.api.cache.SyncCacheApi` (Play's *blocking* API for Scala) *(new in Play 2.6.x)*
 4. `play.api.cache.AsyncCacheApi` (Play's *non-blocking* API for Scala) *(new in Play 2.6.x)*
 4. ~~`play.cache.CacheApi`~~ (Play's *blocking* API for Java) *(deprecated since Play 2.6.x)*
 4. `play.cache.SyncCacheApi` (Play's *blocking* API for Java) *(new in Play 2.6.x)*
 4. `play.cache.AsyncCacheApi` (Play's *non-blocking* API for Java) *(new in Play 2.6.x)*

First, the `CacheAsyncApi` provides extended API to work with Redis and enables **non-blocking** 
connection providing results through `scala.concurrent.Future`.
Second, the `CacheApi` is a thin **blocking** wrapper around the asynchronous implementation.
Third, there are other implementations supporting both contemporary and deprecated `CacheApi`s 
bundled within Play framework. Finally, `play-redis` also supports Java version of the API.


## How to add the module into the project

**Since 1.5.x version** this module builds over [Rediscala connector](https://github.com/etaty/rediscala) and is intended **only for Scala version**
of the Play framework.

To your SBT `build.sbt` add the following lines:

```scala
// enable Play cache API (based on your Play version)
libraryDependencies += play.sbt.PlayImport.cacheApi
// include play-redis library
libraryDependencies += "com.github.karelcemus" %% "play-redis" % "1.4.1"
```

### Using with Guice

Now we **must enable our redis** cache module in `application.conf`:

```
# enable redis cache module
play.modules.enabled += "play.api.cache.redis.RedisCacheModule"
```

It will bind all required components and make them available through runtime DI.

### Using with compile-time DI

To use compile-time DI mix `play.api.cache.redis.RedisCacheComponents` 
into your `BuiltInComponentsFromContext` subclass. It exposes all kinds of supported API.
Next, it exposes a few methods to override and provide customized configuration.

## How to use this module

When you have the library added to your project, you can safely inject the `play.api.cache.redis.CacheApi` trait
for the synchronous cache. If you want the asynchronous implementation, then inject `play.api.cache.redis.CacheAsyncApi`.
There might be some limitations with data types but it should not be anything major. (Note: it uses Akka serialization.
Supported data types are primitives, objects serializable through the java serialization and collections.)
If you encounter any issue, **please feel free to report it**.

**Example:**

```scala
import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.cache.redis.CacheApi

class MyController @Inject() ( cache: CacheApi ) {

  cache.set( "key", "value" )
  // returns Option[ T ] where T stands for String in this example
  cache.get[ String ]( "key" )
  cache.remove( "key" )

  cache.set( "object", MyCaseClass() )
  // returns Option[ T ] where T stands for MyCaseClass
  cache.get[ MyCaseClass ]( "object" )

  // returns Unit
  cache.set( "key", 1.23 )

  // returns Option[ Double ]
  cache.get[ Double ]( "key" )
  // returns Option[ MyCaseClass ]
  cache.get[ MyCaseClass ]( "object" )
  
  // set multiple values at once
  cache.setAll( "key" -> 1.23, "key2" -> 5, "key3" -> 6 )
  // set only when all keys are unused
  cache.setAllIfNotExist( "key" -> 1.23, "key2" -> 5, "key3" -> 6 )
  // get multiple keys at once, returns a list of options
  cache.getAll[ Double ]( "key", "key2", "key3", "key6" )

  // returns T where T is Double. If the value is not in the cache
  // the computed result is saved
  cache.getOrElse( "key" )( 1.24 )

  // same as getOrElse but works for Futures. It returns Future[ T ]
  cache.getOrFuture( "key" )( Future( 1.24 ) )

  // returns Unit and removes a key/keys from the storage
  cache.remove( "key" )
  cache.remove( "key1", "key2" )
  cache.remove( "key1", "key2", "key3" )
  // remove all expects a sequence of keys, it performs same be behavior
  // as remove methods, they are just syntax sugar
  cache.removeAll( "key1", "key2", "key3" )

  // removes all keys in the redis database! Beware using it
  cache.invalidate()

  // refreshes expiration of the key if present
  cache.expire( "key", 1.second )

  // stores the value for infinite time if the key is not used
  // returns true when store performed successfully
  // returns false when some value was already defined
  cache.setIfNotExists( "key", 1.23 )
  // stores the value for limited time if the key is not used
  // this is not atomic operation, redis does not provide direct support
  cache.setIfNotExists( "key", 1.23, 5.seconds )

  // returns true if the key is in the storage, false otherwise
  cache.exists( "key" )

  // returns all keys matching given pattern. Beware, complexity is O(n),
  // where n is the size of the database. It executes KEYS command.
  cache.matching( "page/1/*" )

  // removes all keys matching given pattern. Beware, complexity is O(n),
  // where n is the size of the database. It internally uses method matching.
  // It executes KEYS and DEL commands in a transaction.
  cache.removeMatching( "page/1/*" )

  // importing `play.api.cache.redis._` enables us
  // using both `java.util.Date` and `org.joda.time.DateTime` as expiration
  // dates instead of duration. These implicits are useful when
  // we know the data regularly changes, e.g., at midnight, at 3 AM, etc.
  // We do not have compute the duration ourselves, the library
  // can do it for us
  import play.api.cache.redis._
  cache.set( "key", "value", DateTime.parse( "2015-12-01T00:00" ).asExpiration )

  // atomically increments stored value by one
  // initializes with 0 if not exists
  cache.increment( "integer" ) // returns 1
  cache.increment( "integer" ) // returns 2
  cache.increment( "integer", 5 ) // returns 7

  // atomically decrements stored value by one
  // initializes with 0 if not exists
  cache.decrement( "integer" ) // returns -1
  cache.decrement( "integer" ) // returns -2
  cache.decrement( "integer", 5 ) // returns -7
}
```

**Example of Lists:**

```scala

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.cache.redis.CacheApi

class MyController @Inject() ( cache: CacheApi ) {

  // enables List operations
  // Scala wrapper over the list at this key
  cache.list[ String ]( "my-list" )

  // get the whole list
  cache.list[ String ]( "my-list" ).toList

  // prepend values, beware, values are prepended in the reversed order!
  // result List( "EFG", "ABC" )
  cache.list[ String ]( "my-list" ).prepend( "ABC" ).prepend( "EFG" )
  "EFG" +: "ABC" +: cache.list[ String ]( "my-list" )
  List( "ABC", "EFG" ) ++: cache.list[ String ]( "my-list" )

  // append values to the list
  // result List( "ABC", "EFG" )
  cache.list[ String ]( "my-list" ).append( "ABC" ).append( "EFG" )
  cache.list[ String ]( "my-list" ) :+ "ABC" :+ "EFG"
  cache.list[ String ]( "my-list" ) :++ List( "ABC", "EFG" )

  // getting a value
  cache.list[ String ]( "my-list" ).apply( index = 1 ) // get or an exception
  cache.list[ String ]( "my-list" ).get( index = 1 ) // Some or None
  cache.list[ String ]( "my-list" ).head // get or an exception
  cache.list[ String ]( "my-list" ).headOption // Some or None
  cache.list[ String ]( "my-list" ).headPop // Some or None and REMOVE the head
  cache.list[ String ]( "my-list" ).last // get or an exception
  cache.list[ String ]( "my-list" ).lastOption // Some or None

  // size of the list
  cache.list[ String ]( "my-list" ).size

  // overwrite the value at index
  cache.list[ String ]( "my-list" ).set( position = 1, element = "HIJ" )

  // remove the value
  cache.list[ String ]( "my-list" ).remove( "ABC", count = 2 ) // remove by value
  cache.list[ String ]( "my-list" ).removeAt( position = 1 ) // remove by index

  // returns an API to reading but not modifying the list
  cache.list[ String ]( "my-list" ).view

  // returns an API to modify the underlying list
  cache.list[ String ]( "my-list" ).modify
}
```

**Example of Sets:**

```scala

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.cache.redis.CacheApi

class MyController @Inject() ( cache: CacheApi ) {

  // enables Set operations
  // Scala wrapper over the set at this key
  cache.set[ String ]( "my-set" )

  // get the whole set
  cache.set[ String ]( "my-set" ).toSet

  // add values into the set
  cache.set[ String ]( "my-set" ).add( "ABC", "EDF" )

  // test existence in the set
  cache.set[ String ]( "my-set" ).contains( "ABC" )

  // size of the set
  cache.set[ String ]( "my-set" ).size
  cache.set[ String ]( "my-set" ).isEmpty
  cache.set[ String ]( "my-set" ).nonEmpty

  // remove the value
  cache.set[ String ]( "my-set" ).remove( "ABC" )
}
```

**Example of Maps:**

```scala

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.cache.redis.CacheApi

class MyController @Inject() ( cache: CacheApi ) {

  // enables Set operations
  // Scala wrapper over the map at this key
  cache.map[ Int ]( "my-map" )

  // get the whole map
  cache.map[ Int ]( "my-map" ).toMap
  cache.map[ Int ]( "my-map" ).keySet
  cache.map[ Int ]( "my-map" ).values

  // test existence in the map
  cache.map[ Int ]( "my-map" ).contains( "ABC" )

  // get single value
  cache.map[ Int ]( "my-map" ).get( "ABC" )

  // add values into the map
  cache.map[ Int ]( "my-map" ).add( "ABC", 5 )

  // size of the map
  cache.map[ Int ]( "my-map" ).size
  cache.map[ Int ]( "my-map" ).isEmpty
  cache.map[ Int ]( "my-map" ).nonEmpty

  // remove the value
  cache.map[ Int ]( "my-map" ).remove( "ABC" )
}
```

## Checking operation result

Regardless of current API, all operations throw an exception when fail. Consequently,
successful invocations do not throw an exception. The only difference is in checking for errors.
While synchronous APIs really throw an exception, asynchronous API returns a `Future`
wrapping both the success and the exception, i.e., use `onFailure` or `onComplete` to
check for errors.

## Configuration

There is already default configuration but it can be overwritten in your `conf/application.conf` file.

| Key                                 | Type     | Default                         | Description                         |
|-------------------------------------|---------:|--------------------------------:|-------------------------------------|
| play.cache.redis.host               | String   | `localhost`                     | redis-server address                |
| play.cache.redis.port               | Int      | `6379`                          | redis-server port                   |
| play.cache.redis.database           | Int      | `1`                             | redis-server database, 1-15         |
| play.cache.redis.timeout            | Duration | `1s`                            | conversion timeout applied by `SyncAPI` to convert `Future[T]` to `T`|
| play.cache.redis.dispatcher         | String   | `akka.actor.default-dispatcher` | Akka actor                          |
| play.cache.redis.configuration      | String   | `static`                        | Defines which configuration source enable. Accepted values are `static`, `env`, `custom` |
| play.cache.redis.password           | String   | `null`                          | When authentication is required, this is the password. Value is optional. |
| play.cache.redis.connection-string-variable | String   | `REDIS_URL`             | Name of the environment variable with the connection string. This is used in combination with the `env` configuration. This allows customization of the variable name in PaaS environment. Value is optional. |
| play.cache.redis.recovery           | String   | `log-and-default`               | Defines behavior when command execution fails. Accepted values are `log-and-fail` to log the error and rethrow the exception, `log-and-default` to log the failure and return default value neutral to the operation, `log-condensed-and-default` `log-condensed-and-fail` produce shorter but less informative error logs, and `custom` indicates the user binds his own implementation of `RecoveryPolicy`.        |

### Timeout

The `play-redis` is designed fully asynchronously and there is no timeout applied 
by this library itself. However, there are other timeouts you might be interested in.
First, when you use `SyncAPI` instead of `AsyncAPI`, the `Future[T]` has to converted
into `T`. It uses [`Await.result`](https://www.scala-lang.org/api/current/scala/concurrent/Await$.html#result%5BT%5D(awaitable:scala.concurrent.Awaitable%5BT%5D,atMost:scala.concurrent.duration.Duration):T) 
from standard Scala library, which requires a timeout definition. This is the `play.cache.redis.timeout`.
It is the invocation of the whole request into cache including the communication to Redis, data serialization,
and invocation `orElse` parts. If you don't want any timeout and your application logic cannot never timeout,
just set it to something really high.

The other timeouts you might be interested in are related to the communication to Redis, e.g., connection timeout
and receive timeout. These are provided directly by the underlying connector and `play-redis` doesn't affect them. 
For more details, see 
the [`Scredis` configuration](https://github.com/scredis/scredis/blob/master/src/main/resources/reference.conf#L27).

### Recovery policy

The intention of cache is usually to optimize the application behavior, not to provide any business logic.
In this case it makes sense the cache could be removed without any visible change except for possible
performance loss. In consequence, we think that **failed cache requests should not break the application flow**,
they should be logged and ignored. However, not always this is desired behavior. To resolve this ambiguity,
we provide `RecoveryPolicy` trait implementing the behavior to be executed when the cache request  fails.
By default, we provide two implementations. They both log the failure at first and while one produces
the exception and let the application to deal with it, the other returns some neutral value, which
should result in behavior like there is no cache. However, besides these, it is possible, e.g., to also
rerun a failed command. For more information see `RecoveryPolicy` trait.

### Connection settings on different platforms

In various environments there are various sources of the connection string defining how to connect to Redis instance.
For example, at localhost we are interested in direct definition of host and port in the `application.conf` file.
However, this approach does not fit all environments. For example, Heroku supplies `REDISCLOUD_URL` environment variable
defining the connection string. To resolve this diversity, the library expects an implementation of the `Configuration`
trait available through DI. By default, it enables `static` configuration source, i.e., it reads the settings from the
static configuration file. Another supplied configuration reader is `env`, which reads the environment variable such as
`REDIS_URL` but the variable name is configurable. To easy use on Heroku, we also provide `heroku` configuration
profile expecting `REDISCLOUD_URL` variable. To disable built-in providers you are free to set `custom` and supply your
own implementation of the `Configuration` trait.

### Running on Heroku

To enable redis cache on Heroku we have to do the following steps:

 1. add library into application dependencies
 2. enable `RedisCacheModule`
 4. set either `play.cache.redis.configuration: "heroku"` or  `play.cache.redis.configuration: "heroku-cloud"` depending whether your Heroku addon provides `REDIS_URL` or `REDISCLOUD_URL` environment variable.
 5. done, we can run it and use any of 3 provided interfaces

### Custom configuration source

However, there are scenarios when we need to customize the configuration to better fit our needs. Usually,
we might encounter this when we have a specific development flow or use a specific PaaS. To enable redis cache
implementation with customized configuration we have to do the following steps:

 1. add library into application dependencies
 2. enable `RedisCacheModule`
 4. set `play.cache.redis.configuration: custom`
 5. Implement `play.api.cache.redis.Configuration` trait
 6. Register the implementation into DI provider. This is specific for each provider. If you are using Guice, which is
 Play's default DI provider, then look [here](https://playframework.com/documentation/2.5.x/ScalaDependencyInjection#Advanced:-Extending-the-GuiceApplicationLoader).
 It gives you a hint how to register the implementation during application start.
 7. done, we can run it and use any of 3 provided interfaces

## Caveat

The library **does not enable** the redis module by default. It is to avoid conflict with Play's default EhCache
and let the user define when use Redis. This allows you to use EhCache in your *dev* environment and 
Redis in *production*. Nevertheless, this module **replaces** the EHCache and it is not intended to use 
both implementations along.

## Compatibility matrix

<center>

| play framework  | play-redis     |
|-----------------|---------------:|
| 2.6.x           | 1.5.0          |
| 2.5.x           | 1.4.1          |
| 2.4.x           | 1.0.0          |
| 2.3.x           | 0.2.1          |


</center>

## Changelog

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
