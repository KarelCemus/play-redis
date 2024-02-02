package play.api.cache.redis.impl

import play.api.cache.redis._
import play.api.cache.redis.impl.Builders.AsynchronousBuilder
import play.api.cache.redis.test._

import scala.concurrent.Future

class RedisListSpec extends AsyncUnitSpec with RedisRuntimeMock with RedisConnectorMock with ImplicitFutureMaterialization {

  test("prepend (all variants)") { (list, connector) =>
    for {
      _ <- connector.expect.listPrepend(otherKey, Seq(cacheValue))
      _ <- connector.expect.listPrepend(otherKey, Seq(cacheValue))
      _ <- connector.expect.listPrepend(otherKey, Seq(cacheValue, otherValue))
      _ <- list.prepend(cacheValue).assertingEqual(list)
      _ <- (List(cacheValue, otherValue) ++: list).assertingEqual(list)
      _ <- (cacheValue +: list).assertingEqual(list)
    } yield Passed
  }

  test("prepend (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listPrepend(otherKey, Seq(cacheValue), result = failure)
      _ <- list.prepend(cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("append (all variants)") { (list, connector) =>
    for {
      _ <- connector.expect.listAppend(otherKey, Seq(cacheValue))
      _ <- connector.expect.listAppend(otherKey, Seq(cacheValue))
      _ <- connector.expect.listAppend(otherKey, Seq(cacheValue, cacheValue))
      _ <- list.append(cacheValue).assertingEqual(list)
      _ <- (list :+ cacheValue).assertingEqual(list)
      _ <- (list :++ Seq(cacheValue, cacheValue)).assertingEqual(list)
    } yield Passed
  }

  test("append (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listAppend(otherKey, Seq(cacheValue), result = failure)
      _ <- list.append(cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("get (miss)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 5, 5, Seq(cacheValue))
      _ <- list.get(5).assertingEqual(Some(cacheValue))
    } yield Passed
  }

  test("get (hit)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 5, 5, Seq.empty[String])
      _ <- list.get(5).assertingEqual(None)
    } yield Passed
  }

  test("get (failure)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice[String](otherKey, 5, 5, result = failure)
      _ <- list.get(5).assertingEqual(None)
    } yield Passed
  }

  test("apply (hit)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 5, 5, Seq(cacheValue))
      _ <- list(5).assertingEqual(cacheValue)
    } yield Passed
  }

  test("apply (miss)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 5, 5, Seq.empty[String])
      _ <- list(5).assertingFailure[NoSuchElementException]
    } yield Passed
  }

  test("apply (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice[String](otherKey, 5, 5, result = failure)
      _ <- list(5).assertingFailure[NoSuchElementException]
    } yield Passed
  }

  test("head pop") { (list, connector) =>
    for {
      _ <- connector.expect.listHeadPop[String](otherKey, result = None)
      _ <- list.headPop.assertingEqual(None)
    } yield Passed
  }

  test("head pop (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listHeadPop[String](otherKey, result = failure)
      _ <- list.headPop.assertingEqual(None)
    } yield Passed
  }

  test("size") { (list, connector) =>
    for {
      _ <- connector.expect.listSize(otherKey, 2L)
      _ <- list.size.assertingEqual(2L)
    } yield Passed
  }

  test("size (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSize(otherKey, result = failure)
      _ <- list.size.assertingEqual(0L)
    } yield Passed
  }

  test("insert before") { (list, connector) =>
    for {
      _ <- connector.expect.listInsert(otherKey, "pivot", cacheValue, Some(5L))
      _ <- list.insertBefore("pivot", cacheValue).assertingEqual(Some(5L))
    } yield Passed
  }

  test("insert before (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listInsert(otherKey, "pivot", cacheValue, result = failure)
      _ <- list.insertBefore("pivot", cacheValue).assertingEqual(None)
    } yield Passed
  }

  test("set at position") { (list, connector) =>
    for {
      _ <- connector.expect.listSetAt(otherKey, 5, cacheValue, result = ())
      _ <- list.set(5, cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("set at position (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSetAt(otherKey, 5, cacheValue, result = failure)
      _ <- list.set(5, cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("empty list") { (list, connector) =>
    for {
      _ <- connector.expect.listSize(otherKey, result = 0L)
      _ <- connector.expect.listSize(otherKey, result = 0L)
      _ <- list.isEmpty.assertingEqual(true)
      _ <- list.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  test("non-empty list") { (list, connector) =>
    for {
      _ <- connector.expect.listSize(otherKey, 1L)
      _ <- connector.expect.listSize(otherKey, 1L)
      _ <- list.isEmpty.assertingEqual(false)
      _ <- list.nonEmpty.assertingEqual(true)
    } yield Passed
  }

  test("empty/non-empty list (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSize(otherKey, result = failure)
      _ <- connector.expect.listSize(otherKey, result = failure)
      _ <- list.isEmpty.assertingEqual(true)
      _ <- list.nonEmpty.assertingEqual(false)
    } yield Passed
  }

  test("remove element") { (list, connector) =>
    for {
      _ <- connector.expect.listRemove(otherKey, cacheValue, 1, result = 1L)
      _ <- list.remove(cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("remove element (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listRemove(otherKey, cacheValue, 1, result = failure)
      _ <- list.remove(cacheValue).assertingEqual(list)
    } yield Passed
  }

  test("remove at position") { (list, connector) =>
    for {
      _ <- connector.expect.listRemoveAt(otherKey, 1, result = 1L)
      _ <- list.removeAt(1).assertingEqual(list)
    } yield Passed
  }

  test("remove at position (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listRemoveAt(otherKey, 1, result = failure)
      _ <- list.removeAt(1).assertingEqual(list)
    } yield Passed
  }

  test("view all") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 0, -1, result = Seq(cacheValue))
      _ <- list.view.all.assertingEqual(Seq(cacheValue))
    } yield Passed
  }

  test("view take") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 0, 1, result = Seq(cacheValue))
      _ <- list.view.take(2).assertingEqual(Seq(cacheValue))
    } yield Passed
  }

  test("view drop") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 2, -1, result = Seq(cacheValue))
      _ <- list.view.drop(2).assertingEqual(Seq(cacheValue))
    } yield Passed
  }

  test("view slice") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 1, 2, result = Seq(cacheValue))
      _ <- list.view.slice(1, 2).assertingEqual(Seq(cacheValue))
    } yield Passed
  }

  test("view slice (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice[String](otherKey, 1, 2, result = failure)
      _ <- list.view.slice(1, 2).assertingEqual(Seq.empty)
    } yield Passed
  }

  test("head (non-empty)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 0, 0, result = Seq(cacheValue))
      _ <- connector.expect.listSlice(otherKey, 0, 0, result = Seq(cacheValue))
      _ <- list.head.assertingEqual(cacheValue)
      _ <- list.headOption.assertingEqual(Some(cacheValue))
    } yield Passed
  }

  test("head (empty)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 0, 0, result = Seq.empty[String])
      _ <- connector.expect.listSlice(otherKey, 0, 0, result = Seq.empty[String])
      _ <- list.head.assertingFailure[NoSuchElementException]
      _ <- list.headOption.assertingEqual(None)
    } yield Passed
  }

  test("head (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice[String](otherKey, 0, 0, result = failure)
      _ <- connector.expect.listSlice[String](otherKey, 0, 0, result = failure)
      _ <- list.head.assertingFailure[NoSuchElementException]
      _ <- list.headOption.assertingEqual(None)
    } yield Passed
  }

  test("last (non-empty)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice[String](otherKey, -1, -1, result = Seq(cacheValue))
      _ <- connector.expect.listSlice[String](otherKey, -1, -1, result = Seq(cacheValue))
      _ <- list.last.assertingEqual(cacheValue)
      _ <- list.lastOption.assertingEqual(Some(cacheValue))
    } yield Passed
  }

  test("last (empty)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, -1, -1, result = Seq.empty[String])
      _ <- connector.expect.listSlice(otherKey, -1, -1, result = Seq.empty[String])
      _ <- list.last.assertingFailure[NoSuchElementException]
      _ <- list.lastOption.assertingEqual(None)
    } yield Passed
  }

  test("last (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice[String](otherKey, 0, 0, result = failure)
      _ <- connector.expect.listSlice[String](otherKey, 0, 0, result = failure)
      _ <- list.head.assertingFailure[NoSuchElementException]
      _ <- list.headOption.assertingEqual(None)
    } yield Passed
  }

  test("toList") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice(otherKey, 0, -1, result = Seq(cacheValue, otherValue))
      _ <- list.toList.assertingEqual(Seq(cacheValue, otherValue))
    } yield Passed
  }

  test("toList (failing)") { (list, connector) =>
    for {
      _ <- connector.expect.listSlice[String](otherKey, 0, -1, result = failure)
      _ <- list.toList.assertingEqual(Seq.empty)
    } yield Passed
  }

  test("modify collection") { (list, _) =>
    for {
      _ <- Future.successful(list.modify.collection).assertingEqual(list)
    } yield Passed
  }

  test("modify take") { (list, connector) =>
    for {
      _ <- connector.expect.listTrim(otherKey, 0, 1)
      _ <- list.modify.take(2).assertingSuccess
    } yield Passed
  }

  test("modify drop") { (list, connector) =>
    for {
      _ <- connector.expect.listTrim(otherKey, 2, -1)
      _ <- list.modify.drop(2).assertingSuccess
    } yield Passed
  }

  test("modify clear") { (list, connector) =>
    for {
      _ <- connector.expect.remove(otherKey)
      _ <- list.modify.clear().assertingSuccess
    } yield Passed
  }

  test("modify slice") { (list, connector) =>
    for {
      _ <- connector.expect.listTrim(otherKey, 1, 2)
      _ <- list.modify.slice(1, 2).assertingSuccess
    } yield Passed
  }

  private def test(
    name: String,
    policy: RecoveryPolicy = recoveryPolicy.default,
  )(
    f: (RedisList[String, AsynchronousResult], RedisConnectorMock) => Future[Assertion],
  ): Unit =
    name in {
      implicit val runtime: RedisRuntime = redisRuntime(
        invocationPolicy = LazyInvocation,
        recoveryPolicy = policy,
      )
      implicit val builder: Builders.AsynchronousBuilder.type = AsynchronousBuilder
      val connector: RedisConnectorMock = mock[RedisConnectorMock]
      val list: RedisList[String, AsynchronousResult] = new RedisListImpl[String, AsynchronousResult](otherKey, connector)
      f(list, connector)
    }

}
