package play.api.cache.redis.impl

import scala.concurrent.Future

import play.api.cache.redis.RedisList
import play.cache.redis.AsyncRedisList

class RedisListJavaImpl[Elem](internal: RedisList[Elem, Future])(implicit runtime: RedisRuntime) extends AsyncRedisList[Elem] {
  import JavaCompatibility._

  def This = this

  def prepend(element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.prepend(element).map(_ => this)
    }
  }

  def append(element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.append(element).map(_ => this)
    }
  }

  def apply(index: Int): CompletionStage[Elem] = {
    async { implicit context =>
      internal.apply(index)
    }
  }

  def get(index: Int): CompletionStage[Optional[Elem]] = {
    async { implicit context =>
      internal.get(index).map(_.asJava)
    }
  }

  def headPop(): CompletionStage[Optional[Elem]] = {
    async { implicit context =>
      internal.headPop.map(_.asJava)
    }
  }

  def insertBefore(pivot: Elem, element: Elem): CompletionStage[Optional[java.lang.Long]] = {
    async { implicit context =>
      internal.insertBefore(pivot, element).map(_.map(Long.box).asJava)
    }
  }

  def set(position: Int, element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.set(position, element).map(_ => this)
    }
  }

  def remove(element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.remove(element).map(_ => this)
    }
  }

  def remove(element: Elem, count: Int): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.remove(element, count).map(_ => this)
    }
  }

  def removeAt(position: Int): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.removeAt(position).map(_ => this)
    }
  }

  def view(): AsyncRedisList.AsyncRedisListView[Elem] = {
    new AsyncRedisListViewJavaImpl(internal.view)
  }

  def modify(): AsyncRedisList.AsyncRedisListModification[Elem] = {
    new AsyncRedisListModificationJavaImpl(internal.modify)
  }

  class AsyncRedisListViewJavaImpl(view: internal.RedisListView) extends AsyncRedisList.AsyncRedisListView[Elem] {

    def slice(from: Int, end: Int): CompletionStage[JavaList[Elem]] = {
      async { implicit context =>
        view.slice(from, end).map(_.asJava)
      }
    }
  }

  class AsyncRedisListModificationJavaImpl(modification: internal.RedisListModification) extends AsyncRedisList.AsyncRedisListModification[Elem] {

    def collection(): AsyncRedisList[Elem] = This

    def clear(): CompletionStage[AsyncRedisList.AsyncRedisListModification[Elem]] = {
      async { implicit context =>
        modification.clear().map(_ => this)
      }
    }

    def slice(from: Int, end: Int): CompletionStage[AsyncRedisList.AsyncRedisListModification[Elem]] = {
      async { implicit context =>
        modification.slice(from, end).map(_ => this)
      }
    }
  }

}
