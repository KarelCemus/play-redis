package play.api.cache.redis.impl

import play.api.cache.redis.RedisSet
import play.cache.redis.AsyncRedisSet

import scala.concurrent.Future

class RedisSetJavaImpl[Elem](internal: RedisSet[Elem, Future])(implicit runtime: RedisRuntime) extends AsyncRedisSet[Elem] {
  import JavaCompatibility._

  override def add(elements: Elem*): CompletionStage[AsyncRedisSet[Elem]] =
    async { implicit context =>
      internal.add(elements: _*).map(_ => this)
    }

  override def contains(element: Elem): CompletionStage[java.lang.Boolean] =
    async { implicit context =>
      internal.contains(element).map(Boolean.box)
    }

  override def remove(elements: Elem*): CompletionStage[AsyncRedisSet[Elem]] =
    async { implicit context =>
      internal.remove(elements: _*).map(_ => this)
    }

  override def toSet: CompletionStage[JavaSet[Elem]] =
    async { implicit context =>
      internal.toSet.map(_.asJava)
    }

}
