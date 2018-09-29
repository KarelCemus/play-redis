package play.api.cache.redis.impl

import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RedisListSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._
  import org.mockito._

  val expiration = 1.second

  "Redis List" should {

    "prepend (all variants)" in new MockedList {
      connector.listPrepend(key, value) returns 5L
      connector.listPrepend(key, value, value) returns 10L
      // verify
      list.prepend(value) must beEqualTo(list).await
      List(value, value) ++: list must beEqualTo(list).await
      value +: list must beEqualTo(list).await

      there were two(connector).listPrepend(key, value)
      there were one(connector).listPrepend(key, value, value)
    }

    "prepend (failing)" in new MockedList {
      connector.listPrepend(key, value) returns ex
      // verify
      list.prepend(value) must beEqualTo(list).await

      there were one(connector).listPrepend(key, value)
    }

    "append (all variants)" in new MockedList {
      connector.listAppend(key, value) returns 5L
      connector.listAppend(key, value, value) returns 10L
      // verify
      list.append(value) must beEqualTo(list).await
      list :+ value must beEqualTo(list).await
      list :++ Seq(value, value) must beEqualTo(list).await

      there were two(connector).listAppend(key, value)
      there were one(connector).listAppend(key, value, value)
    }

    "append (failing)" in new MockedList {
      connector.listAppend(key, value) returns ex
      // verify
      list.append(value) must beEqualTo(list).await

      there were one(connector).listAppend(key, value)
    }

    "get (miss)" in new MockedList {
      connector.listSlice[String](beEq(key), anyInt, anyInt)(anyClassTag) returns Seq(value)
      list.get(5) must beSome(value).await
    }

    "get (hit)" in new MockedList {
      connector.listSlice[String](beEq(key), anyInt, anyInt)(anyClassTag) returns Seq.empty[String]
      list.get(5) must beNone.await
    }

    "get (failure)" in new MockedList {
      connector.listSlice[String](beEq(key), anyInt, anyInt)(anyClassTag) returns ex
      list.get(5) must beNone.await
    }

    "apply (hit)" in new MockedList {
      connector.listSlice[String](beEq(key), anyInt, anyInt)(anyClassTag) returns Seq(value)
      list(5) must beEqualTo(value).await
    }

    "apply (miss)" in new MockedList {
      connector.listSlice[String](beEq(key), anyInt, anyInt)(anyClassTag) returns Seq.empty[String]
      list(5) must throwA[NoSuchElementException].await
    }

    "apply (failing)" in new MockedList {
      connector.listSlice[String](beEq(key), anyInt, anyInt)(anyClassTag) returns ex
      list(5) must throwA[NoSuchElementException].await
    }

    "head pop" in new MockedList {
      connector.listHeadPop[String](beEq(key))(anyClassTag) returns None
      list.headPop must beNone.await
    }

    "head pop (failing)" in new MockedList {
      connector.listHeadPop[String](beEq(key))(anyClassTag) returns ex
      list.headPop must beNone.await
    }

    "size" in new MockedList {
      connector.listSize(key) returns 2L
      list.size must beEqualTo(2L).await
    }

    "size (failing)" in new MockedList {
      connector.listSize(key) returns ex
      list.size must beEqualTo(0L).await
    }

    "insert before" in new MockedList {
      connector.listInsert(key, "pivot", value) returns Some(5L)
      list.insertBefore("pivot", value) must beSome(5L).await
      there were one(connector).listInsert(key, "pivot", value)
    }

    "insert before (failing)" in new MockedList {
      connector.listInsert(key, "pivot", value) returns ex
      list.insertBefore("pivot", value) must beNone.await
      there were one(connector).listInsert(key, "pivot", value)
    }

    "set at position" in new MockedList {
      connector.listSetAt(beEq(key), anyInt, beEq(value)) returns unit
      list.set(5, value) must beEqualTo(list).await
      there were one(connector).listSetAt(key, 5, value)
    }

    "set at position (failing)" in new MockedList {
      connector.listSetAt(beEq(key), anyInt, beEq(value)) returns ex
      list.set(5, value) must beEqualTo(list).await
      there were one(connector).listSetAt(key, 5, value)
    }

    "empty list" in new MockedList {
      connector.listSize(beEq(key)) returns 0L
      list.isEmpty must beTrue.await
      list.nonEmpty must beFalse.await
    }

    "non-empty list" in new MockedList {
      connector.listSize(beEq(key)) returns 1L
      list.isEmpty must beFalse.await
      list.nonEmpty must beTrue.await
    }

    "empty/non-empty list (failing)" in new MockedList {
      connector.listSize(beEq(key)) returns ex
      list.isEmpty must beTrue.await
      list.nonEmpty must beFalse.await
    }

    "remove element" in new MockedList {
      connector.listRemove(anyString, anyString, anyInt) returns 1L
      list.remove(value) must beEqualTo(list).await
      there were one(connector).listRemove(key, value, 1)
    }

    "remove element (failing)" in new MockedList {
      connector.listRemove(anyString, anyString, anyInt) returns ex
      list.remove(value) must beEqualTo(list).await
      there were one(connector).listRemove(key, value, 1)
    }

    "remove at position" in new MockedList {
      Mockito.when(connector.listSetAt(anyString, anyInt, anyString)).thenAnswer {
        AdditionalAnswers.answer {
          new stubbing.Answer3[Future[Unit], String, Int, String] {
            def answer(key: String, position: Int, value: String) = {
              data(position) = value
              unit
            }
          }
        }
      }

      Mockito.when(connector.listRemove(anyString, anyString, anyInt)).thenAnswer {
        AdditionalAnswers.answer {
          new stubbing.Answer3[Future[Long], String, String, Int] {
            def answer(key: String, value: String, count: Int) = {
              val index = data.indexOf(value)
              if (index > -1) data.remove(index, 1)
              if (index > -1) 1L else 0L
            }
          }
        }
      }

      list.removeAt(0) must beEqualTo(list).await
      data mustEqual Seq(value, value)

      list.removeAt(1) must beEqualTo(list).await
      data mustEqual Seq(value)
    }

    "remove at position (failing)" in new MockedList {
      connector.listSetAt(anyString, anyInt, anyString) returns ex
      list.removeAt(0) must beEqualTo(list).await
      there were one(connector).listSetAt(key, 0, "play-redis:DELETED")
    }

    "view all" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns data
      list.view.all must beEqualTo(data).await
      there were one(connector).listSlice[String](key, 0, -1)
    }

    "view take" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns data
      list.view.take(2) must beEqualTo(data).await
      there were one(connector).listSlice[String](key, 0, 1)
    }

    "view drop" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns data
      list.view.drop(2) must beEqualTo(data).await
      there were one(connector).listSlice[String](key, 2, -1)
    }

    "view slice" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns data
      list.view.slice(1, 2) must beEqualTo(data).await
      there were one(connector).listSlice[String](key, 1, 2)
    }

    "view slice (failing)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns ex
      list.view.slice(1, 2) must beEqualTo(Seq.empty).await
    }

    "head (non-empty)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns data.headOption.toSeq
      list.head must beEqualTo(data.head).await
      list.headOption must beSome(data.head).await
      there were two(connector).listSlice[String](key, 0, 0)
    }

    "head (empty)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns Seq.empty[String]
      list.head must throwA[NoSuchElementException].await
      list.headOption must beNone.await
      there were two(connector).listSlice[String](key, 0, 0)
    }

    "head (failing)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns ex
      list.head must throwA[NoSuchElementException].await
      list.headOption must beNone.await
    }

    "last (non-empty)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns data.headOption.toSeq
      list.last must beEqualTo(data.head).await
      list.lastOption must beSome(data.head).await
      there were two(connector).listSlice[String](key, -1, -1)
    }

    "last (empty)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns Seq.empty[String]
      list.last must throwA[NoSuchElementException].await
      list.lastOption must beNone.await
      there were two(connector).listSlice[String](key, -1, -1)
    }

    "last (failing)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns ex
      list.head must throwA[NoSuchElementException].await
      list.headOption must beNone.await
    }

    "toList" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns data
      list.toList must beEqualTo(data).await
      there were one(connector).listSlice[String](key, 0, -1)
    }

    "toList (failing)" in new MockedList {
      connector.listSlice[String](anyString, anyInt, anyInt)(anyClassTag) returns ex
      list.toList must beEqualTo(Seq.empty).await
      there were one(connector).listSlice[String](key, 0, -1)
    }

    "modify collection" in new MockedList {
      list.modify.collection mustEqual list
    }

    "modify take" in new MockedList {
      connector.listTrim(anyString, anyInt, anyInt) returns unit
      list.modify.take(2) must not(throwA[Throwable]).await
      there were one(connector).listTrim(key, 0, 1)
    }

    "modify drop" in new MockedList {
      connector.listTrim(anyString, anyInt, anyInt) returns unit
      list.modify.drop(2) must not(throwA[Throwable]).await
      there were one(connector).listTrim(key, 2, -1)
    }

    "modify clear" in new MockedList {
      connector.remove(anyVarArgs) returns unit
      list.modify.clear() must not(throwA[Throwable]).await
      there were one(connector).remove(key)
    }

    "modify slice" in new MockedList {
      connector.listTrim(anyString, anyInt, anyInt) returns unit
      list.modify.slice(1, 2) must not(throwA[Throwable]).await
      there were one(connector).listTrim(key, 1, 2)
    }
  }
}
