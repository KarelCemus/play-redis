package play.api.cache.redis.impl

import java.util.Optional
import java.util.concurrent.{Callable, CompletionStage}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import play.api.Environment
import play.api.cache.redis._

/**
  * Implements Play Java version of play.api.CacheApi
  *
  * This acts as an adapter to Play Scala CacheApi, because Java Api is slightly different than Scala Api
  */
private[impl] class JavaRedis(internal: CacheAsyncApi, environment: Environment)(implicit runtime: RedisRuntime) extends play.cache.AsyncCacheApi {
  import JavaRedis._

  def set(key: String, value: scala.Any, expiration: Int): CompletionStage[Done] =
    set(key, value, expiration.seconds).toJava

  def set(key: String, value: scala.Any): CompletionStage[Done] =
    set(key, value, Duration.Inf).toJava

  def set(key: String, value: scala.Any, duration: Duration): Future[Done] = {
    import dsl._

    Future.sequence(
      Seq(
        // set the value
        internal.set(key, value, duration),
        // and set its type to be able to read it
        internal.set(s"classTag::$key", classTagOf(value), duration)
      )
    ).map {
        case Seq(done, _) => done
      }
  }

  def remove(key: String): CompletionStage[Done] = internal.remove(key).toJava

  def get[T](key: String): CompletionStage[T] = getOrElse[T](key, None)

  def getOptional[T](key: String): CompletionStage[Optional[T]] = {
    async { implicit context =>
      getOrElseOption[T](key, None).map(opt => Optional.ofNullable[T](play.libs.Scala.orNull(opt))).toJava
    }
  }

  def getOrElseUpdate[T](key: String, block: Callable[CompletionStage[T]]): CompletionStage[T] =
    getOrElse[T](key, Some(block))

  def getOrElseUpdate[T](key: String, block: Callable[CompletionStage[T]], expiration: Int): CompletionStage[T] =
    getOrElse[T](key, Some(block), duration = expiration.seconds)

  def getOrElse[T](key: String, callable: Option[Callable[CompletionStage[T]]], duration: Duration = Duration.Inf): CompletionStage[T] = {
    async { implicit context =>
      getOrElseOption(key, callable, duration).map[T](play.libs.Scala.orNull).toJava
    }
  }

  private def getOrElseOption[T](key: String, callable: Option[Callable[CompletionStage[T]]], duration: Duration = Duration.Inf)(implicit context: ExecutionContext): Future[Option[T]] = {
    // get the tag and decode it
    def getClassTag = internal.get[String](s"classTag::$key")
    def decodedClassTag(tag: Option[String]) = tag.map(classTagFrom[T])
    // if tag is defined, get Option[ value ] otherwise None
    def getValue = getClassTag.map(decodedClassTag).flatMap {
      case Some(ClassTag.Null) => Future.successful(Some(null.asInstanceOf[T]))
      case Some(tag)           => internal.get[T](key)(tag)
      case None                => Future.successful(None)
    }
    // compute or else and save it into cache
    def orElse(callable: Callable[CompletionStage[T]]) = callable.call().toScala
    def saveOrElse(value: T) = set(key, value, duration)
    def savedOrElse(callable: Callable[CompletionStage[T]]) = orElse(callable).flatMap {
      value => runtime.invocation.invoke(saveOrElse(value), Some(value))
    }

    getValue.flatMap {
      case Some(value) => Future.successful(Some(value))
      case None        => callable.fold[Future[Option[T]]](Future successful None)(savedOrElse)
    }
  }

  def removeAll() = internal.invalidate().toJava

  protected def classTagOf(value: Any): String = {
    if (value == null) "" else value.getClass.getCanonicalName
  }

  protected def classTagFrom[T](tag: String): ClassTag[T] = {
    if (tag == "") ClassTag.Null.asInstanceOf[ClassTag[T]]
    else ClassTag(classTagNameToClass(tag, environment))
  }

  @inline
  protected def async[T](doAsync: ExecutionContext => T): T = {
    doAsync {
      // save the HTTP context if any and restore it later for orElse clause
      play.core.j.HttpExecutionContext.fromThread(runtime.context)
    }
  }
}

private[impl] object JavaRedis {
  import scala.compat.java8.FutureConverters

  private[impl] implicit class Java8Compatibility[T](val future: Future[T]) extends AnyVal {
    @inline def toJava: CompletionStage[T] = FutureConverters.toJava(future)
  }

  private[impl] implicit class ScalaCompatibility[T](val future: CompletionStage[T]) extends AnyVal {
    @inline def toScala: Future[T] = FutureConverters.toScala(future)
  }

  // $COVERAGE-OFF$
  /** java primitives are serialized into their type names instead of classes */
  private[impl] def classTagNameToClass(name: String, environment: Environment): Class[_] = name match {
    case "boolean[]" => classOf[Array[java.lang.Boolean]]
    case "byte[]"    => classOf[Array[java.lang.Byte]]
    case "char[]"    => classOf[Array[java.lang.Character]]
    case "short[]"   => classOf[Array[java.lang.Short]]
    case "int[]"     => classOf[Array[java.lang.Integer]]
    case "long[]"    => classOf[Array[java.lang.Long]]
    case "float[]"   => classOf[Array[java.lang.Float]]
    case "double[]"  => classOf[Array[java.lang.Double]]
    case clazz       => environment.classLoader.loadClass(clazz)
  }
  // $COVERAGE-ON$
}
