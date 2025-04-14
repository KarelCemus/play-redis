# Configuration

Default configuration and very detailed manual is available in [reference.conf](https://github.com/KarelCemus/play-redis/blob/5.1.0/src/main/resources/reference.conf). It can be overwritten in your `conf/application.conf` file.

There are several features supported in the configuration, they are discussed below. However, by default, there is no need for any further configuration. Default settings are set to the standalone instance running on `localhost:6379?db=0`, which is default for redis server. This instance is named `play` but is also exposed as a default implementation.

**The configuration root is `play.cache.redis`**

## Standalone vs. Cluster

This implementation supports both standalone and cluster instances. By default, the standalone mode is enabled.  It is configured like this:

```hocon
  play.cache.redis {
    host:       localhost
    # redis server: port
    port:       6379
    # redis server: database number (optional)
    database:   0
    # authentication username (optional) with "redis" as fallback
    username:   null
    # authentication password (optional)
    password:   null
  }
```

To enable cluster mode instead, use `source` property. Valid values are `standalone` (default), `cluster`, `connection-string`, and `custom`. For more details, see below. Example of cluster settings:

```hocon
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
      # optional string, defines a username to use with "redis" as fallback
      username:    null
      # optional string, defines a password to use
      password:    null
    }
  ]
}
```

## Aws Cluster

Some platforms such as Amazon AWS use a single DNS record to define a whole cluster. Such
a domain name resolves to multiple IP addresses, which are nodes of a cluster.

```hocon
play.cache.redis {
  instances {
    play {
      host:    cluster.domain.name.com
      source:  aws-cluster
    }
  }
}
``` 

## Sentinel

Use `source: sentinel` to enable sentinel mode. Required parameters are
`master_group_name: ...` and `sentinels: []`. An example of sentinel settings:

```hocon
play.cache.redis {
    source: sentinel

    # master name that you specify when using the `sentinel
    # get-master-addr-by-name NAME` command
    master-group: r0

    # Number of your redis database (optional)
    database: 1
    # Username to your redis hosts (optional)
    userame: some-username
    # Password to your redis hosts (optional)
    password: something

    # List of sentinels
    sentinels: [
        {
            host: localhost
            port: 16380
        },
        {
            host: localhost
            port: 16381
        },
        {
            host: localhost
            port: 16382
        }
    ]
}
```

## Master-Slaves

Use `source: master-slaves` to enable master-slaves mode.
In this mode write only to the master node and read from one of slaves node.
Required parameters are `master: {...}` and `slaves: []`. An example of master-slaves settings:

```hocon
play.cache.redis {
    source: master-slaves

    # username to your redis hosts (optional)
    username: some-username
    # password to your redis hosts, use if not specified for a specific node (optional)
    password: "my-password"
    # number of your redis database, use if not specified for a specific node (optional)
    database: 1 
    
    # master node
    master: {
        host: "localhost"
        port: 6380
        # number of your redis database on master (optional)
        database: 1
        # username on master host (optional)
        username: some-username
        # password on master host (optional)
        password: something
    }
    # slave nodes
    slaves: [
        {
            host: "localhost"
            port: 6381
            # number of your redis database on slave (optional)
            database: 1
            # username on slave host (optional)
            username: some-username
            # password on slave host (optional)
            password: something
        }
    ]
}
```

## Named caches

Play framework supports [named caches](https://www.playframework.com/documentation/3.0.x/ScalaCache#Accessing-different-caches) through a qualifier. For a simplicity, the default cache is also exposed without a qualifier to ease the access. This feature can be disabled by `bind-default` property, which defaults to true. The name of the default cache is defined in `default-cache` property, which defaults to `play` to keep consistency with Play framework.

The configuration of each instance is inherited from the `play.cache.redis` configuration. Inherited values may be locally overridden by the instance's own configuration (e.g., `play.cache.redis.instances.myNamedCache`)

Named caches are defined under the `play.cache.redis.instances` node, which automatically **disables and ignores** the default cache defined directly under the root `play.cache.redis`. The instances are defined as a map with name-definition pairs.

```hocon
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

```scala
class MyController @Inject()( @NamedCache( "myNamedCache" ) local: CacheAsyncApi ) {
  // my implementation
}
```


## Namespace prefix

Each cache can optionally define a namespace for keys. It means that each key is automatically prefixed. For example, having `prefix: "my-prefix"` and the key `my-key`, the cache operates with `my-prefix:my-key`. This behavior is especially useful to avoid collisions, when:

1. either we have multiple named caches working with the same redis database,
2. or there is another application working with the same redis database.

This property may be locally overridden for each named cache.


## Timeout

The `play-redis` is designed fully asynchronously and there is no timeout applied
by this library itself. However, there are other timeouts you might be interested in.

### Synchronization timeout

First, when you use `SyncAPI` instead of `AsyncAPI`, the **internal `Future[T]` has to be converted
into `T`**. It uses [`Await.result`](https://www.scala-lang.org/api/current/scala/concurrent/Await$.html#result%5BT%5D(awaitable:scala.concurrent.Awaitable%5BT%5D,atMost:scala.concurrent.duration.Duration):T)
from standard Scala library, which requires a timeout definition. This is the `play.cache.redis.sync-timeout`.

This timeout applies on the invocation of the whole request including the communication to Redis, data serialization,
and invocation `orElse` part when the `Future` is used. If you don't want any timeouts and your application logic
has to never timeout, just set it to something really high or use asynchronous API to be absolutely sure.

### Timeout on Redis commands

Second, there is a `redis-timeout`, which
limits the waiting for the response from the redis server. However, it is expected
the redis works smoothly thus no timeout is necessary. In consequence, it is **disabled
by default** to avoid unnecessary performance penalty.

### Timeout when disconnected

There is a `connection-timeout`, which
limits the waiting for the response from the redis server when the connection is
not established. As the connection is fully asynchronnous, it tends to wait endlessly. 
To avoid hanging of requests, the connection timeout defines the upper bound of
waiting for a response. Each request is eather resolved or rejected within this
time window. It is expected the redis works smoothly thus the timeout
usually does not apply. However, it is expected the cache should be fast responding
and thus **by default the timeout is set to 500 millis** to avoid unnecessary delays.
This timeout is optional and can be disabled.

## ThreadPool

These are ResourceClient settings passed to Lettuce, a Java Redis client library. For more information see

https://redis.github.io/lettuce/advanced-usage/

```hocon
play.cache.redis {
  io-thread-pool-size:          8 // default 8, min 3
  computation-thread-pool-size: 8 // default 8, min 3
}
```
## Recovery policy

The intention of cache is usually to optimize the application behavior, not to provide any business logic.
In this case, it makes sense the cache could be removed without any visible change except for possible
performance loss.

In consequence, **failed cache requests should not break the application flow**,
they should be logged and ignored. However, not always this is the desired behavior. To resolve this ambiguity,
the module provides `RecoveryPolicy` trait implementing the behavior to be executed when the cache request fails.
There are two major implementations. They both log the failure at first, and while one produces
the exception and let the application to deal with it, the other returns some neutral value, which
should result in behavior like there is no cache (default policy). However, besides these, it is possible,
e.g., to also implement your own policy to, e.g., rerun a failed command. For more information
see `RecoveryPolicy` trait.

```hocon
  # By default, there are two basic implementations:
  #
  # 'log-and-fail':               Logs the error at first and then emits RedisException
  #
  # 'log-condensed-and-fail':     Same as 'log-and-fail' but with reduced logging.
  #
  #
  #
  # 'log-and-default':            Logs the error at first and then returns operation
  #                               neutral value, which should look like there is no
  #                               cache in use.
  #
  # 'log-condensed-and-default':  Same as 'log-and-default' but with reduced logging.
  #
  # 'custom':             User provides his own binding to implementation of `RecoveryPolicy`
  #
  #
  # Besides logging and re-populating the exceptions, it is also possible to
  # send email or re-run the failed command, which could like like certain robustness.
  #
  # note: this is global definition, can be locally overriden for each
  # cache instance. To do so, redefine this property
  # under 'play.cache.redis.instances.instance-name.this-property'.
  #
  recovery:         log-and-default
```

### Custom Recovery Policy

Besides the default implementations, you are free to extend the `RecoveryPolicy` trait and provide your own implementation. For example:

```scala
class MyPolicy extends RecoveryPolicy {
  def recoverFrom[ T ]( rerun: => Future[ T ], default: => Future[ T ], failure: RedisException ) = default
}
```

Next, name it and update the configuration file: `play.cache.redis.recovery: custom`, where `custom` is the name. **Any name is possible**.

Then, if you use runtime DI (e.g. Guice), you have to bind the named `RecoveryPolicy` trait to your implementation.
For example, create a module for it. **Don't forget to register the module** into enabled modules. And that's it.

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

If you use compile-time DI, override `recoveryPolicyResolver` in `RedisCacheComponents`
and return the instance when the policy name matches.


## Eager and Lazy Invocation

Some operations, e.g., `getOrElse` and `getOrFuture`,
besides the intended computation of the value, invoke a
side-effect to store and cache the value, e.g.,
with the `set` operation. However, though it is usually
correct to wait for the side-effect and process the occasional
error, there exist situations, where it is safe to ignore the result
of the side-effect and return the value directly. This mechanism is
handled by the `InvocationPolicy` trait, where default `LazyInvocation`
waits for the result and considers the error (if any),
while the `EagerInvocation` does not wait for the result of the side-effect,
ignores the result and possible error and returns immediately.

The policy to use is configured within the instance. By default,
each instance uses `lazy` policy.


```hocon
  # invocation policy applies in methods `getOrElse`. It determines
  # whether to wait until the `set` completes or return eagerly the
  # computed value. Valid values:
  #  - 'lazy':  for lazy invocation waiting for the `set` completion
  #  - 'eager': for eager invocation returning the computed on miss
  #             without waiting for the `set` completion. Eager
  #             invocation ignores the error in `set`, if occurs.
  #
  # Default value is 'lazy' to properly handle errors, if occurs.
  #
  # note: this is global definition, can be locally overriden for each
  # cache instance. To do so, redefine this property
  # under 'play.cache.redis.instances.instance-name.this-property'.
  #
  invocation:       lazy
```




## Running in different environments

This module can run in various environments, from the localhost through the Heroku to your own premise. Each of these has a possibly different configuration. For this purpose, there is a `source` property accepting 4 values: `standalone` (default), `cluster`, `connection-string`, `master-slaves` and `custom`.

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

Second, when none of the already existing providers is sufficient, you can implement your own and let the
`RedisInstanceResolver` to take care of it. When the module finds a `custom` source, it calls the resolver
with the cache name and expects the configuration in return. So all you need to do is to implement your
own resolver and register it with DI. Details may differ based on the type of DI you use.

### Example: Running on Heroku

To enable redis cache on Heroku we have to do the following steps:

 1. add library into application dependencies
 2. enable `play.cache.redis.RedisCacheModule`
 4. set `play.cache.redis.source` to `"${REDIS_URL}"` or  `"${REDISCLOUD_URL}"`.
 5. done, you can run it and use any of provided interfaces


## Limitation of Data Serialization

The major limitation of this module is data serialization required for their transmission
to and from the server. Play-redis provides native serialization support to basic data
types such as String, Int, etc. However, for other objects including collections,
it uses `JavaSerializer` by default.

Since Akka 2.4.1, default `JavaSerializer` is [officially considered inefficient for production use](https://github.com/akka/akka/pull/18552).
Nevertheless, to keep things simple, play-redis **still uses this inefficient serializer NOT to enforce** any serialization
library to end users. Although, it recommends [kryo serializer](https://github.com/romix/akka-kryo-serialization) claiming
great performance and small output stream. Any serialization library can be smoothly connected through Pekko
configuration, see the [official Pekko documentation](https://pekko.apache.org/docs/pekko/current/serialization.html).


## Overview

### Module wide (valid only under the root)

| Key                         | Type     | Default                         | Description                         |
|-------------------------------------|---------:|--------------------------------:|-------------------------------------|
| play.cache.redis.bind-default | Boolean   | `true` | Whether to bind default unqualified APIs. Applies only with runtime DI  |
| play.cache.redis.default-cache | String   | `play` | Named of the default cache, applies with `bind-default`                          |


### Instance-specific (can be locally overridden)

| Key                                                      | Type     |                              Default | Description                                                                                                                             |
|----------------------------------------------------------|---------:|-------------------------------------:|-----------------------------------------------------------------------------------------------------------------------------------------|
| [play.cache.redis.source](#standalone-vs-cluster)        | String   |                         `standalone` | Defines the source of the configuration. Accepted values are `standalone`, `cluster`, `connection-string`, `master-slaves` and `custom` |
| [play.cache.redis.sync-timeout](#timeout)                | Duration |                                 `1s` | conversion timeout applied by `SyncAPI` to convert `Future[T]` to `T`                                                                   |
| [play.cache.redis.redis-timeout](#timeout)               | Duration |                               `null` | waiting for the response from redis server                                                                                              |
| [play.cache.redis.prefix](#namespace-prefix)             | String   |                               `null` | optional namespace, i.e., key prefix                                                                                                    |
| play.cache.redis.dispatcher                              | String   | `pekko.actor.default-dispatcher` | Pekko actor                                                                                                                             |
| [play.cache.redis.recovery](#recovery-policy)            | String   |                    `log-and-default` | Defines behavior when command execution fails. For accepted values and more see                                                         |
