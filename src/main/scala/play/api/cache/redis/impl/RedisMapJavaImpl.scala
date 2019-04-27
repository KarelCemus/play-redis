package play.api.cache.redis.impl

import scala.concurrent.Future

import play.api.cache.redis.RedisMap
import play.cache.redis.AsyncRedisMap

class RedisMapJavaImpl[Elem](internal: RedisMap[Elem, Future])(implicit runtime: RedisRuntime) extends AsyncRedisMap[Elem] {
  import JavaCompatibility._

  def add(field: String, value: Elem): CompletionStage[AsyncRedisMap[Elem]] = {
    async { implicit context =>
      internal.add(field, value).map(_ => this)
    }
  }

  def get(field: String): CompletionStage[Optional[Elem]] = {
    async { implicit context =>
      internal.get(field).map(_.asJava)
    }
  }

  def contains(field: String): CompletionStage[java.lang.Boolean] = {
    async { implicit context =>
      internal.contains(field).map(Boolean.box)
    }
  }

  def remove(field: String*): CompletionStage[AsyncRedisMap[Elem]] = {
    async { implicit context =>
      internal.remove(field: _*).map(_ => this)
    }
  }

  def increment(field: String): CompletionStage[java.lang.Long] = {
    async { implicit context =>
      internal.increment(field).map(Long.box)
    }
  }

  def increment(field: String, incrementBy: java.lang.Long): CompletionStage[java.lang.Long] = {
    async { implicit context =>
      internal.increment(field, incrementBy).map(Long.box)
    }
  }

  def toMap: CompletionStage[JavaMap[String, Elem]] = {
    async { implicit context =>
      internal.toMap.map(_.asJava)
    }
  }

  def keySet(): CompletionStage[JavaSet[String]] = {
    async { implicit context =>
      internal.keySet.map(_.asJava)
    }
  }

  def values(): CompletionStage[JavaSet[Elem]] = {
    async { implicit context =>
      internal.values.map(_.asJava)
    }
  }
}
