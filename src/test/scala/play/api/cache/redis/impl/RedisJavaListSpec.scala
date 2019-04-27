package play.api.cache.redis.impl

import play.api.cache.redis._

import org.mockito.ArgumentMatchers
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RedisJavaListSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import JavaCompatibility._
  import RedisCacheImplicits._

  import ArgumentMatchers._

  "Redis List" should {

    "prepend" in new MockedJavaList {
      internal.prepend(value) returns internal
      list.prepend(value).asScala must beEqualTo(list).await
      there were one(internal).prepend(value)
    }

    "append" in new MockedJavaList {
      internal.append(value) returns internal
      list.append(value).asScala must beEqualTo(list).await
      there were one(internal).append(value)
    }

    "apply (hit)" in new MockedJavaList {
      internal.apply(beEq(5)) returns value
      list.apply(5).asScala must beEqualTo(value).await
      there were one(internal).apply(5)
    }

    "apply (miss or fail)" in new MockedJavaList {
      internal.apply(beEq(5)) returns NoSuchElementException
      list.apply(5).asScala must throwA[NoSuchElementException].await
      there were one(internal).apply(5)
    }

    "get (miss)" in new MockedJavaList {
      internal.get(beEq(5)) returns None
      list.get(5).asScala must beEqualTo(None.asJava).await
      there were one(internal).get(5)
    }

    "get (hit)" in new MockedJavaList {
      internal.get(beEq(5)) returns Some(value)
      list.get(5).asScala must beEqualTo(Some(value).asJava).await
      there were one(internal).get(5)
    }

    "head (non-empty)" in new MockedJavaList {
      internal.apply(beEq(0)) returns value
      list.head.asScala must beEqualTo(value).await
      there were one(internal).apply(0)
    }

    "head (empty)" in new MockedJavaList {
      internal.apply(beEq(0)) returns NoSuchElementException
      list.head.asScala must throwA(NoSuchElementException).await
      there were one(internal).apply(0)
    }

    "headOption (non-empty)" in new MockedJavaList {
      internal.get(beEq(0)) returns Some(value)
      list.headOption.asScala must beEqualTo(Some(value).asJava).await
      there were one(internal).get(0)
    }

    "headOption (empty)" in new MockedJavaList {
      internal.get(beEq(0)) returns None
      list.headOption.asScala must beEqualTo(None.asJava).await
      there were one(internal).get(0)
    }

    "head pop" in new MockedJavaList {
      internal.headPop returns None
      list.headPop().asScala must beEqualTo(None.asJava).await
      there were one(internal).headPop
    }

    "last (non-empty)" in new MockedJavaList {
      internal.apply(beEq(-1)) returns value
      list.last.asScala must beEqualTo(value).await
      there were one(internal).apply(-1)
    }

    "last (empty)" in new MockedJavaList {
      internal.apply(beEq(-1)) returns NoSuchElementException
      list.last.asScala must throwA(NoSuchElementException).await
      there were one(internal).apply(-1)
    }

    "lastOption (non-empty)" in new MockedJavaList {
      internal.get(beEq(-1)) returns Some(value)
      list.lastOption.asScala must beEqualTo(Some(value).asJava).await
      there were one(internal).get(-1)
    }

    "lastOption (empty)" in new MockedJavaList {
      internal.get(beEq(-1)) returns None
      list.lastOption.asScala must beEqualTo(None.asJava).await
      there were one(internal).get(-1)
    }

    "toList" in new MockedJavaList {
      view.slice(beEq(0), beEq(-1)) returns List.empty[String]
      list.toList.asScala must beEqualTo(List.empty.asJava).await
      there were one(internal).view
      there were one(view).slice(0, -1)
    }

    "insert before" in new MockedJavaList {
      internal.insertBefore("pivot", value) returns Some(5L)
      list.insertBefore("pivot", value).asScala must beEqualTo(Some(5L).asJava).await
      there were one(internal).insertBefore("pivot", value)
    }

    "set at position" in new MockedJavaList {
      internal.set(beEq(2), beEq(value)) returns internal
      list.set(2, value).asScala must beEqualTo(list).await
      there were one(internal).set(2, value)
    }

    "remove element" in new MockedJavaList {
      internal.remove(beEq(value), anyInt) returns internal
      list.remove(value).asScala must beEqualTo(list).await
      there were one(internal).remove(value)
    }

    "remove with count" in new MockedJavaList {
      internal.remove(beEq(value), beEq(2)) returns internal
      list.remove(value, 2).asScala must beEqualTo(list).await
      there were one(internal).remove(value, 2)
    }

    "remove at position" in new MockedJavaList {
      internal.removeAt(beEq(2)) returns internal
      list.removeAt(2).asScala must beEqualTo(list).await
      there were one(internal).removeAt(2)
    }

    "view all" in new MockedJavaList {
      view.slice(beEq(0), beEq(-1)) returns List.empty[String]
      list.view().all().asScala must beEqualTo(List.empty.asJava).await
      there were one(internal).view
      there were one(view).slice(0, -1)
    }

    "view take" in new MockedJavaList {
      view.slice(beEq(0), beEq(1)) returns List.empty[String]
      list.view().take(2).asScala must beEqualTo(List.empty.asJava).await
      there were one(internal).view
      there were one(view).slice(0, 1)
    }

    "view drop" in new MockedJavaList {
      view.slice(beEq(2), beEq(-1)) returns List.empty[String]
      list.view().drop(2).asScala must beEqualTo(List.empty.asJava).await
      there were one(internal).view
      there were one(view).slice(2, -1)
    }

    "view slice" in new MockedJavaList {
      view.slice(beEq(1), beEq(2)) returns List.empty[String]
      list.view().slice(1, 2).asScala must beEqualTo(List.empty.asJava).await
      there were one(internal).view
      there were one(view).slice(1, 2)
    }

    "modify collection" in new MockedJavaList {
      list.modify().collection() mustEqual list
    }

    "modify clear" in new MockedJavaList {
      private val javaModifier = list.modify()
      modifier.clear() returns modifier
      javaModifier.clear().asScala must beEqualTo(javaModifier).await
      there were one(internal).modify
      there were one(modifier).clear()
    }

    "modify slice" in new MockedJavaList {
      private val javaModifier = list.modify()
      modifier.slice(beEq(1), beEq(2)) returns modifier
      javaModifier.slice(1, 2).asScala must beEqualTo(javaModifier).await
      there were one(internal).modify
      there were one(modifier).slice(1, 2)
    }
  }
}
