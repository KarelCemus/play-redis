package play.api.cache.redis.impl

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import play.api.Environment
import play.api.cache.redis._
import play.cache.redis._

/**
  * Implements Play Java version of play.api.CacheApi
  *
  * This acts as an adapter to Play Scala CacheApi, because Java Api is slightly different than Scala Api
  */
private[impl] class AsyncJavaRedis(internal: CacheAsyncApi)(implicit environment: Environment, runtime: RedisRuntime) extends play.cache.AsyncCacheApi with play.cache.redis.AsyncCacheApi {
  import JavaCompatibility._

  def set(key: String, value: scala.Any, expiration: Int): CompletionStage[Done] = {
    async { implicit context =>
      set(key, value, expiration.seconds)
    }
  }

  def set(key: String, value: scala.Any): CompletionStage[Done] = {
    async { implicit context =>
      set(key, value, Duration.Inf)
    }
  }

  private def set(key: String, value: scala.Any, duration: Duration)(implicit ec: ExecutionContext): Future[Done] = {
    Future.from(
      // set the value
      internal.set(key, value, duration),
      // and set its type to be able to read it
      internal.set(classTagKey(key), classTagOf(value), duration)
    ).asDone
  }

  def remove(key: String): CompletionStage[Done] = {
    internal.remove(key, classTagKey(key)).asJava
  }

  def get[T](key: String): CompletionStage[T] = getOrElse[T](key, None)

  def getOptional[T](key: String): CompletionStage[Optional[T]] = {
    async { implicit context =>
      getOrElseOption[T](key, None).map(_.asJava)
    }
  }

  def getOrElse[T](key: String, block: Callable[T]): CompletionStage[T] =
    getOrElseUpdate[T](key, (() => Future.successful(block.call()).asJava).asJava)

  def getOrElse[T](key: String, block: Callable[T], expiration: Int): CompletionStage[T] =
    getOrElseUpdate[T](key, (() => Future.successful(block.call()).asJava).asJava, expiration)

  def getOrElseUpdate[T](key: String, block: Callable[CompletionStage[T]]): CompletionStage[T] =
    getOrElse[T](key, Some(block))

  def getOrElseUpdate[T](key: String, block: Callable[CompletionStage[T]], expiration: Int): CompletionStage[T] =
    getOrElse[T](key, Some(block), duration = expiration.seconds)

  private def getOrElse[T](key: String, callable: Option[Callable[CompletionStage[T]]], duration: Duration = Duration.Inf): CompletionStage[T] = {
    async { implicit context =>
      getOrElseOption(key, callable, duration).map[T](play.libs.Scala.orNull)
    }
  }

  private def getOrElseOption[T](key: String, callable: Option[Callable[CompletionStage[T]]], duration: Duration = Duration.Inf)(implicit context: ExecutionContext): Future[Option[T]] = {
    // get the tag and decode it
    def getClassTag = internal.get[String](classTagKey(key))
    def decodedClassTag(tag: Option[String]) = tag.map(classTagFrom[T])
    // if tag is defined, get Option[ value ] otherwise None
    def getValue = getClassTag.map(decodedClassTag).flatMap {
      case Some(ClassTag.Null) => Future.successful(Some(null.asInstanceOf[T]))
      case Some(tag)           => internal.get[T](key)(tag)
      case None                => Future.successful(None)
    }
    // compute or else and save it into cache
    def orElse(callable: Callable[CompletionStage[T]]) = callable.call().asScala
    def saveOrElse(value: T) = set(key, value, duration)
    def savedOrElse(callable: Callable[CompletionStage[T]]) = orElse(callable).flatMap {
      value => runtime.invocation.invoke(saveOrElse(value), Some(value))
    }

    getValue.flatMap {
      case Some(value) => Future.successful(Some(value))
      case None        => callable.fold[Future[Option[T]]](Future successful None)(savedOrElse)
    }
  }

  def getAll[T](classTag: Class[T], keys: JavaList[String]): CompletionStage[JavaList[Optional[T]]] = {
    async { implicit context =>
      internal.getAll(keys.asScala)(classTag).map(_.map(_.asJava).asJava)
    }
  }

  def removeAll() = internal.invalidate().asJava

  def exists(key: String): CompletionStage[java.lang.Boolean] = {
    async { implicit context =>
      internal.exists(key).map(Boolean.box)
    }
  }

  def matching(pattern: String): CompletionStage[JavaList[String]] = {
    async { implicit context =>
      internal.matching(pattern).map(_.asJava)
    }
  }

  def setIfNotExists(key: String, value: Any): CompletionStage[java.lang.Boolean] = {
    async { implicit context =>
      Future.from(
        internal.setIfNotExists(key, value).map(Boolean.box),
        internal.setIfNotExists(classTagKey(key), classTagOf(value)).map(Boolean.box)
      ).map(_.head)
    }
  }

  def setIfNotExists(key: String, value: Any, expiration: Int): CompletionStage[java.lang.Boolean] = {
    async { implicit context =>
      Future.from(
        internal.setIfNotExists(key, value, expiration.seconds).map(Boolean.box),
        internal.setIfNotExists(classTagKey(key), classTagOf(value), expiration.seconds).map(Boolean.box)
      ).map(_.head)
    }
  }

  def setAll(keyValues: KeyValue*): CompletionStage[Done] = {
    async { implicit context =>
      internal.setAll(
        keyValues.flatMap { kv =>
          Iterable((kv.key, kv.value), (classTagKey(kv.key), classTagOf(kv.value)))
        }: _*
      )
    }
  }

  def setAllIfNotExist(keyValues: KeyValue*): CompletionStage[java.lang.Boolean] = {
    async { implicit context =>
      internal.setAllIfNotExist(
        keyValues.flatMap(kv => Seq((kv.key, kv.value), (classTagKey(kv.key), classTagOf(kv.value)))): _*
      ).map(Boolean.box)
    }
  }

  def append(key: String, value: String): CompletionStage[Done] = {
    async { implicit context =>
      Future.from(
        internal.append(key, value),
        internal.setIfNotExists(classTagKey(key), classTagOf(value))
      ).asDone
    }
  }

  def append(key: String, value: String, expiration: Int): CompletionStage[Done] = {
    async { implicit context =>
      Future.from(
        internal.append(key, value, expiration.seconds),
        internal.setIfNotExists(classTagKey(key), classTagOf(value), expiration.seconds)
      ).asDone
    }
  }

  def expire(key: String, expiration: Int): CompletionStage[Done] = {
    async { implicit context =>
      Future.from(
        internal.expire(key, expiration.seconds),
        internal.expire(classTagKey(key), expiration.seconds)
      ).asDone
    }
  }

  def expiresIn(key: String): CompletionStage[Optional[java.lang.Long]] = {
    async { implicit context =>
      internal.expiresIn(key).map(_.map(_.toSeconds).map(Long.box).asJava)
    }
  }

  def remove(key1: String, key2: String, keys: String*): CompletionStage[Done] = {
    removeAllKeys(Seq(key1, key2) ++ keys: _*)
  }

  def removeAllKeys(keys: String*): CompletionStage[Done] = {
    async { implicit context =>
      internal.removeAll(keys.flatMap(_.withClassTag): _*)
    }
  }

  def removeMatching(pattern: String): CompletionStage[Done] = {
    async { implicit context =>
      Future.from(
        internal.removeMatching(pattern),
        internal.removeMatching(classTagKey(pattern))
      ).asDone
    }
  }

  def increment(key: String, by: java.lang.Long): CompletionStage[java.lang.Long] = {
    async { implicit context =>
      internal.increment(key, by).map(Long.box)
    }
  }

  def decrement(key: String, by: java.lang.Long): CompletionStage[java.lang.Long] = {
    async { implicit context =>
      internal.decrement(key, by).map(Long.box)
    }
  }

  def list[T](key: String, classTag: Class[T]): AsyncRedisList[T] = new RedisListJavaImpl(internal.list[T](key)(classTag))

  def set[T](key: String, classTag: Class[T]): AsyncRedisSet[T] = new RedisSetJavaImpl(internal.set[T](key)(classTag))

  def map[T](key: String, classTag: Class[T]): AsyncRedisMap[T] = new RedisMapJavaImpl(internal.map[T](key)(classTag))
}
