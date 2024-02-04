package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.test._
import play.cache.redis.AsyncRedisList

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class RedisJavaListSpec extends AsyncUnitSpec with RedisListJavaMock with RedisRuntimeMock {

  test("prepend") { (list, internal) =>
    for {
      _ <- internal.expect.prepend(cacheValue)
      _ <- list.prepend(cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("append") { (list, internal) =>
    for {
      _ <- internal.expect.append(cacheValue)
      _ <- list.append(cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("apply (hit)") { (list, internal) =>
    for {
      _ <- internal.expect.apply(5, Some(cacheValue))
      _ <- list.apply(5).assertingEqual(cacheValue)
    } yield Passed
  }

  test("apply (miss or fail)") { (list, internal) =>
    for {
      _ <- internal.expect.apply(5, None)
      _ <- list.apply(5).assertingFailure[NoSuchElementException]
    } yield Passed
  }

  test("get (miss)") { (list, internal) =>
    for {
      _ <- internal.expect.get(5, None)
      _ <- list.get(5).assertingEqual(None.toJava)
    } yield Passed
  }

  test("get (hit)") { (list, internal) =>
    for {
      _ <- internal.expect.get(5, Some(cacheValue))
      _ <- list.get(5).assertingEqual(Some(cacheValue).toJava)
    } yield Passed
  }

  test("head (non-empty)") { (list, internal) =>
    for {
      _ <- internal.expect.apply(0, Some(cacheValue))
      _ <- list.head.assertingEqual(cacheValue)
    } yield Passed
  }

  test("head (empty)") { (list, internal) =>
    for {
      _ <- internal.expect.apply(0, None)
      _ <- list.head.assertingFailure[NoSuchElementException]
    } yield Passed
  }

  test("headOption (non-empty)") { (list, internal) =>
    for {
      _ <- internal.expect.get(0, Some(cacheValue))
      _ <- list.headOption().assertingEqual(Some(cacheValue).toJava)
    } yield Passed
  }

  test("headOption (empty)") { (list, internal) =>
    for {
      _ <- internal.expect.get(0, None)
      _ <- list.headOption().assertingEqual(None.toJava)
    } yield Passed
  }

  test("head pop") { (list, internal) =>
    for {
      _ <- internal.expect.headPop(None)
      _ <- list.headPop().assertingEqual(None.toJava)
    } yield Passed
  }

  test("last (non-empty)") { (list, internal) =>
    for {
      _ <- internal.expect.apply(-1, Some(cacheValue))
      _ <- list.last().assertingEqual(cacheValue)
    } yield Passed
  }

  test("last (empty)") { (list, internal) =>
    for {
      _ <- internal.expect.apply(-1, None)
      _ <- list.last().assertingFailure[NoSuchElementException]
    } yield Passed
  }

  test("lastOption (non-empty)") { (list, internal) =>
    for {
      _ <- internal.expect.get(-1, Some(cacheValue))
      _ <- list.lastOption().assertingEqual(Some(cacheValue).toJava)
    } yield Passed
  }

  test("lastOption (empty)") { (list, internal) =>
    for {
      _ <- internal.expect.get(-1, None)
      _ <- list.lastOption().assertingEqual(None.toJava)
    } yield Passed
  }

  test("toList") { (list, internal) =>
    for {
      _ <- internal.expect.view.slice(0, -1, List.empty)
      _ <- list.toList.assertingEqual(List.empty.asJava)
    } yield Passed
  }

  test("insert before") { (list, internal) =>
    for {
      _ <- internal.expect.insertBefore("pivot", cacheValue, Some(5L))
      _ <- list.insertBefore("pivot", cacheValue).assertingEqual(Option(5L).map(long2Long).toJava)
    } yield Passed
  }

  test("set at position") { (list, internal) =>
    for {
      _ <- internal.expect.set(2, cacheValue)
      _ <- list.set(2, cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("remove element") { (list, internal) =>
    for {
      _ <- internal.expect.remove(cacheValue)
      _ <- list.remove(cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("remove with count") { (list, internal) =>
    for {
      _ <- internal.expect.remove(cacheValue, 2)
      _ <- list.remove(cacheValue, 2).assertingEqual(list)
    } yield Passed
  }

  test("remove at position") { (list, internal) =>
    for {
      _ <- internal.expect.removeAt(2)
      _ <- list.removeAt(2).assertingEqual(list)
    } yield Passed
  }

  test("view all") { (list, internal) =>
    for {
      _ <- internal.expect.view.slice(0, -1, List.empty)
      _ <- list.view().all().assertingEqual(List.empty.asJava)
    } yield Passed
  }

  test("view take") { (list, internal) =>
    for {
      _ <- internal.expect.view.slice(0, 1, List.empty)
      _ <- list.view().take(2).assertingEqual(List.empty.asJava)
    } yield Passed
  }

  test("view drop") { (list, internal) =>
    for {
      _ <- internal.expect.view.slice(2, -1, List.empty)
      _ <- list.view().drop(2).assertingEqual(List.empty.asJava)
    } yield Passed
  }

  test("view slice") { (list, internal) =>
    for {
      _ <- internal.expect.view.slice(1, 2, List.empty)
      _ <- list.view().slice(1, 2).assertingEqual(List.empty.asJava)
    } yield Passed
  }

  test("modify clear") { (list, internal) =>
    for {
      _ <- internal.expect.modify.clear()
      _ <- list.modify().clear().assertingEqual(list.modify())
    } yield Passed
  }

  test("modify slice") { (list, internal) =>
    for {
      _ <- internal.expect.modify.slice(1, 2)
      _ <- list.modify().slice(1, 2).assertingEqual(list.modify())
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default,
  )(
    f: (AsyncRedisList[String], RedisListMock) => Future[Assertion],
  ): Unit =
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      val internal: RedisListMock = mock[RedisListMock]
      val view: internal.RedisListView = mock[internal.RedisListView]
      val modifier: internal.RedisListModification = mock[internal.RedisListModification]
      val list: AsyncRedisList[String] = new RedisListJavaImpl(internal)

      (() => internal.view).expects().returns(view).anyNumberOfTimes()
      (() => internal.modify).expects().returns(modifier).anyNumberOfTimes()

      f(list, internal)
    }

}
