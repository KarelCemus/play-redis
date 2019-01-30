package play.api.cache.redis.impl

import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RedisCacheSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  val key = "key"
  val value = "value"
  val expiration = 1.second

  "Redis Cache" should {

    "get and miss" in new MockedCache {
      connector.get[String](anyString)(anyClassTag) returns None
      cache.get[String](key) must beNone.await
    }

    "get and hit" in new MockedCache {
      connector.get[String](anyString)(anyClassTag) returns Some(value)
      cache.get[String](key) must beSome(value).await
    }

    "get recover with default" in new MockedCache {
      connector.get[String](anyString)(anyClassTag) returns ex
      cache.get[String](key) must beNone.await
    }

    "get all" in new MockedCache {
      connector.mGet[String](anyVarArgs)(anyClassTag) returns Seq(Some(value), None, None)
      cache.getAll[String](key, key, key) must beEqualTo(Seq(Some(value), None, None)).await
    }

    "get all recover with default" in new MockedCache {
      connector.mGet[String](anyVarArgs)(anyClassTag) returns ex
      cache.getAll[String](key, key, key) must beEqualTo(Seq(None, None, None)).await
    }

    "get all (keys in a collection)" in new MockedCache {
      connector.mGet[String](anyVarArgs)(anyClassTag) returns Seq(Some(value), None, None)
      cache.getAll[String](Seq(key, key, key)) must beEqualTo(Seq(Some(value), None, None)).await
    }

    "set" in new MockedCache {
      connector.set(anyString, anyString, any[Duration], beEq(false)) returns true
      cache.set(key, value) must beDone.await
    }

    "set recover with default" in new MockedCache {
      connector.set(anyString, anyString, any[Duration], beEq(false)) returns ex
      cache.set(key, value) must beDone.await
    }

    "set if not exists (exists)" in new MockedCache {
      connector.set(anyString, anyString, any[Duration], beEq(true)) returns false
      cache.setIfNotExists(key, value) must beFalse.await
    }

    "set if not exists (not exists)" in new MockedCache {
      connector.set(anyString, anyString, any[Duration], beEq(true)) returns true
      cache.setIfNotExists(key, value) must beTrue.await
    }

    "set if not exists (exists) with expiration" in new MockedCache {
      connector.set(anyString, anyString, any[Duration], beEq(true)) returns false
      connector.expire(anyString, any[Duration]) returns unit
      cache.setIfNotExists(key, value, expiration) must beFalse.await
    }

    "set if not exists (not exists) with expiration" in new MockedCache {
      connector.set(anyString, anyString, any[Duration], beEq(true)) returns true
      connector.expire(anyString, any[Duration]) returns unit
      cache.setIfNotExists(key, value, expiration) must beTrue.await
    }

    "set if not exists recover with default" in new MockedCache {
      connector.set(anyString, anyString, any[Duration], beEq(true)) returns ex
      cache.setIfNotExists(key, value) must beTrue.await
    }

    "set all" in new MockedCache {
      connector.mSet(anyVarArgs) returns unit
      cache.setAll(key -> value) must beDone.await
    }

    "set all recover with default" in new MockedCache {
      connector.mSet(anyVarArgs) returns ex
      cache.setAll(key -> value) must beDone.await
    }

    "set all if not exists (exists)" in new MockedCache {
      connector.mSetIfNotExist(anyVarArgs) returns false
      cache.setAllIfNotExist(key -> value) must beFalse.await
    }

    "set all if not exists (not exists)" in new MockedCache {
      connector.mSetIfNotExist(anyVarArgs) returns true
      cache.setAllIfNotExist(key -> value) must beTrue.await
    }

    "set all if not exists recover with default" in new MockedCache {
      connector.mSetIfNotExist(anyVarArgs) returns ex
      cache.setAllIfNotExist(key -> value) must beTrue.await
    }

    "append" in new MockedCache {
      connector.append(anyString, anyString) returns 5L
      cache.append(key, value) must beDone.await
    }

    "append with expiration" in new MockedCache {
      connector.append(anyString, anyString) returns 5L
      connector.expire(anyString, any[Duration]) returns unit
      cache.append(key, value, expiration) must beDone.await
    }

    "append recover with default" in new MockedCache {
      connector.append(anyString, anyString) returns ex
      cache.append(key, value) must beDone.await
    }

    "expire" in new MockedCache {
      connector.expire(anyString, any[Duration]) returns unit
      cache.expire(key, expiration) must beDone.await
    }

    "expire recover with default" in new MockedCache {
      connector.expire(anyString, any[Duration]) returns ex
      cache.expire(key, expiration) must beDone.await
    }

    "matching" in new MockedCache {
      connector.matching(anyString) returns Seq(key)
      cache.matching("pattern") must beEqualTo(Seq(key)).await
    }

    "matching recover with default" in new MockedCache {
      connector.matching(anyString) returns ex
      cache.matching("pattern") must beEqualTo(Seq.empty).await
    }

    "matching with a prefix" in new MockedCache {
      // define a non-empty prefix
      val prefix = "prefix"
      runtime.prefix returns new RedisPrefixImpl(prefix)
      connector.matching(beEq(s"$prefix:pattern")) returns Seq(s"$prefix:$key")
      cache.matching("pattern") must beEqualTo(Seq(key)).await
    }

    "get or else (hit)" in new MockedCache with OrElse {
      connector.get[String](anyString)(anyClassTag) returns Some(value)
      cache.getOrElse(key)(doElse(value)) must beEqualTo(value).await
      orElse mustEqual 0
    }

    "get or else (miss)" in new MockedCache with OrElse {
      connector.get[String](anyString)(anyClassTag) returns None
      connector.set(anyString, anyString, any[Duration], beEq(false)) returns true
      cache.getOrElse(key)(doElse(value)) must beEqualTo(value).await
      orElse mustEqual 1
    }

    "get or else (failure)" in new MockedCache with OrElse {
      connector.get[String](anyString)(anyClassTag) returns ex
      cache.getOrElse(key)(doElse(value)) must beEqualTo(value).await
      orElse mustEqual 1
    }

    "get or else (prefixed,miss)" in new MockedSyncRedis with OrElse {
      runtime.prefix returns new RedisPrefixImpl("prefix")
      connector.get[String](beEq(s"prefix:$key"))(anyClassTag) returns None
      connector.set(beEq(s"prefix:$key"), anyString, any[Duration], anyBoolean) returns true
      cache.getOrElse(key)(doElse(value)) must beEqualTo(value)
      orElse mustEqual 1
    }

    "get or future (hit)" in new MockedCache with OrElse {
      connector.get[String](anyString)(anyClassTag) returns Some(value)
      cache.getOrFuture(key)(doFuture(value)) must beEqualTo(value).await
      orElse mustEqual 0
    }

    "get or future (miss)" in new MockedCache with OrElse {
      connector.get[String](anyString)(anyClassTag) returns None
      connector.set(anyString, anyString, any[Duration], beEq(false)) returns true
      cache.getOrFuture(key)(doFuture(value)) must beEqualTo(value).await
      orElse mustEqual 1
    }

    "get or future (failure)" in new MockedCache with OrElse {
      connector.get[String](anyString)(anyClassTag) returns ex
      cache.getOrFuture(key)(doFuture(value)) must beEqualTo(value).await
      orElse mustEqual 1
    }

    "get or future (failing orElse)" in new MockedCache with OrElse {
      connector.get[String](anyString)(anyClassTag) returns None
      cache.getOrFuture(key)(failedFuture) must throwA[TimeoutException].await
      orElse mustEqual 2
    }

    "get or future (rerun)" in new MockedCache with OrElse with Attempts {
      override protected def policy = new RecoveryPolicy {
        def recoverFrom[T](rerun: => Future[T], default: => Future[T], failure: RedisException) = rerun
      }
      connector.get[String](anyString)(anyClassTag) returns None
      connector.set(anyString, anyString, any[Duration], beEq(false)) returns true
      // run the test
      cache.getOrFuture(key) {
        attempts match {
          case 0 => attempt(failedFuture)
          case _ => attempt(doFuture(value))
        }
      } must beEqualTo(value).await
      // verification
      orElse mustEqual 2
      there were two(connector).get[String](anyString)(anyClassTag)
      there was one(connector).set(key, value, Duration.Inf, ifNotExists = false)
    }

    "remove" in new MockedCache {
      connector.remove(anyVarArgs) returns unit
      cache.remove(key) must beDone.await
    }

    "remove recover with default" in new MockedCache {
      connector.remove(anyVarArgs) returns ex
      cache.remove(key) must beDone.await
    }

    "remove multiple" in new MockedCache {
      connector.remove(anyVarArgs) returns unit
      cache.remove(key, key, key, key) must beDone.await
    }

    "remove multiple recover with default" in new MockedCache {
      connector.remove(anyVarArgs) returns ex
      cache.remove(key, key, key, key) must beDone.await
    }

    "remove all" in new MockedCache {
      connector.remove(anyVarArgs) returns unit
      cache.removeAll(Seq(key, key, key, key): _*) must beDone.await
    }

    "remove all recover with default" in new MockedCache {
      connector.remove(anyVarArgs) returns ex
      cache.removeAll(Seq(key, key, key, key): _*) must beDone.await
    }

    "remove matching" in new MockedCache {
      connector.matching(beEq("pattern")) returns Seq(key, key)
      connector.remove(key, key) returns unit
      cache.removeMatching("pattern") must beDone.await
    }

    "remove matching recover with default" in new MockedCache {
      connector.matching(anyVarArgs) returns ex
      cache.removeMatching("pattern") must beDone.await
    }

    "invalidate" in new MockedCache {
      connector.invalidate() returns unit
      cache.invalidate() must beDone.await
    }

    "invalidate recover with default" in new MockedCache {
      connector.invalidate() returns ex
      cache.invalidate() must beDone.await
    }

    "exists" in new MockedCache {
      connector.exists(key) returns true
      cache.exists(key) must beTrue.await
    }

    "exists recover with default" in new MockedCache {
      connector.exists(key) returns ex
      cache.exists(key) must beFalse.await
    }

    "increment" in new MockedCache {
      connector.increment(key, 5L) returns 10L
      cache.increment(key, 5L) must beEqualTo(10L).await
    }

    "increment recover with default" in new MockedCache {
      connector.increment(key, 5L) returns ex
      cache.increment(key, 5L) must beEqualTo(5L).await
    }

    "decrement" in new MockedCache {
      connector.increment(key, -5L) returns 10L
      cache.decrement(key, 5L) must beEqualTo(10L).await
    }

    "decrement recover with default" in new MockedCache {
      connector.increment(key, -5L) returns ex
      cache.decrement(key, 5L) must beEqualTo(-5L).await
    }
  }
}
