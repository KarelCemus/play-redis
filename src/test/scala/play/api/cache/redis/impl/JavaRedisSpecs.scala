package play.api.cache.redis.impl

import java.util.concurrent.Callable

import scala.concurrent.duration.Duration

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

/**
  * @author Karel Cemus
  */
class JavaRedisSpecs( implicit ee: ExecutionEnv ) extends Specification with ReducedMockito {

  import Implicits._
  import JavaRedis._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "Java Redis Cache" should {

    "get and miss" in new MockedJavaRedis {
      async.get[ String ]( anyString )( anyClassTag ) returns None
      cache.get[ String ]( key ).toScala must beNull.await
    }

    "get and hit" in new MockedJavaRedis {
      async.get[ String ]( beEq( key ) )( anyClassTag ) returns Some( value )
      async.get[ String ]( beEq( s"classTag::$key" ) )( anyClassTag ) returns Some( "java.lang.String" )
      cache.get[ String ]( key ).toScala must beEqualTo( value ).await
    }

    "get null" in new MockedJavaRedis {
      async.get[ String ]( beEq( s"classTag::$key" ) )( anyClassTag ) returns Some( "" )
      cache.get[ String ]( key ).toScala must beNull.await
      there was one( async ).get[ String ]( s"classTag::$key" )
    }

    "set" in new MockedJavaRedis {
      async.set( anyString, anyString, any[ Duration ] ) returns execDone
      cache.set( key, value ).toScala must beDone.await
      there was one( async ).set( key, value, Duration.Inf )
      there was one( async ).set( s"classTag::$key", "java.lang.String", Duration.Inf )
    }

    "set with expiration" in new MockedJavaRedis {
      async.set( anyString, anyString, any[ Duration ] ) returns execDone
      cache.set( key, value, expiration.toSeconds.toInt ).toScala must beDone.await
      there was one( async ).set( key, value, expiration )
      there was one( async ).set( s"classTag::$key", "java.lang.String", expiration )
    }

    "set null" in new MockedJavaRedis {
      async.set( anyString, any, any[ Duration ] ) returns execDone
      cache.set( key, null ).toScala must beDone.await
      there was one( async ).set( key, null, Duration.Inf )
      there was one( async ).set( s"classTag::$key", "", Duration.Inf )
    }

    "get or else (hit)" in new MockedJavaRedis with OrElse {
      async.get[ String ]( beEq( key ) )( anyClassTag ) returns Some( value )
      async.get[ String ]( beEq( s"classTag::$key" ) )( anyClassTag ) returns Some( "java.lang.String" )
      cache.getOrElseUpdate( key, doFuture( value ).toJava ).toScala must beEqualTo( value ).await
      orElse mustEqual 0
      there was one( async ).get[ String ]( key )
    }

    "get or else (miss)" in new MockedJavaRedis with OrElse {
      async.get[ String ]( beEq( s"classTag::$key" ) )( anyClassTag ) returns None
      async.set( anyString, anyString, any[ Duration ] ) returns execDone
      cache.getOrElseUpdate( key, doFuture( value ).toJava ).toScala must beEqualTo( value ).await
      orElse mustEqual 1
      there was one( async ).get[ String ]( s"classTag::$key" )
      there was one( async ).set( key, value, Duration.Inf )
      there was one( async ).set( s"classTag::$key", "java.lang.String", Duration.Inf )
    }

    "get or else with expiration (hit)" in new MockedJavaRedis with OrElse {
      async.get[ String ]( beEq( key ) )( anyClassTag ) returns Some( value )
      async.get[ String ]( beEq( s"classTag::$key" ) )( anyClassTag ) returns Some( "java.lang.String" )
      cache.getOrElseUpdate( key, doFuture( value ).toJava, expiration.toSeconds.toInt ).toScala must beEqualTo( value ).await
      orElse mustEqual 0
      there was one( async ).get[ String ]( key )
    }

    "get or else with expiration (miss)" in new MockedJavaRedis with OrElse {
      async.get[ String ]( beEq( s"classTag::$key" ) )( anyClassTag ) returns None
      async.set( anyString, anyString, any[ Duration ] ) returns execDone
      cache.getOrElseUpdate( key, doFuture( value ).toJava, expiration.toSeconds.toInt ).toScala must beEqualTo( value ).await
      orElse mustEqual 1
      there was one( async ).get[ String ]( s"classTag::$key" )
      there was one( async ).set( key, value, expiration )
      there was one( async ).set( s"classTag::$key", "java.lang.String", expiration )
    }

    "remove" in new MockedJavaRedis {
      async.remove( anyString ) returns execDone
      cache.remove( key ).toScala must beDone.await
    }

    "remove all" in new MockedJavaRedis {
      async.invalidate() returns execDone
      cache.removeAll().toScala must beDone.await
    }
  }
}
