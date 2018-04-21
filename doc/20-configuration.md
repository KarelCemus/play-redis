# Configuration

Default configuration and very detailed manual is available in [reference.conf](https://github.com/KarelCemus/play-redis/blob/master/src/main/resources/reference.conf). It can be overwritten in your `conf/application.conf` file.

There are several features supported in the configuration, they are discussed below. However, by default, there is no need for any further configuration. Default settings are set to the standalone instance running on `localhost:6379?db=0`, which is default for redis server. This instance is named `play` but is also exposed as a default implementation.

**The configuration root is `play.cache.redis`**

## Standalone vs. Cluster

This implementation supports both standalone and cluster instances. By default, the standalone mode is enabled.  It is configured like this:

```
  play.cache.redis {
    host:       localhost
    # redis server: port
    port:       6379
    # redis server: database number (optional)
    database:   0
    # authentication password (optional)
    password:   null
  }
```

To enable cluster mode instead, use `source` property. Valid values are `standalone` (default), `cluster`, `connection-string`, and `custom`. For more details, see below. Example of cluster settings:

```
play.cache.redis {
  # enable cluster mode
  source: cluster

  # nodes are defined as a sequence of objects:
  cluster:  [
    {
      # required string, defining a host the node is running on
      host:        localhost
      # required integer, defining a port the node is running on
      port:        6379
      # optional string, defines a password to use
      password:    null
    }
  ]
}
```


## Named caches

Play framework supports [named caches](https://www.playframework.com/documentation/2.6.x/ScalaCache#Accessing-different-caches) through a qualifier. For a simplicity, the default cache is also exposed without a qualifier to ease the access. This feature can be disabled by `bind-default` property, which defaults to true. The name of the default cache is defined in `default-cache` property, which defaults to `play` to keep consistency with Play framework.

The configuration of each instance is inherited from the `play.cache.redis` configuration. Inherited values may be locally overridden by the instance's own configuration (e.g., `play.cache.redis.instances.myNamedCache`)

Named caches are defined under the `play.cache.redis.instances` node, which automatically **disables and ignores** the default cache defined directly under the root `play.cache.redis`. The instances are defined as a map with name-definition pairs.

``` 
play.cache.redis {

  # source property; standalone is default
  source: standalone

  instances {
    play {
      # source property fallbacks to the value under play.cache.redis
      host:       localhost
      port:       6379
    }
    myNamedCache {
      source: cluster
      cluster: [
        { host: localhost, port: 6380 }
      ]
    }
  }

}
```


### Namespace prefix

Each cache can optionally define a namespace for keys. It means that each key is automatically prefixed. For example, having `prefix: "my-prefix"` and the key `my-key`, the cache operates with `my-prefix:my-key`. This behavior is especially useful to avoid collisions, when:

1. either we have multiple named caches working with the same redis database,
2. or there is another application working with the same redis database.

This property may be locally overridden for each named cache.


### Timeout

The `play-redis` is designed fully asynchronously and there is no timeout applied
by this library itself. However, there are other timeouts you might be interested in.
First, when you use `SyncAPI` instead of `AsyncAPI`, the **internal `Future[T]` has to be converted
into `T`**. It uses [`Await.result`](https://www.scala-lang.org/api/current/scala/concurrent/Await$.html#result%5BT%5D(awaitable:scala.concurrent.Awaitable%5BT%5D,atMost:scala.concurrent.duration.Duration):T)
from standard Scala library, which requires a timeout definition. This is the `play.cache.redis.timeout`.

This timeout applies on the invocation of the whole request including the communication to Redis, data serialization,
and invocation `orElse` parts. If you don't want any timeouts and your application logic has to never timeout,
just set it to something really high or use asynchronous API to be absolutely sure.

The other timeouts you might be interested in are related to the communication to Redis, e.g., connection timeout
and receive timeout. These are provided directly by the underlying connector and `play-redis` doesn't affect them.
For more details, see
the [`Redis` configuration](https://github.com/etaty/rediscala).

### Recovery policy

The intention of cache is usually to optimize the application behavior, not to provide any business logic.
In this case, it makes sense the cache could be removed without any visible change except for possible
performance loss. In consequence, **failed cache requests should not break the application flow**,
they should be logged and ignored. However, not always this is the desired behavior. To resolve this ambiguity,
the module provides `RecoveryPolicy` trait implementing the behavior to be executed when the cache request fails.
There are two major implementations. They both log the failure at first, and while one produces
the exception and let the application to deal with it, the other returns some neutral value, which
should result in behavior like there is no cache (default policy). However, besides these, it is possible, e.g., to also implement your own policy to, e.g., rerun a failed command. For more information see `RecoveryPolicy` trait.

Besides the default implementations, you are free to extend the `RecoveryPolicy` trait and provide your own implementation. For example:

```scala
class MyPolicy extends RecoveryPolicy {
  def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ) = default
}
```

Next, name it and update the configuration file: `play.cache.redis.recovery: custom`, where `custom` is the name. Any name is possible.

Then, if you use runtime DI (e.g. Guice), you have to bind the named `RecoveryPolicy` trait to your implementation. For example, create a module for it. Don't forget to register the module into enabled modules. And that's it.

```scala
import play.api.cache.redis.RecoveryPolicy
import play.api.inject._
import play.api.{Configuration, Environment}

class ApplicationModule extends Module {

  def bindings( environment: Environment, configuration: Configuration ) = Seq(
    // "custom" is the name in the configuration file
    bind[ RecoveryPolicy ].qualifiedWith( "custom" ).to( classOf[ MyPolicy ] )
  )
}
```

If you use compile-time DI, override `recoveryPolicyResolver` in `RedisCacheComponents` and return the instance when the policy name matches.

## Running in different environments

This module can run in various environments, from the localhost through the Heroku to your own premise. Each of these has a possibly different configuration. For this purpose, there is a `source` property accepting 4 values: `standalone` (default), `cluster`, `connection-string`, and `custom`.

The `standalone` and `cluster` options are already explained. The latter two simplify the use in environments, where the connection cannot be written into the configuration file up front.

First, the module supports instance definition through a connection string. This is especially useful when you are running the application, e.g., on Heroku or some other service.

```
play.cache.redis {
  # enable connection-string mode, i.e., a standalone
  # configured through a connection string
  source: connection-string

  # HOCOON automatically injects the environmental variable
  # and the module parses the string.
  connection-string: '${REDIS_URL}'
}
```

Second, when none of the already existing providers is sufficient, you can implement your own and let the `RedisInstanceResolver` to take care of it. When the module finds a `custom` source, it calls the resolver with the cache name and expects the configuration in return. So all you need to do is to implement your own resolver and register it with DI. Details may differ based on the type of DI you use.

### Example: Running on Heroku

To enable redis cache on Heroku we have to do the following steps:

 1. add library into application dependencies
 2. enable `play.cache.redis.RedisCacheModule`
 4. set `play.cache.redis.source` to `"${REDIS_URL}"` or  `"${REDISCLOUD_URL}"`.
 5. done, you can run it and use any of provided interfaces


## Overview

### Module wide (valid only under the root)

| Key                         | Type     | Default                         | Description                         |
|-------------------------------------|---------:|--------------------------------:|-------------------------------------|
| play.cache.redis.bind-default | Boolean   | `true` | Whether to bind default unqualified APIs. Applies only with runtime DI  |
| play.cache.redis.default-cache | String   | `play` | Named of the default cache, applies with `bind-default`                          |


### Instance-specific (can be locally overridden)

| Key                         | Type     | Default                         | Description                         |
|-------------------------------------|---------:|--------------------------------:|-------------------------------------|
| play.cache.redis.source     | String   | `standalone`                        | Defines the source of the configuration. Accepted values are `standalone`, `cluster`, `connection-string`, and `custom` |
| play.cache.redis.timeout    | Duration | `1s`                            | conversion timeout applied by `SyncAPI` to convert `Future[T]` to `T`|
| play.cache.redis.prefix    | String | `null`                            | optional namespace, i.e., key prefix |
| play.cache.redis.dispatcher | String   | `akka.actor.default-dispatcher` | Akka actor                          |
| play.cache.redis.recovery   | String   | `log-and-default`               | Defines behavior when command execution fails. Accepted values are `log-and-fail` to log the error and rethrow the exception, `log-and-default` to log the failure and return default value neutral to the operation, `log-condensed-and-default` `log-condensed-and-fail` produce shorter but less informative error logs, and `custom` indicates the user binds his own implementation of `RecoveryPolicy`.        |

