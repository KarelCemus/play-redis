package play.api.cache.redis.impl

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RedisJavaMapSpec(implicit ee: ExecutionEnv) extends Specification with ReducedMockito {
  import Implicits._
  import JavaCompatibility._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "Redis Map" should {

    "set" in new MockedJavaMap {
      internal.add(beEq(field), beEq(value)) returns internal
      map.add(field, value).asScala must beEqualTo(map).await
      there were one(internal).add(field, value)
    }

    "get" in new MockedJavaMap {
      internal.get(beEq(field)) returns Some(value)
      map.get(field).asScala must beEqualTo(Some(value).asJava).await
      there were one(internal).get(field)
    }

    "contains" in new MockedJavaMap {
      internal.contains(beEq(field)) returns true
      map.contains(field).asScala.map(Boolean.unbox) must beEqualTo(true).await
      there were one(internal).contains(field)
    }

    "remove" in new MockedJavaMap {
      internal.remove(anyVarArgs[String]) returns internal
      map.remove(field, other).asScala must beEqualTo(map).await
      there were one(internal).remove(field, other)
    }

    "increment" in new MockedJavaMap {
      internal.increment(beEq(field), anyLong()) returns 4L
      map.increment(field).asScala.map(Long.unbox) must beEqualTo(4L).await
      there were one(internal).increment(field)
    }

    "increment by" in new MockedJavaMap {
      internal.increment(beEq(field), beEq(2L)) returns 6L
      map.increment(field, 2L).asScala.map(Long.unbox) must beEqualTo(6L).await
      there were one(internal).increment(field, 2L)
    }

    "toMap" in new MockedJavaMap {
      internal.toMap returns Map(key -> value)
      map.toMap().asScala must beEqualTo(Map(key -> value).asJava).await
      there were one(internal).toMap
    }

    "keySet" in new MockedJavaMap {
      internal.keySet returns Set(key, other)
      map.keySet().asScala must beEqualTo(Set(key, other).asJava).await
      there were one(internal).keySet
    }

    "values" in new MockedJavaMap {
      internal.values returns Set(value, other)
      map.values().asScala must beEqualTo(Set(value, other).asJava).await
      there were one(internal).values
    }
  }
}
