package play.api.cache.redis.impl

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RedisPrefixSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {

  import Implicits._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "RedisPrefix" should {

    "apply when defined" in new MockedCache {
      runtime.prefix returns new RedisPrefixImpl("prefix")
      connector.get[String](anyString)(anyClassTag) returns None
      connector.mGet[String](anyVarArgs)(anyClassTag) returns Seq(None, Some(value))
      // run the test
      cache.get[String](key) must beNone.await
      cache.getAll[String](key, other) must beEqualTo(Seq(None, Some(value))).await
      there were one(connector).get[String](s"prefix:$key")
      there were one(connector).mGet[String](s"prefix:$key", s"prefix:$other")
    }

    "not apply when is empty" in new MockedCache {
      runtime.prefix returns RedisEmptyPrefix
      connector.get[String](anyString)(anyClassTag) returns None
      // run the test
      cache.get[String](key) must beNone.await
      there were one(connector).get[String](key)
    }
  }
}
