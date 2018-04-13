package play.api.cache.redis.impl

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

/**
  * @author Karel Cemus
  */
class RedisMapSpecs( implicit ee: ExecutionEnv ) extends Specification with ReducedMockito {

  import Implicits._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "Redis Map" should {

    "set" in new MockedMap {
      connector.hashSet( anyString, anyString, anyString ) returns true
      map.add( field, value ) must beEqualTo( map ).await
      there were one( connector ).hashSet( key, field, value )
    }

    "set (failing)" in new MockedMap {
      connector.hashSet( anyString, anyString, anyString ) returns ex
      map.add( field, value ) must beEqualTo( map ).await
      there were one( connector ).hashSet( key, field, value )
    }

    "get" in new MockedMap {
      connector.hashGet[ String ]( anyString, beEq( field ) )( anyClassTag ) returns Some( value )
      connector.hashGet[ String ]( anyString, beEq( other ) )( anyClassTag ) returns None
      map.get( field ) must beSome( value ).await
      map.get( other ) must beNone.await
      there were one( connector ).hashGet[ String ]( key, field )
      there were one( connector ).hashGet[ String ]( key, other )
    }

    "get (failing)" in new MockedMap {
      connector.hashGet[ String ]( anyString, beEq( field ) )( anyClassTag ) returns ex
      map.get( field ) must beNone.await
      there were one( connector ).hashGet[ String ]( key, field )
    }

    "contains" in new MockedMap {
      connector.hashExists( anyString, beEq( field ) ) returns true
      connector.hashExists( anyString, beEq( other ) ) returns false
      map.contains( field ) must beTrue.await
      map.contains( other ) must beFalse.await
    }

    "contains (failing)" in new MockedMap {
      connector.hashExists( anyString, anyString ) returns ex
      map.contains( field ) must beFalse.await
      there were one( connector ).hashExists( key, field )
    }

    "remove" in new MockedMap {
      connector.hashRemove( anyString, anyVarArgs ) returns 1L
      map.remove( field ) must beEqualTo( map ).await
      map.remove( field, other ) must beEqualTo( map ).await
      there were one( connector ).hashRemove( key, field )
      there were one( connector ).hashRemove( key, field, other )
    }

    "remove (failing)" in new MockedMap {
      connector.hashRemove( anyString, anyVarArgs ) returns ex
      map.remove( field ) must beEqualTo( map ).await
      there were one( connector ).hashRemove( key, field )
    }

    "increment" in new MockedMap {
      connector.hashIncrement( anyString, beEq( field ), anyLong ) returns 5L
      map.increment( field, 2L ) must beEqualTo( 5L ).await
      there were one( connector ).hashIncrement( key, field, 2L )
    }

    "increment (failing)" in new MockedMap {
      connector.hashIncrement( anyString, beEq( field ), anyLong ) returns ex
      map.increment( field, 2L ) must beEqualTo( 2L ).await
      there were one( connector ).hashIncrement( key, field, 2L )
    }

    "toMap" in new MockedMap {
      connector.hashGetAll[ String ]( anyString )( anyClassTag ) returns Map( field -> value )
      map.toMap must beEqualTo( Map( field -> value ) ).await
    }

    "toMap (failing)" in new MockedMap {
      connector.hashGetAll[ String ]( anyString )( anyClassTag ) returns ex
      map.toMap must beEqualTo( Map.empty ).await
    }

    "keySet" in new MockedMap {
      connector.hashKeys( anyString ) returns Set( field )
      map.keySet must beEqualTo( Set( field ) ).await
    }

    "keySet (failing)" in new MockedMap {
      connector.hashKeys( anyString ) returns ex
      map.keySet must beEqualTo( Set.empty ).await
    }

    "values" in new MockedMap {
      connector.hashValues[ String ]( anyString )( anyClassTag ) returns Set( value )
      map.values must beEqualTo( Set( value ) ).await
    }

    "values (failing)" in new MockedMap {
      connector.hashValues[ String ]( anyString )( anyClassTag ) returns ex
      map.values must beEqualTo( Set.empty ).await
    }

    "size" in new MockedMap {
      connector.hashSize( key ) returns 2L
      map.size must beEqualTo( 2L ).await
    }

    "size (failing)" in new MockedMap {
      connector.hashSize( key ) returns ex
      map.size must beEqualTo( 0L ).await
    }

    "empty map" in new MockedMap {
      connector.hashSize( beEq( key ) ) returns 0L
      map.isEmpty must beTrue.await
      map.nonEmpty must beFalse.await
    }

    "non-empty map" in new MockedMap {
      connector.hashSize( beEq( key ) ) returns 1L
      map.isEmpty must beFalse.await
      map.nonEmpty must beTrue.await
    }

    "empty/non-empty map (failing)" in new MockedMap {
      connector.hashSize( beEq( key ) ) returns ex
      map.isEmpty must beTrue.await
      map.nonEmpty must beFalse.await
    }
  }
}
