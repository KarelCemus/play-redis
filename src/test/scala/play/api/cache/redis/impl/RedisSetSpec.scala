package play.api.cache.redis.impl

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RedisSetSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "Redis Set" should {

    "add" in new MockedSet {
      connector.setAdd(anyString, anyVarArgs) returns 5L
      set.add(value) must beEqualTo(set).await
      set.add(value, other) must beEqualTo(set).await
      there were one(connector).setAdd(key, value)
      there were one(connector).setAdd(key, value, other)
    }

    "add (failing)" in new MockedSet {
      connector.setAdd(anyString, anyVarArgs) returns ex
      set.add(value) must beEqualTo(set).await
      there were one(connector).setAdd(key, value)
    }

    "contains" in new MockedSet {
      connector.setIsMember(anyString, beEq(value)) returns true
      connector.setIsMember(anyString, beEq(other)) returns false
      set.contains(value) must beTrue.await
      set.contains(other) must beFalse.await
    }

    "contains (failing)" in new MockedSet {
      connector.setIsMember(anyString, anyString) returns ex
      set.contains(value) must beFalse.await
      there were one(connector).setIsMember(key, value)
    }

    "remove" in new MockedSet {
      connector.setRemove(anyString, anyVarArgs) returns 1L
      set.remove(value) must beEqualTo(set).await
      set.remove(other, value) must beEqualTo(set).await
      there were one(connector).setRemove(key, value)
      there were one(connector).setRemove(key, other, value)
    }

    "remove (failing)" in new MockedSet {
      connector.setRemove(anyString, anyVarArgs) returns ex
      set.remove(value) must beEqualTo(set).await
      there were one(connector).setRemove(key, value)
    }

    "toSet" in new MockedSet {
      connector.setMembers[String](anyString)(anyClassTag) returns (data.toSet: Set[String])
      set.toSet must beEqualTo(data).await
    }

    "toSet (failing)" in new MockedSet {
      connector.setMembers[String](anyString)(anyClassTag) returns ex
      set.toSet must beEqualTo(Set.empty).await
    }

    "size" in new MockedSet {
      connector.setSize(key) returns 2L
      set.size must beEqualTo(2L).await
    }

    "size (failing)" in new MockedSet {
      connector.setSize(key) returns ex
      set.size must beEqualTo(0L).await
    }

    "empty set" in new MockedSet {
      connector.setSize(beEq(key)) returns 0L
      set.isEmpty must beTrue.await
      set.nonEmpty must beFalse.await
    }

    "non-empty set" in new MockedSet {
      connector.setSize(beEq(key)) returns 1L
      set.isEmpty must beFalse.await
      set.nonEmpty must beTrue.await
    }

    "empty/non-empty set (failing)" in new MockedSet {
      connector.setSize(beEq(key)) returns ex
      set.isEmpty must beTrue.await
      set.nonEmpty must beFalse.await
    }
  }
}
