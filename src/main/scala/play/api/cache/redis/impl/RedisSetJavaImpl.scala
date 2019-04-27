package play.api.cache.redis.impl

import scala.concurrent.Future

import play.api.cache.redis.RedisSet
import play.cache.redis.AsyncRedisSet

class RedisSetJavaImpl[Elem](internal: RedisSet[Elem, Future])(implicit runtime: RedisRuntime) extends AsyncRedisSet[Elem] {
  import JavaCompatibility._

  def add(element: Elem*): CompletionStage[AsyncRedisSet[Elem]] = {
    async { implicit context =>
      internal.add(element: _*).map(_ => this)
    }
  }

  def contains(element: Elem): CompletionStage[java.lang.Boolean] = {
    async { implicit context =>
      internal.contains(element).map(Boolean.box)
    }
  }

  def remove(element: Elem*): CompletionStage[AsyncRedisSet[Elem]] = {
    async { implicit context =>
      internal.remove(element: _*).map(_ => this)
    }
  }

  def toSet: CompletionStage[JavaSet[Elem]] = {
    async { implicit context =>
      internal.toSet.map(_.asJava)
    }
  }
}
