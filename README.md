# Redis Cache plugin for Play framework

**Note: This version supports Play framework 2.3.x.**

By default, [Play framework 2](http://playframework.com/) is delivered with EHCache plugin implementing
[CacheAPI](https://github.com/playframework/playframework/blob/2.3.8/framework/src/play-cache/src/main/scala/play/api/cache/Cache.scala).
Unfortunately, this plugin suffers from a very inconvenient issue: as the ClassLoaders are evolved on
application recompilation **during a development**, the class versions does not match thus the **cache is wiped on
every compilation**. That might result in *repeated logging in*, *loosing a session* or *computing an extensive value*.
All these things very slow down development.

The goal of this plugin is to enable the **redis-server** key/value cache storage to be smoothly used within the
Play framework 2. Furthermore, besides the backward compatibility with the [CacheAPI](https://github.com/playframework/playframework/blob/2.3.8/framework/src/play-cache/src/main/scala/play/api/cache/Cache.scala),
it introduces more evolved API called [CacheAPI20](https://github.com/KarelCemus/play-redis/blob/master/src/main/scala/play/cache/api/CacheAPI20.scala).
As the cache implementation uses Akka actor system, it is **completely non-blocking and asynchronous**. Furthermore,
besides the basic methods such as `get`, `set` and `remove` it provides more convenient methods such as `setIfNotExists`,
`getOrElse` and `invalidate`.

This library delivers three plugins:

 1. play.cache.redis.RedisCachePlugin
 2. play.cache.redis.RedisCachePlugin20
 3. play.cache.redis.RedisCacheAdapterPlugin

The first plugin is very basic asynchronous connector to the redis-server. It implements only basic operations such as
`get`, `set`, `remove`, `exists` and `invalidate` for strings. The second plugin brings the `CacheAPI20` with more
convenient methods plus adds support of non-string values - primitives as well as custom object. The last plugin
is an adapter to the standard **blocking** CacheAPI introduced by Play framework 2. It also supports various
data types.

<div align="center">
  <strong>
   It is HIGHLY recommended to use the CacheAPI20, if possible, as it is designed to be the primary interface to the redis cache.
  </strong>
</div>



## How to add the plugin into the project

This plugin builds over [Brando connector](https://github.com/chrisdinn/brando) and is intended **only for Scala version**
of the Play framework.

To your SBT `build.sbt` add the following lines:

```scala
libraryDependencies ++= Seq(
  // enable work with cache, introduces CacheAPI
  play.PlayImport.cache,
  // redis-server cache
  "com.github.karelcemus" %% "play-redis" % "0.2"
)
```


## How to use the Play framework Cache singleton with redis-server

When you have the library added to your project, you can safely use the `play.api.Cache` object. Its implementation
recognizes this plugin and uses redis instead. There might be some limitations with data types but it should not
be anything major. If so, please feel free to report the bug.

**Example:**

```scala
import play.api.Cache

Cache.set( "key", "value" )
Cache.get( "key" ) // returns Option[ Any ] where Any stands for String
Cache.remove( "key" )

Cache.set( "object", MyCaseClass() )
Cache.get( "object" ) // returns Option[ Any ] where Any stands for MyCaseClass
```


<div align="center">
  <strong>
   Please keep in mind that this implementation is blocking!
  </strong>
</div>



## How to use the CacheAPI20

Preferred way to use this redis-server plugin is through the [CacheAPI20](https://github.com/KarelCemus/play-redis/blob/master/src/main/scala/play/cache/api/CacheAPI20.scala).
It is **asynchronous** thus all operations return a **Future** object. Furthermore, `get` operation takes a generic
class type and instead of `Option[ Any ]` it returns `Option[ T ]`. Supported data types are primitives,
objects serializable through the java serialization and collections. (Note: it uses Akka serialization).

To preserve the ease of use, there is new version of the `Cache` singleton. There is tiny difference in the package name.
While the Play framework's version is `play.api.Cache`, the new version is `play.cache.AsyncCache`. This singleton implements
`CacheAPI20` and delegates requests to the `RedisCachePlugin20`.

**Example:**

```scala
import play.cache.{AsyncCache=>Cache} // alias for convenience

// returns Future[ Try[ String ] ] where the value string
// should be Success( "OK" ) or Failure( ex )
Cache.set( "key", 1.23 )

// returns Future[ Option[ Double ] ]
Cache.get[ Double ]( "key" )
// returns Future[ Option[ MyCaseClass ] ]
Cache.get[ MyCaseClass ]( "object" )

// returns Future[ Try[ String ] ] where the value string
// should be Success( "OK" ) or Failure( ex )
Cache.setIfNotExists( "key", 1.24 )

// returns Future[ T ], either the value from the cache
// or the computed value from the orElse the computation
// result is saved
Cache.getOrElse[ Double ]( "key" )( 10 * 1.23 )

// returns Future[ Try[ String ] ] where the value
// string should be Success( "OK" ) or Failure( ex )
// OK is returned even when the value was not in the
// cache thus it could not be removed
Cache.remove( "key" )

// invalidates all keys in the redis server
Cache.invalidate()
```

## Configuration

There is already default configuration but it can be overwritten in your `conf/application.conf` file.

| Key                           | Type   | Default                       | Description                         |
|-------------------------------|-------:|------------------------------:|-------------------------------------|
| play.cache.redis.host               | String | localhost                     | redis-server address                |
| play.cache.redis.port               | Int    | 6379                          | redis-server port                   |
| play.cache.redis.database           | Int    | 3                             | redis-server database, 1-15         |
| play.cache.redis.timeout            | Int    | 1000                          | connection timeout in milliseconds  |
| play.cache.redis.dispatcher         | String | akka.actor.default-dispatcher | Akka actor                          |
| play.cache.redis.expiration.default | Int    | 3600                          | default value expiration in seconds |

Furthermore, there are more advanced ways how to set value expiration.

1. Pass it as Int into `play.api.Cache#set`
1. Pass it as Some( Int ) into `play.cache.AsyncCache#set` or `play.cache.AsyncCache#setIfNotExists` or `play.cache.AsyncCache#getOrElse`
1. Do not set it directly (set 0 or None respectively) and leave it up to the plugin to compute it. The computation is
as follows:
  1. The configuration `play.cache.redis.expiration` is acquired as the root configuration
  2. The key is looked up in this configuration.
  3. If the key was not found, the tail since the last `.` is dropped. For example, if the key would be `my.first.key`,
  after the first attempt the `key` would be dropped and the `my.first` would remain.
  4. With the new shorten key the steps 2 and 3 are repeated while either the key is found or there are no more parts to drop.
  5. If the expiration was neither passed nor found, the default value is used.

For example, to default expiration for all keys starting with the prefix `my.first`, you have to set the key `play.cache.redis.expiration.my.first`.
Then the key `my.first.key` will expire in `my.first` in seconds.

<div align="center">
  <strong>
   Note: Dot notation is very preferred. Other delimiters such as <code>@#$</code> are not supported in keys as they cannot be keys in the configuration!
  </strong>
</div>



## Worth knowing to avoid surprises

The library configuration automatically **disables EHCache plugin**, it contains the following line in its `conf/reference.conf`.
You do not have to take care of it but it is good to be aware of it, because it **replaces** the EHCache by redis.

```
# disable default Play framework cache plugin
ehcacheplugin = disabled
```

The library enables all three plugins through `conf/play.plugins` with priorities 2000, 2010 and 2020. The order matters. The priority *has to be
higher than 1000*, which is the priority of the `play.api.libs.concurrent.AkkaPlugin`, which is connector's dependency.
