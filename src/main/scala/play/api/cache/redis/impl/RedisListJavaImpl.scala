package play.api.cache.redis.impl

import scala.concurrent.Future

import play.api.cache.redis.RedisList
import play.cache.redis.AsyncRedisList

class RedisListJavaImpl[Elem](internal: RedisList[Elem, Future])(implicit runtime: RedisRuntime) extends AsyncRedisList[Elem] {
  import JavaCompatibility._

  def This: RedisListJavaImpl[Elem] = this

  private lazy val modifier: AsyncRedisList.AsyncRedisListModification[Elem] = newModifier()
  private lazy val viewer: AsyncRedisList.AsyncRedisListView[Elem] = newViewer()

  private def newViewer(): AsyncRedisList.AsyncRedisListView[Elem] = {
    new AsyncRedisListViewJavaImpl(internal.view)
  }

  private def newModifier(): AsyncRedisList.AsyncRedisListModification[Elem] = {
    new AsyncRedisListModificationJavaImpl(internal.modify)
  }

  override def prepend(element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.prepend(element).map(_ => this)
    }
  }

  override def append(element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.append(element).map(_ => this)
    }
  }

  override def apply(index: Int): CompletionStage[Elem] = {
    async { implicit context =>
      internal.apply(index)
    }
  }

  override def get(index: Int): CompletionStage[Optional[Elem]] = {
    async { implicit context =>
      internal.get(index).map(_.asJava)
    }
  }

  override def headPop(): CompletionStage[Optional[Elem]] = {
    async { implicit context =>
      internal.headPop.map(_.asJava)
    }
  }

  override def insertBefore(pivot: Elem, element: Elem): CompletionStage[Optional[java.lang.Long]] = {
    async { implicit context =>
      internal.insertBefore(pivot, element).map(_.map(Long.box).asJava)
    }
  }

  override def set(position: Int, element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.set(position, element).map(_ => this)
    }
  }

  override def remove(element: Elem): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.remove(element).map(_ => this)
    }
  }

  override def remove(element: Elem, count: Int): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.remove(element, count).map(_ => this)
    }
  }

  override def removeAt(position: Int): CompletionStage[AsyncRedisList[Elem]] = {
    async { implicit context =>
      internal.removeAt(position).map(_ => this)
    }
  }

  override def view(): AsyncRedisList.AsyncRedisListView[Elem] = viewer

  override def modify(): AsyncRedisList.AsyncRedisListModification[Elem] = modifier

  private class AsyncRedisListViewJavaImpl(view: internal.RedisListView) extends AsyncRedisList.AsyncRedisListView[Elem] {

    override def slice(from: Int, end: Int): CompletionStage[JavaList[Elem]] = {
      async { implicit context =>
        view.slice(from, end).map(_.asJava)
      }
    }
  }

  private class AsyncRedisListModificationJavaImpl(modification: internal.RedisListModification) extends AsyncRedisList.AsyncRedisListModification[Elem] {

    override def collection(): AsyncRedisList[Elem] = This

    override def clear(): CompletionStage[AsyncRedisList.AsyncRedisListModification[Elem]] = {
      async { implicit context =>
        modification.clear().map(_ => this)
      }
    }

    override def slice(from: Int, end: Int): CompletionStage[AsyncRedisList.AsyncRedisListModification[Elem]] = {
      async { implicit context =>
        modification.slice(from, end).map(_ => this)
      }
    }
  }

}
