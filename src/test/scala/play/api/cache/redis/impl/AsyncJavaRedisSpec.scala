package play.api.cache.redis.impl

import java.util.Optional

import scala.concurrent.duration.Duration

import play.api.cache.redis._
import play.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class AsyncJavaRedisSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import JavaCompatibility._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "Java Redis Cache" should {

    "get and miss" in new MockedJavaRedis {
      async.get[String](anyString)(anyClassTag) returns None
      cache.get[String](key).asScala must beNull.await
    }

    "get and hit" in new MockedJavaRedis {
      async.get[String](beEq(key))(anyClassTag) returns Some(value)
      async.get[String](beEq(classTagKey))(anyClassTag) returns Some(classTag)
      cache.get[String](key).asScala must beEqualTo(value).await
    }

    "get null" in new MockedJavaRedis {
      async.get[String](beEq(classTagKey))(anyClassTag) returns Some("null")
      cache.get[String](key).asScala must beNull.await
      there was one(async).get[String](classTagKey)
    }

    "set" in new MockedJavaRedis {
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.set(key, value).asScala must beDone.await
      there was one(async).set(key, value, Duration.Inf)
      there was one(async).set(classTagKey, classTag, Duration.Inf)
    }

    "set with expiration" in new MockedJavaRedis {
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.set(key, value, expiration.toSeconds.toInt).asScala must beDone.await
      there was one(async).set(key, value, expiration)
      there was one(async).set(classTagKey, classTag, expiration)
    }

    "set null" in new MockedJavaRedis {
      async.set(anyString, any, any[Duration]) returns execDone
      cache.set(key, null: AnyRef).asScala must beDone.await
      there was one(async).set(key, null, Duration.Inf)
      there was one(async).set(classTagKey, "null", Duration.Inf)
    }

    "get or else (hit)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(key))(anyClassTag) returns Some(value)
      async.get[String](beEq(classTagKey))(anyClassTag) returns Some(classTag)
      cache.getOrElse(key, doElse(value)).asScala must beEqualTo(value).await
      cache.getOrElseUpdate(key, doFuture(value).asJava).asScala must beEqualTo(value).await
      orElse mustEqual 0
      there was two(async).get[String](key)
      there was two(async).get[String](classTagKey)
    }

    "get or else (miss)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(classTagKey))(anyClassTag) returns None
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.getOrElse(key, doElse(value)).asScala must beEqualTo(value).await
      cache.getOrElseUpdate(key, doFuture(value).asJava).asScala must beEqualTo(value).await
      orElse mustEqual 2
      there was two(async).get[String](classTagKey)
      there was two(async).set(key, value, Duration.Inf)
      there was two(async).set(classTagKey, classTag, Duration.Inf)
    }

    "get or else with expiration (hit)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(key))(anyClassTag) returns Some(value)
      async.get[String](beEq(classTagKey))(anyClassTag) returns Some(classTag)
      cache.getOrElse(key, doElse(value), expiration.toSeconds.toInt).asScala must beEqualTo(value).await
      cache.getOrElseUpdate(key, doFuture(value).asJava, expiration.toSeconds.toInt).asScala must beEqualTo(value).await
      orElse mustEqual 0
      there was two(async).get[String](key)
    }

    "get or else with expiration (miss)" in new MockedJavaRedis with OrElse {
      async.get[String](beEq(classTagKey))(anyClassTag) returns None
      async.set(anyString, anyString, any[Duration]) returns execDone
      cache.getOrElse(key, doElse(value), expiration.toSeconds.toInt).asScala must beEqualTo(value).await
      cache.getOrElseUpdate(key, doFuture(value).asJava, expiration.toSeconds.toInt).asScala must beEqualTo(value).await
      orElse mustEqual 2
      there was two(async).get[String](classTagKey)
      there was two(async).set(key, value, expiration)
      there was two(async).set(classTagKey, classTag, expiration)
    }

    "get optional (none)" in new MockedJavaRedis {
      async.get[String](anyString)(anyClassTag) returns None
      cache.getOptional[String](key).asScala must beEqualTo(Optional.ofNullable(null)).await
    }

    "get optional (some)" in new MockedJavaRedis {
      async.get[String](anyString)(anyClassTag) returns Some("value")
      async.get[String](beEq(classTagKey))(anyClassTag) returns Some(classTag)
      cache.getOptional[String](key).asScala must beEqualTo(Optional.ofNullable("value")).await
    }

    "remove" in new MockedJavaRedis {
      async.remove(anyString, anyString, anyVarArgs) returns execDone
      cache.remove(key).asScala must beDone.await
      there was one(async).remove(key, classTagKey)
    }

    "remove all" in new MockedJavaRedis {
      async.invalidate() returns execDone
      cache.removeAll().asScala must beDone.await
      there was one(async).invalidate()
    }

    "get and set 'byte'" in new MockedJavaRedis {
      val byte = JavaTypes.byteValue

      // set a value
      // note: there should be hit on "byte" but the value is wrapped instead
      async.set(anyString, beEq(byte), any[Duration]) returns execDone
      async.set(anyString, beEq("byte"), any[Duration]) returns execDone
      async.set(anyString, beEq("java.lang.Byte"), any[Duration]) returns execDone
      cache.set(key, byte).asScala must beDone.await

      // hit on GET
      async.get[Byte](beEq(key))(anyClassTag) returns Some(byte)
      async.get[String](beEq(classTagKey))(anyClassTag) returns Some("java.lang.Byte")
      cache.get[Byte](key).asScala must beEqualTo(byte).await
    }

    "get and set 'byte[]'" in new MockedJavaRedis {
      val bytes = JavaTypes.bytesValue

      // set a value
      async.set(anyString, beEq(bytes), any[Duration]) returns execDone
      async.set(anyString, beEq("byte[]"), any[Duration]) returns execDone
      cache.set(key, bytes).asScala must beDone.await

      // hit on GET
      async.get[Array[Byte]](beEq(key))(anyClassTag) returns Some(bytes)
      async.get[String](beEq(classTagKey))(anyClassTag) returns Some("byte[]")
      cache.get[Array[Byte]](key).asScala must beEqualTo(bytes).await
    }

    "get all" in new MockedJavaRedis {
      async.getAll[String](beEq(Iterable(key, key, key)))(anyClassTag) returns Seq(Some(value), None, None)
      cache.getAll(classOf[String], key, key, key).asScala.map(_.asScala) must beEqualTo(Seq(Optional.of(value), Optional.empty, Optional.empty)).await
    }

    "get all (keys in a collection)" in new MockedJavaRedis {
      async.getAll[String](beEq(Iterable(key, key, key)))(anyClassTag) returns Seq(Some(value), None, None)
      cache.getAll(classOf[String], JavaList(key, key, key)).asScala.map(_.asScala) must beEqualTo(Seq(Optional.of(value), Optional.empty, Optional.empty)).await
    }

    "set if not exists (exists)" in new MockedJavaRedis {
      async.setIfNotExists(beEq(key), beEq(value), any[Duration]) returns false
      async.setIfNotExists(beEq(classTagKey), beEq(classTag), any[Duration]) returns false
      cache.setIfNotExists(key, value).asScala.map(Boolean.unbox) must beFalse.await
      there was one(async).setIfNotExists(key, value, null)
      there was one(async).setIfNotExists(classTagKey, classTag, null)
    }

    "set if not exists (not exists)" in new MockedJavaRedis {
      async.setIfNotExists(beEq(key), beEq(value), any[Duration]) returns true
      async.setIfNotExists(beEq(classTagKey), beEq(classTag), any[Duration]) returns true
      cache.setIfNotExists(key, value).asScala.map(Boolean.unbox) must beTrue.await
      there was one(async).setIfNotExists(key, value, null)
      there was one(async).setIfNotExists(classTagKey, classTag, null)
    }

    "set if not exists (exists) with expiration" in new MockedJavaRedis {
      async.setIfNotExists(beEq(key), beEq(value), any[Duration]) returns false
      async.setIfNotExists(beEq(classTagKey), beEq(classTag), any[Duration]) returns false
      cache.setIfNotExists(key, value, expirationInt).asScala.map(Boolean.unbox) must beFalse.await
      there was one(async).setIfNotExists(key, value, expiration)
      there was one(async).setIfNotExists(classTagKey, classTag, expiration)
    }

    "set if not exists (not exists) with expiration" in new MockedJavaRedis {
      async.setIfNotExists(beEq(key), beEq(value), any[Duration]) returns true
      async.setIfNotExists(beEq(classTagKey), beEq(classTag), any[Duration]) returns true
      cache.setIfNotExists(key, value, expirationInt).asScala.map(Boolean.unbox) must beTrue.await
      there was one(async).setIfNotExists(key, value, expiration)
      there was one(async).setIfNotExists(classTagKey, classTag, expiration)
    }

    "set all" in new MockedJavaRedis {
      async.setAll(anyVarArgs) returns Done
      cache.setAll(new KeyValue(key, value), new KeyValue(other, value)).asScala must beDone.await
      there was one(async).setAll((key, value), (classTagKey, classTag), (other, value), (classTagOther, classTag))
    }

    "set all if not exists (exists)" in new MockedJavaRedis {
      async.setAllIfNotExist(anyVarArgs) returns false
      cache.setAllIfNotExist(new KeyValue(key, value), new KeyValue(other, value)).asScala.map(Boolean.unbox) must beFalse.await
      there was one(async).setAllIfNotExist((key, value), (classTagKey, classTag), (other, value), (classTagOther, classTag))
    }

    "set all if not exists (not exists)" in new MockedJavaRedis {
      async.setAllIfNotExist(anyVarArgs) returns true
      cache.setAllIfNotExist(new KeyValue(key, value), new KeyValue(other, value)).asScala.map(Boolean.unbox) must beTrue.await
      there was one(async).setAllIfNotExist((key, value), (classTagKey, classTag), (other, value), (classTagOther, classTag))
    }

    "append" in new MockedJavaRedis {
      async.append(anyString, anyString, any[Duration]) returns Done
      async.setIfNotExists(anyString, anyString, any[Duration]) returns false
      cache.append(key, value).asScala must beDone.await
      there was one(async).append(key, value, null)
      there was one(async).setIfNotExists(classTagKey, classTag, null)
    }

    "append with expiration" in new MockedJavaRedis {
      async.append(anyString, anyString, any[Duration]) returns Done
      async.setIfNotExists(anyString, anyString, any[Duration]) returns false
      cache.append(key, value, expirationInt).asScala must beDone.await
      there was one(async).append(key, value, expiration)
      there was one(async).setIfNotExists(classTagKey, classTag, expiration)
    }

    "expire" in new MockedJavaRedis {
      async.expire(anyString, any[Duration]) returns Done
      cache.expire(key, expirationInt).asScala must beDone.await
      there was one(async).expire(key, expiration)
      there was one(async).expire(classTagKey, expiration)
    }

    "expires in (defined)" in new MockedJavaRedis {
      async.expiresIn(anyString) returns Some(expiration)
      cache.expiresIn(key).asScala must beEqualTo(Optional.of(expirationLong)).await
      there was one(async).expiresIn(key)
      there was no(async).expiresIn(classTagKey)
    }

    "expires in (undefined)" in new MockedJavaRedis {
      async.expiresIn(anyString) returns None
      cache.expiresIn(key).asScala must beEqualTo(Optional.empty).await
      there was one(async).expiresIn(key)
      there was no(async).expiresIn(classTagKey)
    }

    "matching" in new MockedJavaRedis {
      async.matching(anyString) returns Seq(key)
      cache.matching("pattern").asScala.map(_.asScala) must beEqualTo(Seq(key)).await
      there was one(async).matching("pattern")
    }

    "remove multiple" in new MockedJavaRedis {
      async.removeAll(anyVarArgs) returns Done
      cache.remove(key, key, key, key).asScala must beDone.await
      there was one(async).removeAll(key, classTagKey, key, classTagKey, key, classTagKey, key, classTagKey)
    }

    "remove all" in new MockedJavaRedis {
      async.removeAll(anyVarArgs) returns Done
      cache.removeAllKeys(key, key, key, key).asScala must beDone.await
      there was one(async).removeAll(key, classTagKey, key, classTagKey, key, classTagKey, key, classTagKey)
    }

    "remove matching" in new MockedJavaRedis {
      async.removeMatching(anyString) returns Done
      cache.removeMatching("pattern").asScala must beDone.await
      there was one(async).removeMatching("pattern")
      there was one(async).removeMatching("classTag::pattern")
    }

    "exists" in new MockedJavaRedis {
      async.exists(beEq(key)) returns true
      cache.exists(key).asScala.map(Boolean.unbox) must beTrue.await
      there was one(async).exists(key)
      there was no(async).exists(classTagKey)
    }

    "increment" in new MockedJavaRedis {
      async.increment(beEq(key), anyLong) returns 10L
      cache.increment(key).asScala.map(Long.unbox) must beEqualTo(10L).await
      cache.increment(key, 2L).asScala.map(Long.unbox) must beEqualTo(10L).await
      there was one(async).increment(key, by = 1L)
      there was one(async).increment(key, by = 2L)
    }

    "decrement" in new MockedJavaRedis {
      async.decrement(beEq(key), anyLong) returns 10L
      cache.decrement(key).asScala.map(Long.unbox) must beEqualTo(10L).await
      cache.decrement(key, 2L).asScala.map(Long.unbox) must beEqualTo(10L).await
      there was one(async).decrement(key, by = 1L)
      there was one(async).decrement(key, by = 2L)
    }

    "create list" in new MockedJavaRedis {
      private val list = mock[RedisList[String, Future]]
      async.list(beEq(key))(anyClassTag[String]) returns list
      cache.list(key, classOf[String]) must beAnInstanceOf[AsyncRedisList[String]]
      there was one(async).list[String](key)
    }

    "create set" in new MockedJavaRedis {
      private val set = mock[RedisSet[String, Future]]
      async.set(beEq(key))(anyClassTag[String]) returns set
      cache.set(key, classOf[String]) must beAnInstanceOf[AsyncRedisSet[String]]
      there was one(async).set[String](key)
    }

    "create map" in new MockedJavaRedis {
      private val map = mock[RedisMap[String, Future]]
      async.map(beEq(key))(anyClassTag[String]) returns map
      cache.map(key, classOf[String]) must beAnInstanceOf[AsyncRedisMap[String]]
      there was one(async).map[String](key)
    }
  }
}
