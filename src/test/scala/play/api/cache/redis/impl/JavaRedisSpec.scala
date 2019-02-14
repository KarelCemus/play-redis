package play.api.cache.redis.impl

import java.util.Optional

import scala.concurrent.duration.Duration

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class JavaRedisSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {

  import Implicits._
  import JavaRedis._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "Java Redis Cache" should {

    "get and miss" in new MockedJavaRedis {
      async.get[String](anyString)(anyClassTag) returns None
      cache.get[String](key).toScala must beNull.await
    }

    "get and hit" in new MockedJavaRedis {
      async.get[String](beEq(key))(anyClassTag) returns Some(value)
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns Some("java.lang.String")
      cache.get[String](key).toScala must beEqualTo(value).await
    }

    "get null" in new MockedJavaRedis {
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns Some("")
      cache.get[String](key).toScala must beNull.await
      there was one(async).get[String](s"classTag::$key")
    }

    "set" in new MockedJavaRedis {
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.set(key, value).toScala must beDone.await
      there was one(async).set(key, value, Duration.Inf)
      there was one(async).set(s"classTag::$key", "java.lang.String", Duration.Inf)
    }

    "set with expiration" in new MockedJavaRedis {
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.set(key, value, expiration.toSeconds.toInt).toScala must beDone.await
      there was one(async).set(key, value, expiration)
      there was one(async).set(s"classTag::$key", "java.lang.String", expiration)
    }

    "set null" in new MockedJavaRedis {
      async.set(anyString, any, any[Duration]) returns execDone
      cache.set(key, null).toScala must beDone.await
      there was one(async).set(key, null, Duration.Inf)
      there was one(async).set(s"classTag::$key", "", Duration.Inf)
    }

    "get or else (hit)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(key))(anyClassTag) returns Some(value)
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns Some("java.lang.String")
      cache.getOrElseUpdate(key, doFuture(value).toJava).toScala must beEqualTo(value).await
      orElse mustEqual 0
      there was one(async).get[String](key)
    }

    "get or else (miss)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns None
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.getOrElseUpdate(key, doFuture(value).toJava).toScala must beEqualTo(value).await
      orElse mustEqual 1
      there was one(async).get[String](s"classTag::$key")
      there was one(async).set(key, value, Duration.Inf)
      there was one(async).set(s"classTag::$key", "java.lang.String", Duration.Inf)
    }

    "get or else with expiration (hit)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(key))(anyClassTag) returns Some(value)
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns Some("java.lang.String")
      cache.getOrElseUpdate(key, doFuture(value).toJava, expiration.toSeconds.toInt).toScala must beEqualTo(value).await
      orElse mustEqual 0
      there was one(async).get[String](key)
    }

    "get or else with expiration (miss)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns None
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.getOrElseUpdate(key, doFuture(value).toJava, expiration.toSeconds.toInt).toScala must beEqualTo(value).await
      orElse mustEqual 1
      there was one(async).get[String](s"classTag::$key")
      there was one(async).set(key, value, expiration)
      there was one(async).set(s"classTag::$key", "java.lang.String", expiration)
    }

    "get optional (none)" in new MockedJavaRedis {
      async.get[String](anyString)(anyClassTag) returns None
      cache.getOptional[String](key).toScala must beEqualTo(Optional.ofNullable(null)).await
    }

    "get optional (some)" in new MockedJavaRedis {
      async.get[String](anyString)(anyClassTag) returns Some("value")
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns Some("java.lang.String")
      cache.getOptional[String](key).toScala must beEqualTo(Optional.ofNullable("value")).await
    }

    "remove" in new MockedJavaRedis {
      async.remove(anyString) returns execDone
      cache.remove(key).toScala must beDone.await
    }

    "remove all" in new MockedJavaRedis {
      async.invalidate() returns execDone
      cache.removeAll().toScala must beDone.await
    }

    "get and set 'byte'" in new MockedJavaRedis {
      val byte = JavaTypes.byteValue

      // set a value
      // note: there should be hit on "byte" but the value is wrapped instead
      async.set(anyString, beEq(byte), any[Duration]) returns execDone
      async.set(anyString, beEq("byte"), any[Duration]) returns execDone
      async.set(anyString, beEq("java.lang.Byte"), any[Duration]) returns execDone
      cache.set(key, byte).toScala must beDone.await

      // hit on GET
      async.get[Byte](beEq(key))(anyClassTag) returns Some(byte)
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns Some("java.lang.Byte")
      cache.get[Byte](key).toScala must beEqualTo(byte).await
    }

    "get and set 'byte[]'" in new MockedJavaRedis {
      val bytes = JavaTypes.bytesValue

      // set a value
      async.set(anyString, beEq(bytes), any[Duration]) returns execDone
      async.set(anyString, beEq("byte[]"), any[Duration]) returns execDone
      cache.set(key, bytes).toScala must beDone.await

      // hit on GET
      async.get[Array[Byte]](beEq(key))(anyClassTag) returns Some(bytes)
      async.get[String](beEq(s"classTag::$key"))(anyClassTag) returns Some("byte[]")
      cache.get[Array[Byte]](key).toScala must beEqualTo(bytes).await
    }
  }
}
