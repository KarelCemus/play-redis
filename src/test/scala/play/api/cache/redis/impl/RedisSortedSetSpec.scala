package play.api.cache.redis.impl

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import play.api.cache.redis._

class RedisSortedSetSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import RedisCacheImplicits._
  import org.mockito.ArgumentMatchers._

  "Redis Set" should {

    "add" in new MockedSortedSet {
      connector.zsetAdd(anyString, anyVarArgs) returns 5L
      set.add(scoreValue) must beEqualTo(set).await
      set.add(scoreValue, otherScoreValue) must beEqualTo(set).await
      there were one(connector).zsetAdd(key, scoreValue)
      there were one(connector).zsetAdd(key, scoreValue, otherScoreValue)
    }

    "add (failing)" in new MockedSortedSet {
      connector.zsetAdd(anyString, anyVarArgs) returns ex
      set.add(scoreValue) must beEqualTo(set).await
      there were one(connector).zsetAdd(key, scoreValue)
    }

    "remove" in new MockedSortedSet {
      connector.zsetRemove(anyString, anyVarArgs) returns 1L
      set.remove(value) must beEqualTo(set).await
      set.remove(other, value) must beEqualTo(set).await
      there were one(connector).zsetRemove(key, value)
      there were one(connector).zsetRemove(key, other, value)
    }

    "remove (failing)" in new MockedSortedSet {
      connector.zsetRemove(anyString, anyVarArgs) returns ex
      set.remove(value) must beEqualTo(set).await
      there were one(connector).zsetRemove(key, value)
    }

    "size" in new MockedSortedSet {
      connector.zsetSize(key) returns 2L
      set.size must beEqualTo(2L).await
    }

    "size (failing)" in new MockedSortedSet {
      connector.zsetSize(key) returns ex
      set.size must beEqualTo(0L).await
    }

    "empty set" in new MockedSortedSet {
      connector.zsetSize(beEq(key)) returns 0L
      set.isEmpty must beTrue.await
      set.nonEmpty must beFalse.await
    }

    "non-empty set" in new MockedSortedSet {
      connector.zsetSize(beEq(key)) returns 1L
      set.isEmpty must beFalse.await
      set.nonEmpty must beTrue.await
    }

    "empty/non-empty set (failing)" in new MockedSortedSet {
      connector.zsetSize(beEq(key)) returns ex
      set.isEmpty must beTrue.await
      set.nonEmpty must beFalse.await
    }
  }
}
