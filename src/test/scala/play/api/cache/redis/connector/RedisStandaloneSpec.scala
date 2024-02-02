package play.api.cache.redis.connector

import akka.actor.ActorSystem
import play.api.cache.redis._
import play.api.cache.redis.configuration._
import play.api.cache.redis.impl._
import play.api.cache.redis.test._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class RedisStandaloneSpec extends IntegrationSpec with RedisStandaloneContainer {

  test("pong on ping") { (_, connector) =>
    for {
      _ <- connector.ping().assertingSuccess
    } yield Passed
  }

  test("miss on get") { (cacheKey, connector) =>
    for {
      _ <- connector.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("hit after set") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value").assertingEqual(true)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
    } yield Passed
  }

  test("ignore set if not exists when already defined") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "previous").assertingEqual(true)
      _ <- connector.set(cacheKey, "value", ifNotExists = true).assertingEqual(false)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("previous"))
    } yield Passed
  }

  test("perform set if not exists when undefined") { (cacheKey, connector) =>
    for {
      _ <- connector.get[String](cacheKey).assertingEqual(None)
      _ <- connector.set(cacheKey, "value", ifNotExists = true).assertingEqual(true)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.set(cacheKey, "other", ifNotExists = true).assertingEqual(false)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
    } yield Passed
  }

  test("perform set if not exists with expiration") { (cacheKey, connector) =>
    for {
      _ <- connector.get[String](cacheKey).assertingEqual(None)
      _ <- connector.set(cacheKey, "value", 300.millis, ifNotExists = true).assertingEqual(true)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      // wait until the first duration expires
      _ <- Future.waitFor(400.millis)
      _ <- connector.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("hit after mset") { (cacheKey, connector) =>
    for {
      _ <- connector.mSet(s"$cacheKey-1" -> "value-1", s"$cacheKey-2" -> "value-2")
      _ <- connector.mGet[String](s"$cacheKey-1", s"$cacheKey-2", s"$cacheKey-3").assertingEqual(List(Some("value-1"), Some("value-2"), None))
      _ <- connector.mSet(s"$cacheKey-3" -> "value-3", s"$cacheKey-2" -> null)
      _ <- connector.mGet[String](s"$cacheKey-1", s"$cacheKey-2", s"$cacheKey-3").assertingEqual(List(Some("value-1"), None, Some("value-3")))
      _ <- connector.mSet(s"$cacheKey-3" -> null)
      _ <- connector.mGet[String](s"$cacheKey-1", s"$cacheKey-2", s"$cacheKey-3").assertingEqual(List(Some("value-1"), None, None))
    } yield Passed
  }

  test("ignore msetnx if already defined") { (cacheKey, connector) =>
    for {
      _ <- connector.mSetIfNotExist(s"$cacheKey-1" -> "value-1", s"$cacheKey-2" -> "value-2").assertingEqual(true)
      _ <- connector.mGet[String](s"$cacheKey-1", s"$cacheKey-2").assertingEqual(List(Some("value-1"), Some("value-2")))
      _ <- connector.mSetIfNotExist(s"$cacheKey-3" -> "value-3", s"$cacheKey-2" -> "value-2").assertingEqual(false)
    } yield Passed
  }

  test("expire refreshes expiration") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value", 200.millis)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.expire(cacheKey, 1000.millis)
      // wait until the first duration expires
      _ <- Future.waitFor(300.millis)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
    } yield Passed
  }

  test("expires in returns finite duration") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value", 2.second)
      _ <- connector.expiresIn(cacheKey).assertingCondition(_.exists(_ <= 2.seconds))
    } yield Passed
  }

  test("expires in returns infinite duration") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value")
      _ <- connector.expiresIn(cacheKey).assertingEqual(Some(Duration.Inf))
    } yield Passed
  }

  test("expires in returns not defined key") { (cacheKey, connector) =>
    for {
      _ <- connector.expiresIn(cacheKey).assertingEqual(None)
      _ <- connector.set(cacheKey, "value", 200.millis)
      _ <- connector.expiresIn(cacheKey).assertingCondition(_.exists(_ <= 200.millis))
      // wait until the first duration expires
      _ <- Future.waitFor(300.millis)
      _ <- connector.expiresIn(cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("positive exists on existing keys") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value")
      _ <- connector.exists(cacheKey).assertingEqual(true)
    } yield Passed
  }

  test("negative exists on expired and missing keys") { (cacheKey, connector) =>
    for {
      _ <- connector.set(s"$cacheKey-1", "value", 200.millis)
      _ <- connector.exists(s"$cacheKey-1").assertingEqual(true)
      // wait until the duration expires
      _ <- Future.waitFor(250.millis)
      _ <- connector.exists(s"$cacheKey-1").assertingEqual(false)
      _ <- connector.exists(s"$cacheKey-2").assertingEqual(false)
    } yield Passed
  }

  test("miss after remove") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value")
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.remove(cacheKey).assertingSuccess
      _ <- connector.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("remove on empty key") { (cacheKey, connector) =>
    for {
      _ <- connector.get[String](cacheKey).assertingEqual(None)
      _ <- connector.remove(cacheKey).assertingSuccess
      _ <- connector.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("remove with empty args") { (_, connector) =>
      connector.remove().assertingSuccess
  }

  test("clear with setting null") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value")
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.set(cacheKey, null)
      _ <- connector.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("miss after timeout") { (cacheKey, connector) =>
    for {
      // set
      _ <- connector.set(cacheKey, "value", 200.millis)
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      // wait until it expires
      _ <- Future.waitFor(250.millis)
      // miss
      _ <- connector.get[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("find all matching keys") { (cacheKey, connector) =>
    for {
      _ <- connector.set(s"$cacheKey-key-A", "value", 3.second)
      _ <- connector.set(s"$cacheKey-note-A", "value", 3.second)
      _ <- connector.set(s"$cacheKey-key-B", "value", 3.second)
      _ <- connector.matching(s"$cacheKey-*").map(_.toSet).assertingEqual(Set(s"$cacheKey-key-A", s"$cacheKey-note-A", s"$cacheKey-key-B"))
      _ <- connector.matching(s"$cacheKey-*A").map(_.toSet).assertingEqual(Set(s"$cacheKey-key-A", s"$cacheKey-note-A"))
      _ <- connector.matching(s"$cacheKey-key-*").map(_.toSet).assertingEqual(Set(s"$cacheKey-key-A", s"$cacheKey-key-B"))
      _ <- connector.matching(s"$cacheKey-* A * ").assertingEqual(Seq.empty)
    }
    yield Passed
  }

  test("remove multiple keys at once") { (cacheKey, connector) =>
    for {
      _ <- connector.set(s"$cacheKey-1", "value")
      _ <- connector.get[String](s"$cacheKey-1").assertingEqual(Some("value"))
      _ <- connector.set(s"$cacheKey-2", "value")
      _ <- connector.get[String](s"$cacheKey-2").assertingEqual(Some("value"))
      _ <- connector.set(s"$cacheKey-3", "value")
      _ <- connector.get[String](s"$cacheKey-3").assertingEqual(Some("value"))
      _ <- connector.remove(s"$cacheKey-1", s"$cacheKey-2", s"$cacheKey-3")
      _ <- connector.get[String](s"$cacheKey-1").assertingEqual(None)
      _ <- connector.get[String](s"$cacheKey-2").assertingEqual(None)
      _ <- connector.get[String](s"$cacheKey-3").assertingEqual(None)
    } yield Passed
  }

  test("remove in batch") { (cacheKey, connector) =>
    for {
      _ <- connector.set(s"$cacheKey-1", "value")
      _ <- connector.get[String](s"$cacheKey-1").assertingEqual(Some("value"))
      _ <- connector.set(s"$cacheKey-2", "value")
      _ <- connector.get[String](s"$cacheKey-2").assertingEqual(Some("value"))
      _ <- connector.set(s"$cacheKey-3", "value")
      _ <- connector.get[String](s"$cacheKey-3").assertingEqual(Some("value"))
      _ <- connector.remove(s"$cacheKey-1", s"$cacheKey-2", s"$cacheKey-3")
      _ <- connector.get[String](s"$cacheKey-1").assertingEqual(None)
      _ <- connector.get[String](s"$cacheKey-2").assertingEqual(None)
      _ <- connector.get[String](s"$cacheKey-3").assertingEqual(None)
    } yield Passed
  }

  test("set a zero when not exists and then increment") { (cacheKey, connector) =>
    for {
      _ <- connector.increment(cacheKey, 1).assertingEqual(1)
    } yield Passed
  }

  test("throw an exception when not integer") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value")
      _ <- connector.increment(cacheKey, 1).assertingFailure[ExecutionFailedException]
    } yield Passed
  }

  test("increment by one") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, 5)
      _ <- connector.increment(cacheKey, 1).assertingEqual(6)
      _ <- connector.increment(cacheKey, 1).assertingEqual(7)
      _ <- connector.increment(cacheKey, 1).assertingEqual(8)
    } yield Passed
  }

  test("increment by some") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, 5)
      _ <- connector.increment(cacheKey, 1).assertingEqual(6)
      _ <- connector.increment(cacheKey, 2).assertingEqual(8)
      _ <- connector.increment(cacheKey, 3).assertingEqual(11)
    } yield Passed
  }

  test("decrement by one") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, 5)
      _ <- connector.increment(cacheKey, -1).assertingEqual(4)
      _ <- connector.increment(cacheKey, -1).assertingEqual(3)
      _ <- connector.increment(cacheKey, -1).assertingEqual(2)
      _ <- connector.increment(cacheKey, -1).assertingEqual(1)
      _ <- connector.increment(cacheKey, -1).assertingEqual(0)
      _ <- connector.increment(cacheKey, -1).assertingEqual(-1)
    } yield Passed
  }

  test("decrement by some") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, 5)
      _ <- connector.increment(cacheKey, -1).assertingEqual(4)
      _ <- connector.increment(cacheKey, -2).assertingEqual(2)
      _ <- connector.increment(cacheKey, -3).assertingEqual(-1)
    } yield Passed
  }

  test("append like set when value is undefined") { (cacheKey, connector) =>
    for {
      _ <- connector.get[String](cacheKey).assertingEqual(None)
      _ <- connector.append(cacheKey, "value")
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
    } yield Passed
  }

  test("append to existing string") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "some")
      _ <- connector.get[String](cacheKey).assertingEqual(Some("some"))
      _ <- connector.append(cacheKey, " value")
      _ <- connector.get[String](cacheKey).assertingEqual(Some("some value"))
    } yield Passed
  }

  test("list push left") { (cacheKey, connector) =>
    for {
      _ <- connector.listPrepend(cacheKey, "A", "B", "C").assertingEqual(3)
      _ <- connector.listPrepend(cacheKey, "D", "E", "F").assertingEqual(6)
      _ <- connector.listSlice[String](cacheKey, 0, -1).assertingEqual(List("F", "E", "D", "C", "B", "A"))
    } yield Passed
  }

  test("list push right") { (cacheKey, connector) =>
    for {
      _ <- connector.listAppend(cacheKey, "A", "B", "C").assertingEqual(3)
      _ <- connector.listAppend(cacheKey, "D", "E", "A").assertingEqual(6)
      _ <- connector.listSlice[String](cacheKey, 0, -1).assertingEqual(List("A", "B", "C", "D", "E", "A"))
    } yield Passed
  }

  test("list size") { (cacheKey, connector) =>
    for {
      _ <- connector.listSize(cacheKey).assertingEqual(0)
      _ <- connector.listPrepend(cacheKey, "A", "B", "C").assertingEqual(3)
      _ <- connector.listSize(cacheKey).assertingEqual(3)
    } yield Passed
  }

  test("list overwrite at index") { (cacheKey, connector) =>
    for {
      _ <- connector.listPrepend(cacheKey, "C", "B", "A").assertingEqual(3)
      _ <- connector.listSetAt(cacheKey, 1, "D")
      _ <- connector.listSlice[String](cacheKey, 0, -1).assertingEqual(List("A", "D", "C"))
      _ <- connector.listSetAt(cacheKey, 3, "D").assertingFailure[IndexOutOfBoundsException]
    } yield Passed
  }

  test("list pop head") { (cacheKey, connector) =>
    for {
      _ <- connector.listHeadPop[String](cacheKey).assertingEqual(None)
      _ <- connector.listPrepend(cacheKey, "C", "B", "A").assertingEqual(3)
      _ <- connector.listHeadPop[String](cacheKey).assertingEqual(Some("A"))
      _ <- connector.listHeadPop[String](cacheKey).assertingEqual(Some("B"))
      _ <- connector.listHeadPop[String](cacheKey).assertingEqual(Some("C"))
      _ <- connector.listHeadPop[String](cacheKey).assertingEqual(None)
    } yield Passed
  }

  test("list slice view") { (cacheKey, connector) =>
    for {
      _ <- connector.listSlice[String](cacheKey, 0, -1).assertingEqual(List.empty)
      _ <- connector.listPrepend(cacheKey, "C", "B", "A").assertingEqual(3)
      _ <- connector.listSlice[String](cacheKey, 0, -1).assertingEqual(List("A", "B", "C"))
      _ <- connector.listSlice[String](cacheKey, 0, 0).assertingEqual(List("A"))
      _ <- connector.listSlice[String](cacheKey, -2, -1).assertingEqual(List("B", "C"))
    } yield Passed
  }

  test("list remove by value") { (cacheKey, connector) =>
    for {
      _ <- connector.listRemove(cacheKey, "A", count = 1).assertingEqual(0)
      _ <- connector.listPrepend(cacheKey, "A", "B", "C").assertingEqual(3)
      _ <- connector.listRemove(cacheKey, "A", count = 1).assertingEqual(1)
      _ <- connector.listSize(cacheKey).assertingEqual(2)
    } yield Passed
  }

  test("list trim") { (cacheKey, connector) =>
    for {
      _ <- connector.listPrepend(cacheKey, "C", "B", "A").assertingEqual(3)
      _ <- connector.listTrim(cacheKey, 1, 2)
      _ <- connector.listSize(cacheKey).assertingEqual(2)
      _ <- connector.listSlice[String](cacheKey, 0, -1).assertingEqual(List("B", "C"))
    } yield Passed
  }

  test("list insert") { (cacheKey, connector) =>
    for {
      _ <- connector.listSize(cacheKey).assertingEqual(0)
      _ <- connector.listInsert(cacheKey, "C", "B").assertingEqual(None)
      _ <- connector.listPrepend(cacheKey, "C", "A").assertingEqual(2)
      _ <- connector.listInsert(cacheKey, "C", "B").assertingEqual(Some(3L))
      _ <- connector.listInsert(cacheKey, "E", "D").assertingEqual(None)
      _ <- connector.listSlice[String](cacheKey, 0, -1).assertingEqual(List("A", "B", "C"))
    } yield Passed
  }

  test("list set to invalid type") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value").assertingSuccess
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.listPrepend(cacheKey, "A").assertingFailure[IllegalArgumentException]
      _ <- connector.listAppend(cacheKey, "C", "B").assertingFailure[IllegalArgumentException]
      _ <- connector.listInsert(cacheKey, "C", "B").assertingFailure[IllegalArgumentException]
    } yield Passed
  }

    test("set add") { (cacheKey, connector) =>
      for {
        _ <- connector.setSize(cacheKey).assertingEqual(0)
        _ <- connector.setAdd(cacheKey, "A", "B").assertingEqual(2)
        _ <- connector.setSize(cacheKey).assertingEqual(2)
        _ <- connector.setAdd(cacheKey, "C", "B").assertingEqual(1)
        _ <- connector.setSize(cacheKey).assertingEqual(3)
      } yield Passed
    }

  test("set add into invalid type") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value").assertingSuccess
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.setAdd(cacheKey, "A", "B").assertingFailure[IllegalArgumentException]
    } yield Passed
  }

    test("set rank") { (cacheKey, connector) =>
      for {
        _ <- connector.setSize(cacheKey).assertingEqual(0)
        _ <- connector.setAdd(cacheKey, "A", "B").assertingEqual(2)
        _ <- connector.setSize(cacheKey).assertingEqual(2)

        _ <- connector.setIsMember(cacheKey, "A").assertingEqual(true)
        _ <- connector.setIsMember(cacheKey, "B").assertingEqual(true)
        _ <- connector.setIsMember(cacheKey, "C").assertingEqual(false)

        _ <- connector.setAdd(cacheKey, "C", "B").assertingEqual(1)

        _ <- connector.setIsMember(cacheKey, "A").assertingEqual(true)
        _ <- connector.setIsMember(cacheKey, "B").assertingEqual(true)
        _ <- connector.setIsMember(cacheKey, "C").assertingEqual(true)
      } yield Passed
    }

  test("set size") { (cacheKey, connector) =>
    for {
      _ <- connector.setSize(cacheKey).assertingEqual(0)
      _ <- connector.setAdd(cacheKey, "A", "B").assertingEqual(2)
      _ <- connector.setSize(cacheKey).assertingEqual(2)
    } yield Passed
  }

  test("set rem") { (cacheKey, connector) =>
    for {
      _ <- connector.setSize(cacheKey).assertingEqual(0)
      _ <- connector.setAdd(cacheKey, "A", "B", "C").assertingEqual(3)
      _ <- connector.setSize(cacheKey).assertingEqual(3)

      _ <- connector.setRemove(cacheKey, "A").assertingEqual(1)
      _ <- connector.setSize(cacheKey).assertingEqual(2)
      _ <- connector.setRemove(cacheKey, "B", "C", "D").assertingEqual(2)
      _ <- connector.setSize(cacheKey).assertingEqual(0)
    } yield Passed
  }

    test("set slice") { (cacheKey, connector) =>
      for {
        _ <- connector.setSize(cacheKey).assertingEqual(0)
        _ <- connector.setAdd(cacheKey, "A", "B", "C").assertingEqual(3)
        _ <- connector.setSize(cacheKey).assertingEqual(3)

        _ <- connector.setMembers[String](cacheKey).assertingEqual(Set("A", "B", "C"))

        _ <- connector.setSize(cacheKey).assertingEqual(3)
      } yield Passed
    }

  test("hash set values") { (cacheKey, connector) =>
    for {
      _ <- connector.hashSize(cacheKey).assertingEqual(0)
      _ <- connector.hashGetAll[String] (cacheKey).assertingEqual(Map.empty)
      _ <- connector.hashKeys(cacheKey).assertingEqual(Set.empty)
      _ <- connector.hashValues[String] (cacheKey).assertingEqual(Set.empty)

      _ <- connector.hashGet[String] (cacheKey, "KA").assertingEqual(None)
      _ <- connector.hashSet(cacheKey, "KA", "VA1").assertingEqual(true)
      _ <- connector.hashGet[String] (cacheKey, "KA").assertingEqual(Some("VA1"))
      _ <- connector.hashSet(cacheKey, "KA", "VA2").assertingEqual(false)
      _ <- connector.hashGet[String] (cacheKey, "KA").assertingEqual(Some("VA2"))
      _ <- connector.hashSet(cacheKey, "KB", "VB").assertingEqual(true)

      _ <- connector.hashGet[String] (cacheKey, Seq("KA", "KB", "KC")).assertingEqual(Seq(Some("VA2"), Some("VB"), None))

      _ <- connector.hashExists(cacheKey, "KB").assertingEqual(true)
      _ <- connector.hashExists(cacheKey, "KC").assertingEqual(false)

      _ <- connector.hashSize(cacheKey).assertingEqual(2)
      _ <- connector.hashGetAll[String] (cacheKey).assertingEqual(Map("KA" -> "VA2", "KB" -> "VB"))
      _ <- connector.hashKeys(cacheKey).assertingEqual(Set("KA", "KB"))
      _ <- connector.hashValues[String] (cacheKey).assertingEqual(Set("VA2", "VB"))

      _ <- connector.hashRemove(cacheKey, "KB").assertingEqual(1)
      _ <- connector.hashRemove(cacheKey, "KC").assertingEqual(0)
      _ <- connector.hashExists(cacheKey, "KB").assertingEqual(false)
      _ <- connector.hashExists(cacheKey, "KA").assertingEqual(true)

      _ <- connector.hashSize(cacheKey).assertingEqual(1)
      _ <- connector.hashGetAll[String] (cacheKey).assertingEqual(Map("KA" -> "VA2"))
      _ <- connector.hashKeys(cacheKey).assertingEqual(Set("KA"))
      _ <- connector.hashValues[String] (cacheKey).assertingEqual(Set("VA2"))

      _ <- connector.hashSet(cacheKey, "KD", 5).assertingEqual(true)
      _ <- connector.hashIncrement(cacheKey, "KD", 2).assertingEqual(7)
      _ <- connector.hashGet[Int](cacheKey, "KD").assertingEqual(Some(7))
    } yield Passed
  }

  test("hash set into invalid type") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value").assertingSuccess
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.hashSet(cacheKey, "KA", "VA1").assertingFailure[IllegalArgumentException]
    } yield Passed
  }

  test("sorted set add") { (cacheKey, connector) =>
    for {
      _ <- connector.sortedSetAdd(cacheKey, (1, "A")).assertingEqual(1)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(1)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(1)
      _ <- connector.sortedSetAdd(cacheKey, (2, "B"), (3, "C")).assertingEqual(2)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(3)
      _ <- connector.sortedSetAdd(cacheKey, (1, "A")).assertingEqual(0)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(3)
    } yield Passed
  }

  test("sorted set add invalid type") { (cacheKey, connector) =>
    for {
      _ <- connector.set(cacheKey, "value").assertingSuccess
      _ <- connector.get[String](cacheKey).assertingEqual(Some("value"))
      _ <- connector.sortedSetAdd(cacheKey, 1D -> "VA1").assertingFailure[IllegalArgumentException]
    } yield Passed
  }

  test("sorted set score") { (cacheKey, connector) =>
    for {
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(0)
      _ <- connector.sortedSetAdd(cacheKey, 1D -> "A", 3D -> "B").assertingEqual(2)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(2)

      _ <- connector.sortedSetScore(cacheKey, "A").assertingEqual(Some(1D))
      _ <- connector.sortedSetScore(cacheKey, "B").assertingEqual(Some(3D))
      _ <- connector.sortedSetScore(cacheKey, "C").assertingEqual(None)

      _ <- connector.sortedSetAdd(cacheKey, 2D -> "C", 4D -> "B").assertingEqual(1)

      _ <- connector.sortedSetScore(cacheKey, "A").assertingEqual(Some(1D))
      _ <- connector.sortedSetScore(cacheKey, "B").assertingEqual(Some(4D))
      _ <- connector.sortedSetScore(cacheKey, "C").assertingEqual(Some(2D))
    } yield Passed
  }

  test("sorted set size") { (cacheKey, connector) =>
    for {
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(0)
      _ <- connector.sortedSetAdd(cacheKey, (1, "A"), (2, "B")).assertingEqual(2)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(2)
    } yield Passed
  }

  test("sorted set remove") { (cacheKey, connector) =>
    for {
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(0)
      _ <- connector.sortedSetAdd(cacheKey, 1D -> "A", 2D -> "B", 3D -> "C").assertingEqual(3)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(3)

      _ <- connector.sortedSetRemove(cacheKey, "A").assertingEqual(1)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(2)
      _ <- connector.sortedSetRemove(cacheKey, "B", "C", "D").assertingEqual(2)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(0)
    } yield Passed
  }

  test("sorted set range") { (cacheKey, connector) =>
    for {
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(0)
      _ <- connector.sortedSetAdd(cacheKey, 1D -> "A", 2D -> "B", 4D -> "C").assertingEqual(3)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(3)

      _ <- connector.sortedSetRange[String](cacheKey, 0, 1).assertingEqual(Vector("A", "B"))
      _ <- connector.sortedSetRange[String](cacheKey, 0, 4).assertingEqual(Vector("A", "B", "C"))
      _ <- connector.sortedSetRange[String](cacheKey, 1, 9).assertingEqual(Vector("B", "C"))

      _ <- connector.sortedSetSize(cacheKey).assertingEqual(3)
    } yield Passed
  }

  test("sorted set reverse range") { (cacheKey, connector) =>
    for {
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(0)
      _ <- connector.sortedSetAdd(cacheKey, 1D -> "A", 2D -> "B", 4D -> "C").assertingEqual(3)
      _ <- connector.sortedSetSize(cacheKey).assertingEqual(3)

      _ <- connector.sortedSetReverseRange[String](cacheKey, 0, 1).assertingEqual(Vector("C", "B"))
      _ <- connector.sortedSetReverseRange[String](cacheKey, 0, 4).assertingEqual(Vector("C", "B", "A"))
      _ <- connector.sortedSetReverseRange[String](cacheKey, 1, 9).assertingEqual(Vector("B", "A"))

      _ <- connector.sortedSetSize(cacheKey).assertingEqual(3)
    } yield Passed
  }

  def test(name: String)(f: (String, RedisConnector) => Future[Assertion]): Unit = {
    name in {
      implicit val system: ActorSystem = ActorSystem("test", classLoader = Some(getClass.getClassLoader))
      implicit val runtime: RedisRuntime = RedisRuntime("standalone", syncTimeout = 5.seconds, ExecutionContext.global, new LogAndFailPolicy, LazyInvocation)
      implicit val application: StoppableApplication = StoppableApplication(system)
      val serializer = new AkkaSerializerImpl(system)

      lazy val instance = RedisStandalone(
        name = "play",
        host = RedisHost(container.containerIpAddress, container.mappedPort(defaultPort)),
        settings = RedisSettings.load(
          config = Helpers.configuration.default.underlying,
          path = "play.cache.redis"
        )
      )
      
      val cacheKey = name.toLowerCase().replace(" ", "-")

      application.runAsyncInApplication {
        for {
          connector <- Future(new RedisConnectorProvider(instance, serializer).get)
          // initialize the connector by flushing the database
          _ <- connector.invalidate()
          // run the test
          _ <- f(cacheKey, connector)
        } yield Passed
      }
    }
  }

 }
