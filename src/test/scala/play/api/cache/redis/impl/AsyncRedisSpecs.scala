package play.api.cache.redis.impl

import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class AsyncRedisSpecs(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "AsyncRedis" should {

    "removeAll" in new MockedAsyncRedis {
      connector.invalidate() returns unit
      cache.removeAll() must beDone.await
      there was one(connector).invalidate()
    }

    "getOrElseUpdate (hit)" in new MockedAsyncRedis with OrElse {
      connector.get[String](anyString)(anyClassTag) returns Some(value)
      cache.getOrElseUpdate(key)(doFuture(value)) must beEqualTo(value).await
      orElse mustEqual 0
    }

    "getOrElseUpdate (miss)" in new MockedAsyncRedis with OrElse {
      connector.get[String](anyString)(anyClassTag) returns None
      connector.set(anyString, anyString, any[Duration], anyBoolean) returns true
      cache.getOrElseUpdate(key)(doFuture(value)) must beEqualTo(value).await
      orElse mustEqual 1
    }

    "getOrElseUpdate (failure)" in new MockedAsyncRedis with OrElse {
      connector.get[String](anyString)(anyClassTag) returns ex
      cache.getOrElseUpdate(key)(doFuture(value)) must beEqualTo(value).await
      orElse mustEqual 1
    }

    "getOrElseUpdate (failing orElse)" in new MockedAsyncRedis with OrElse {
      connector.get[String](anyString)(anyClassTag) returns None
      cache.getOrElseUpdate(key)(failedFuture) must throwA[TimeoutException].await
      orElse mustEqual 2
    }

    "getOrElseUpdate (rerun)" in new MockedAsyncRedis with OrElse with Attempts {
      override protected def policy = new RecoveryPolicy {
        def recoverFrom[T](rerun: => Future[T], default: => Future[T], failure: RedisException) = rerun
      }
      connector.get[String](anyString)(anyClassTag) returns None
      connector.set(anyString, anyString, any[Duration], anyBoolean) returns true
      // run the test
      cache.getOrElseUpdate(key) {
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
  }
}
