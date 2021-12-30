package play.api.cache.redis.impl

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import play.api.cache.redis._

import scala.reflect.ClassTag

class RedisSortedSetSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import RedisCacheImplicits._
  import org.mockito.ArgumentMatchers._

  "Redis Set" should {

    "add" in new MockedSortedSet {
      connector.sortedSetAdd(anyString, anyVarArgs) returns 5L
      set.add(scoreValue) must beEqualTo(set).await
      set.add(scoreValue, otherScoreValue) must beEqualTo(set).await
      there were one(connector).sortedSetAdd(key, scoreValue)
      there were one(connector).sortedSetAdd(key, scoreValue, otherScoreValue)
    }

    "add (failing)" in new MockedSortedSet {
      connector.sortedSetAdd(anyString, anyVarArgs) returns ex
      set.add(scoreValue) must beEqualTo(set).await
      there were one(connector).sortedSetAdd(key, scoreValue)
    }

    "contains (hit)" in new MockedSortedSet {
      connector.sortedSetScore(beEq(key), beEq(other)) returns Some(1D)
      set.contains(other) must beTrue.await
      there was one(connector).sortedSetScore(key, other)
    }

    "contains (miss)" in new MockedSortedSet {
      connector.sortedSetScore(beEq(key), beEq(other)) returns None
      set.contains(other) must beFalse.await
      there was one(connector).sortedSetScore(key, other)
    }

    "remove" in new MockedSortedSet {
      connector.sortedSetRemove(anyString, anyVarArgs) returns 1L
      set.remove(value) must beEqualTo(set).await
      set.remove(other, value) must beEqualTo(set).await
      there were one(connector).sortedSetRemove(key, value)
      there were one(connector).sortedSetRemove(key, other, value)
    }

    "remove (failing)" in new MockedSortedSet {
      connector.sortedSetRemove(anyString, anyVarArgs) returns ex
      set.remove(value) must beEqualTo(set).await
      there was one(connector).sortedSetRemove(key, value)
    }

    "range" in new MockedSortedSet {
      val data = Seq(value, other)
      connector.sortedSetRange[String](anyString, anyLong, anyLong)(anyClassTag) returns data
      set.range(1, 5) must beEqualTo(data).await
      there was one(connector).sortedSetRange(key, 1, 5)(implicitly[ClassTag[String]])
    }

    "range (reversed)" in new MockedSortedSet {
      val data = Seq(value, other)
      connector.sortedSetReverseRange[String](anyString, anyLong, anyLong)(anyClassTag) returns data
      set.range(1, 5, isReverse = true) must beEqualTo(data).await
      there was one(connector).sortedSetReverseRange(key, 1, 5)(implicitly[ClassTag[String]])
    }

    "size" in new MockedSortedSet {
      connector.sortedSetSize(key) returns 2L
      set.size must beEqualTo(2L).await
    }

    "size (failing)" in new MockedSortedSet {
      connector.sortedSetSize(key) returns ex
      set.size must beEqualTo(0L).await
    }

    "empty set" in new MockedSortedSet {
      connector.sortedSetSize(beEq(key)) returns 0L
      set.isEmpty must beTrue.await
      set.nonEmpty must beFalse.await
    }

    "non-empty set" in new MockedSortedSet {
      connector.sortedSetSize(beEq(key)) returns 1L
      set.isEmpty must beFalse.await
      set.nonEmpty must beTrue.await
    }

    "empty/non-empty set (failing)" in new MockedSortedSet {
      connector.sortedSetSize(beEq(key)) returns ex
      set.isEmpty must beTrue.await
      set.nonEmpty must beFalse.await
    }
  }
}
