<h1 align="center">Redis Cache module for Play framework</h1>

<p align="center"><strong>Note: This version supports Play framework 2.4.x with JDK 8.<br/>For previous versions see older releases.</strong></p>

<p align="center">
  <a href='http://jenkins.karelcemus.cz/view/Play%20frameworks/job/play-redis/'><img src='http://jenkins.karelcemus.cz/buildStatus/icon?job=play-redis'></a>
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

This library delivers a single module with two implementations of the API:

 1. play.api.cache.redis.CacheApi
 2. play.api.cache.redis.CacheAsyncApi

First, the CacheApi is extended play.api.cache.CacheApi and it implements the connection in the **blocking** manner.
This implementation is active by default. Second, the CacheAsyncApi enables **non-blocking** connection providing
results through `scala.concurrent.Future`. This implementation must be enabled **manually**.


## How to add the module into the project

This module builds over [Brando connector](https://github.com/chrisdinn/brando) and is intended **only for Scala version**
of the Play framework.

To your SBT `build.sbt` add the following lines:

```scala
libraryDependencies ++= Seq(
  // redis-server cache
  "com.github.karelcemus" %% "play-redis" % "0.3-SNAPSHOT"
)
```

Now your cache is enabled. The Redis module is **enabled by default**, it also **enables synchronous** implementation
and **disables** default Play EHCache. All these can be changed through the configuration file.


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

  // returns Future[ Try[ String ] ] where the value string
  // should be Success( "OK" ) or Failure( ex )
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

  // invalidates all keys in the redis server! Beware using it
  cache.invalidate()

  // refreshes expiration of the key if present
  cache.expire( "key", 1.second )

  // returns true if the key is in the storage, false otherwise
  cache.exists( "key" )

  // when we import `play.api.cache.redis._` it enables us
  // using both `java.util.Date` and `org.joda.time.DateTime` as expiration
  // dates instead of duration. These implicits are useful when
  // we know the data regularly changes, e.g., at midnight, at 3 AM, etc.
  // We do not have compute the duration ourselves, the library
  // can do it for us
  import play.api.cache.redis._
  cache.set( "key", "value", DateTime.parse( "2015-12-01T00:00" ).asExpiration )
}
```


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
| play.cache.redis.enabled            | String[] | `[ "sync" ]`                    | Enabled implementations of the api. Possible values are `sync` and `async` |
| play.cache.redis.configuration      | String   | `local`                         | Defines which configuration source enable. Accepted values are `local`, `heroku`, `none` |
| play.cache.redis.password           | String   | `null`                          | When authentication is required, this is the password. Value is optional. |


### Connection settings on different platforms

In various environments there are various sources of the connection string defining how to connect to Redis instance.
For example, at localhost we are interested in direct definition of host and port in the `application.conf` file.
However, this approach does not fit all environments. For example, Heroku supplies `REDIS_URL` environment variable
defining the connection string. To resolve this diversity, the library expects an implementation of the `Configuration`
trait available through DI. By default, it enables `local` configuration source, i.e., it reads the settings from the
configuration file. Another supplied configuration reader is `heroku`, which reads the `REDIS_URL` variable. To disable
built-in providers you are free to set `none` and supply your own implementation of the `Configuration` trait.

## Worth knowing to avoid surprises

The library configuration automatically **disables EHCache module**, it contains the following line in its `conf/reference.conf`.
You do not have to take care of it but it is good to be aware of it, because it **replaces** the EHCache by redis.

```
# disable default Play framework cache plugin
play.modules.disabled += "play.api.cache.EhCacheModule"
```

The library enables only synchronous implementation. The asynchronous version must be enabled manually in the configuration file.
