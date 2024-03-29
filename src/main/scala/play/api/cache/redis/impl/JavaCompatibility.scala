package play.api.cache.redis.impl

import org.apache.pekko.Done
import play.api.Environment
import play.api.cache.redis._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

private[impl] object JavaCompatibility extends JavaCompatibilityBase {
  import scala.jdk.javaapi.{FutureConverters, OptionConverters}

  type CompletionStage[T] = java.util.concurrent.CompletionStage[T]
  type Callable[T] = java.util.concurrent.Callable[T]
  type Optional[T] = java.util.Optional[T]
  type JavaMap[K, V] = java.util.Map[K, V]
  type JavaList[T] = java.util.List[T]
  type JavaSet[T] = java.util.Set[T]

  object JavaList {

    def apply[T](values: T*): JavaList[T] = {
      val list = new java.util.ArrayList[T]()
      val _ = list.addAll(values.asJava)
      list
    }

  }

  implicit class Java8Stage[T](private val future: Future[T]) extends AnyVal {
    @inline def asJava: CompletionStage[T] = FutureConverters.asJava(future)
    @inline def asDone(implicit ec: ExecutionContext): Future[Done] = future.map(_ => Done)
  }

  implicit class Java8Callable[T](private val f: () => T) extends AnyVal {
    @inline def asJava: Callable[T] = () => f()
  }

  implicit class Java8Optional[T](private val option: Option[T]) extends AnyVal {
    @inline def asJava: Optional[T] = OptionConverters.toJava(option)
  }

  implicit class ScalaCompatibility[T](private val future: CompletionStage[T]) extends AnyVal {
    @inline def asScala: Future[T] = FutureConverters.asScala(future)
  }

  implicit class RichFuture(private val future: Future.type) extends AnyVal {
    @inline def from[T](futures: Future[T]*)(implicit ec: ExecutionContext): Future[Seq[T]] = future.sequence(futures)
  }

  @inline implicit def class2tag[T](classOf: Class[T]): ClassTag[T] = ClassTag(classOf)

  @inline def async[T](doAsync: ExecutionContext => Future[T])(implicit runtime: RedisRuntime): CompletionStage[T] =
    doAsync {
      // save the HTTP context if any and restore it later for orElse clause
      play.core.j.ClassLoaderExecutionContext.fromThread(runtime.context)
    }.asJava

  @inline def classTagKey(key: String): String = s"classTag::$key"

  @inline def classTagOf(value: Any): String =
    if (Option(value).isEmpty) "null" else value.getClass.getCanonicalName

  @inline def classTagFrom[T](tag: String)(implicit environment: Environment): ClassTag[T] =
    if (tag === "null") ClassTag.Null.asInstanceOf[ClassTag[T]]
    else ClassTag(classTagNameToClass(tag, environment))

  implicit class CacheKey(private val key: String) extends AnyVal {
    @inline def withClassTag: Seq[String] = Seq(key, classTagKey(key))
  }

  // $COVERAGE-OFF$
  /** java primitives are serialized into their type names instead of classes */
  private def classTagNameToClass(name: String, environment: Environment): Class[?] = name match {
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
