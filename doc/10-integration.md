# Integration Guide

The library comes with the support of both compile-time and runtime-time dependency injection.
Although the use of runtime-time injection is preferred, both options are equal and fully implemented.

## Add Library into the Project

```sbt
// enable Play cache API (based on your Play version)
libraryDependencies += play.sbt.PlayImport.cacheApi
// include play-redis library
libraryDependencies += "com.github.karelcemus" %% "play-redis" % "5.2.0"
```


## Runtime-time Dependency Injection

You **must enable the redis cache module** in `application.conf`:

```hocon
# enable redis cache module
play.modules.enabled += "play.api.cache.redis.RedisCacheModule"
```

It will bind all required components and make them available through runtime DI according to the configuration.


## Compile-time Dependency Injection

To use compile-time DI, mix `play.api.cache.redis.RedisCacheComponents`
into your `BuiltInComponentsFromContext` subclass. It exposes `cacheApi` method
accepting a redis instance (or just cache name, if configured) and returns an instance
of `RedisCaches` encapsulating all available APIs for this particular cache. Then you
can access and expose them yourself. Next, it provides a few methods to override and
provide customized configuration. For more details, see directly `RedisCacheComponents` source.

```scala
   // 'play' is the name of the named cache
   // (play is default name of the default cache)
   //
   // the 'play' literal is implicitly converted into
   // the instance but has to be configured in 'application.conf'
   val playCache: RedisCaches = cacheApi( "play" )

   // expose `play.api.cache.redis.CacheAsyncApi`
   val asynchronousRedis = playCache.async

```
