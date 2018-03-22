<div align="center">

  # Redis Cache module for Play framework

  **This version supports Play framework 2.6.x with JDK 8 and both Scala 2.11 and Scala 2.12.**<br/>
  **For previous versions see older releases.**

  [![Travis CI: Status](https://travis-ci.org/KarelCemus/play-redis.svg?branch=master)](https://travis-ci.org/KarelCemus/play-redis)
  [![Coverage Status](https://coveralls.io/repos/github/KarelCemus/play-redis/badge.svg?branch=master)](https://coveralls.io/github/KarelCemus/play-redis?branch=master)

</div>


## About the Project

This module for Play framework 2 adds **support of Redis cache** server and provides
a **set of handful APIs**. 

## Features

- [synchronous and asynchronous APIs](https://github.com/KarelCemus/play-redis/wiki#provided-apis)
- [implements standard APIs defined by Play's `cacheApi` project](https://github.com/KarelCemus/play-redis/wiki#provided-apis)
- support of [named caches](https://github.com/KarelCemus/play-redis/wiki/Configuration#named-caches)
- [works with Guice](https://github.com/KarelCemus/play-redis/wiki/Integration-Guide#runtime-time-dependency-injection) as well as [compile-time DI](https://github.com/KarelCemus/play-redis/wiki/Integration-Guide#compile-time-dependency-injection)
- [getOrElse and getOrFuture operations](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#use-of-cacheapi) easing the use
- [wildcards in remove operation](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#use-of-cacheapi)
- support of collections: [sets](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#use-of-sets), [lists](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#use-of-lists), and [maps](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#use-of-maps)
- [increment and decrement operations](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#use-of-cacheapi)
- [eager and lazy invocation policies](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#eager-and-lazy-invocation) waiting or not waiting for the result
- several [recovery policies](https://github.com/KarelCemus/play-redis/wiki/Configuration#recovery-policy) and possibility of further customization
- support of [several configuration sources](https://github.com/KarelCemus/play-redis/wiki/Configuration#running-in-different-environments) 
    - static in the configuration file
    - from the connection string optionally in the environmental variable
    - custom implementation of the configuration provider
- support of [both standalone and cluster modes](https://github.com/KarelCemus/play-redis/wiki/Configuration#standalone-vs-cluster)
- build on the top of Akka actors and serializers, [agnostic to the serialization mechanism](https://github.com/KarelCemus/play-redis/wiki/How-to-Use#limitations)
    - for simplicity, it uses deprecated Java serialization by default
    - it is recommended to use [Kryo library](https://github.com/romix/akka-kryo-serialization) or any other mechanism  

## Documentation
 
**[The full documentation](https://github.com/KarelCemus/play-redis/wiki) for the version `2.x.x`** 
and newer is **see [on the wiki](https://github.com/KarelCemus/play-redis/wiki)**. For 
the documentation of older versions see README at corresponding 
[tag in git history](https://github.com/KarelCemus/play-redis/releases).

## Samples and Getting Started

To ease the initial learning, there are [several sample projects](https://github.com/KarelCemus/play-redis-samples) 
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
libraryDependencies += "com.github.karelcemus" %% "play-redis" % "2.0.2"
```

## Compatibility matrix

| play framework  | play-redis     |
|-----------------|---------------:|
| 2.6.x           | 2.0.2  ([Migration Guide](https://github.com/KarelCemus/play-redis/wiki/Migration-Guide))        |
| 2.5.x           | 1.4.2          |
| 2.4.x           | 1.0.0          |
| 2.3.x           | 0.2.1          |

## Contribution

If you encounter any issue, have a feature request, or just
like this library, please feel free to report it or contact me.

## Changelog

For the list of changes and migration guide please see
[the Changelog](https://github.com/KarelCemus/play-redis/blob/master/CHANGELOG.md).


## Caveat

The library **does not enable** the redis module by default. It is to avoid conflict with Play's default EhCache
and let the user define when use Redis. This allows you to use EhCache in your *dev* environment and
Redis in *production*. You can also combine the modules using named caches. 
