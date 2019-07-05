# How to Use this Module

When you have the library added to your project, you can safely inject
`play.api.cache.redis.CacheApi` trait for the synchronous cache. If you
want the asynchronous implementation, then inject `play.api.cache.redis.CacheAsyncApi`.
And for Java version, there is available `play.cache.redis.AsyncCacheApi`,
which provides Java-friendly interface, though it is limited, slightely slower,
and has to deal with missing `ClassTag`.

Besides various common operations over the cache, the API supports working with
the collections: List, Set, and Map. First, create a typed worker to use the collection
under the give key. For example: `cache.list[ String ]( "my-list" )` Then you can fully
operate the collection. Please **be aware of the complexity** of the operations and
**optimize your code**. Although the API is simple and seems efficient, each of your calls
is transmitted to Redis.

## Checking operation result

Regardless of current API, all operations throw an exception when fail. Consequently,
successful invocations do not throw an exception. The only difference is in checking for errors.
While synchronous APIs really throw an exception, asynchronous API returns a `Future`
wrapping both the success and the exception, i.e., use `onFailure` or `onComplete` to
check for errors.


## Use of `CacheApi`

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
  cache.getOrFuture( "key" )( Future.successful( 1.24 ) )

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

## Use of Lists

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

## Use of Sets

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

## Use of Maps:

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
