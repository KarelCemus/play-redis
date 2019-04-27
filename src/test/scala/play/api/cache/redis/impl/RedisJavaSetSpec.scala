package play.api.cache.redis.impl

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RedisJavaSetSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import JavaCompatibility._
  import RedisCacheImplicits._

  "Redis Set" should {

    "add" in new MockedJavaSet {
      internal.add(anyVarArgs[String]) returns internal
      set.add(key, value).asScala must beEqualTo(set).await
      there were one(internal).add(key, value)
    }

    "contains" in new MockedJavaSet {
      internal.contains(beEq(key)) returns true
      set.contains(key).asScala.map(Boolean.unbox) must beTrue.await
      there were one(internal).contains(key)
    }

    "remove" in new MockedJavaSet {
      internal.remove(anyVarArgs[String]) returns internal
      set.remove(key, value).asScala must beEqualTo(set).await
      there were one(internal).remove(key, value)
    }

    "toSet" in new MockedJavaSet {
      internal.toSet returns Set(key, value)
      set.toSet.asScala must beEqualTo(Set(key, value).asJava).await
      there were one(internal).toSet
    }
  }
}
