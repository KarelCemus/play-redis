<h1 align="center">Redis Cache module for Play framework</h1>

<p align="center"><strong>Note: This version supports Play framework 2.4.x with JDK 8.<br/>For previous versions see older releases.</strong></p>

<p align="center">
  <a href='https://travis-ci.org/KarelCemus/play-redis'><img src='https://travis-ci.org/KarelCemus/play-redis.svg?branch=master'></a>
</p>

By default, [Play framework 2](http://playframework.com/) is delivered with EHCache module implementing
[CacheApi](https://www.playframework.com/documentation/2.4.x/api/scala/index.html#play.api.cache.CacheApi).
Unfortunately, this module suffers from a very inconvenient issue: as the ClassLoaders are evolved on
application recompilation **during a development**, the class versions does not match thus the **cache is wiped on
every compilation**. That might result in *repeated logging in*, *loosing a session* or *computing an extensive value*.
All these things very slow down development.

The goal of this module is to enable the **redis-server** key/value cache storage to be smoothly used within the
Play framework 2. Furthermore, besides the backward compatibility with the [CacheApi](https://www.playframework.com/documentation/2.4.x/api/scala/index.html#play.api.cache.CacheApi),
it introduces more evolved API called [play.api.cache.redis.CacheApi](https://github.com/KarelCemus/play-redis/blob/master/src/main/scala/play/api/cache/redis/InternalCacheApi.scala).
As the cache implementation uses Akka actor system, it is **completely non-blocking and asynchronous**. Furthermore,
besides the basic methods such as `get`, `set` and `remove` it provides more convenient methods such as `expire`,
`exists` and `invalidate`.

This library delivers a single module with following implementations of the API:

 1. play.api.cache.redis.CacheApi (blocking Scala implementation)
 2. play.api.cache.redis.CacheAsyncApi (non-blocking Scala implementation)
 3. play.api.cache.CacheApi (Play's blocking API for Scala)
 4. play.cache.CacheApi (Play's blocking API for Java)

First, the CacheApi is extended play.api.cache.CacheApi and it implements the connection in the **blocking** manner.
Second, the CacheAsyncApi enables **non-blocking** connection providing results through `scala.concurrent.Future`.
Third, the synchronous implementation also implements standard CacheApi bundled within Play framework.


## How to add the module into the project

This module builds over [Brando connector](https://github.com/chrisdinn/brando) and is intended **only for Scala version**
of the Play framework.

To your SBT `build.sbt` add the following lines:

```scala
// redis-server cache
libraryDependencies += "com.github.karelcemus" %% "play-redis" % "1.0.0"

// repository with the Brando connector
resolvers += "Brando Repository" at "http://chrisdinn.github.io/releases/"
```

Now we **must enable our redis** cache module and **disable default Play's EhCache** module. Into `application.conf` and following
two lines:

```
# disable default Play framework cache plugin
play.modules.disabled += "play.api.cache.EhCacheModule"

# enable redis cache module
play.modules.enabled += "play.api.cache.redis.RedisCacheModule"
```

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
}
```

## Checking operation result

Independently on the used API, all operations throw an exception when fail. Consequently,
successful invocations do not throw an exception. The only difference is in checking for errors.
Synchronous APIs really throw an exception, while asynchronous API returns a `Future`
wrapping both the success and the exception, i.e., use `onFailure` or `onComplete` to
check for errors.


<div align="center">
  <strong>
   Please keep in mind that this implementation is blocking!
  </strong>
</div>


## Configuration

There is already default configuration but it can be overwritten in your `conf/application.conf` file.

| Key                                 | Type     | Default                         | Description                         |
|-------------------------------------|---------:|--------------------------------:|-------------------------------------|
| play.cache.redis.host               | String   | `localhost`                     | redis-server address                |
| play.cache.redis.port               | Int      | `6379`                          | redis-server port                   |
| play.cache.redis.database           | Int      | `1`                             | redis-server database, 1-15         |
| play.cache.redis.timeout            | Duration | `1s`                            | connection timeout                  |
| play.cache.redis.dispatcher         | String   | `akka.actor.default-dispatcher` | Akka actor                          |
| play.cache.redis.configuration      | String   | `static`                        | Defines which configuration source enable. Accepted values are `static`, `env`, `custom` |
| play.cache.redis.password           | String   | `null`                          | When authentication is required, this is the password. Value is optional. |
| play.cache.redis.connection-string-variable | String   | `REDIS_URL`             | Name of the environment variable with the connection string. This is used in combination with the `env` configuration. This allows customization of the variable name in PaaS environment. Value is optional. |


### Connection settings on different platforms

In various environments there are various sources of the connection string defining how to connect to Redis instance.
For example, at localhost we are interested in direct definition of host and port in the `application.conf` file.
However, this approach does not fit all environments. For example, Heroku supplies `REDIS_URL` environment variable
defining the connection string. To resolve this diversity, the library expects an implementation of the `Configuration`
trait available through DI. By default, it enables `static` configuration source, i.e., it reads the settings from the
static configuration file. Another supplied configuration reader is `env`, which reads the environment variable such as
`REDIS_URL` but the name is configurable. To disable built-in providers you are free to set `custom` and supply your
own implementation of the `Configuration` trait.

### Running on Heroku

To enable redis cache on Heroku we have to do the following steps:

 1. add library into application dependencies
 2. enable `RedisCacheModule`
 3. disable `EhCacheModule`
 4. set `play.cache.redis.configuration: env`
 5. done, we can run it and use any of 3 provided interfaces

### Custom configuration source

However, there are scenarios when we need to customize the configuration to better fit our needs. Usually,
we might encounter this when we have a specific development flow or use a specific PaaS. To enable redis cache
implementation with customized configuration we have to do the following steps:

 1. add library into application dependencies
 2. enable `RedisCacheModule`
 3. disable `EhCacheModule`
 4. set `play.cache.redis.configuration: custom`
 5. Implement `play.api.cache.redis.Configuration` trait
 6. Register the implementation into DI provider. This is specific for each provider. If you are using Guice, which is
 Play's default DI provider, then look [here](https://playframework.com/documentation/2.4.x/ScalaDependencyInjection#Advanced:-Extending-the-GuiceApplicationLoader).
 It gives you a hint how to register the implementation during application start.
 7. done, we can run it and use any of 3 provided interfaces

## Worth knowing to avoid surprises

The library **does not enable** the redis module by default. It is to avoid conflict with Play's default EhCache.
The Play discourages disabling modules within the library thus it leaves it up to developers to disable EhCache
and enable Redis manually. This also allows you to use EhCache in your *dev* environment and redis in *production*.
Nevertheless, this module **replaces** the EHCache and it is not intended to use both implementations along.
