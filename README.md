<div align="center">

  # Redis Cache module for Play framework

  **This version supports Play framework 2.6.x with JDK 8 and both Scala 2.11 and Scala 2.12.**<br/>
  **For previous versions see older releases.**

  [![Travis CI: Status](https://travis-ci.org/KarelCemus/play-redis.svg?branch=master)](https://travis-ci.org/KarelCemus/play-redis)
  [![Coverage Status](https://coveralls.io/repos/github/KarelCemus/play-redis/badge.svg?branch=master)](https://coveralls.io/github/KarelCemus/play-redis?branch=master)

</div>


## About the project

This module for Play framework 2 adds support of Redis cache server and provides
a set of handful APIs. For more details and **full documentation for the version `2.x.x`** and newer please **see
[the wiki](https://github.com/KarelCemus/play-redis/wiki)**. For the documentation of older versions see
README at corresponding tag in git history. For example, **documentation for the latest released version 
`1.6.1` is [here](https://github.com/KarelCemus/play-redis/blob/1.6.1/README.md)**. 

## How to add the module into the project

To your SBT `build.sbt` add the following lines:

```scala
// enable Play cache API (based on your Play version)
libraryDependencies += play.sbt.PlayImport.cacheApi
// include play-redis library
libraryDependencies += "com.github.karelcemus" %% "play-redis" % "1.6.1"
```

## Compatibility matrix

| play framework  | play-redis     |
|-----------------|---------------:|
| 2.6.x           | 1.6.1          |
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
