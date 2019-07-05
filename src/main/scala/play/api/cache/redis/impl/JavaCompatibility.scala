package play.api.cache.redis.impl

import scala.collection.convert.{DecorateAsJava, DecorateAsScala}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.reflect.ClassTag

import play.api.Environment

import akka.Done

private[impl] object JavaCompatibility extends DecorateAsScala with DecorateAsJava {
  import scala.compat.java8.{FutureConverters, OptionConverters}

  type CompletionStage[T] = java.util.concurrent.CompletionStage[T]
  type Callable[T] = java.util.concurrent.Callable[T]
  type Optional[T] = java.util.Optional[T]
  type JavaMap[K, V] = java.util.Map[K, V]
  type JavaList[T] = java.util.List[T]
  type JavaSet[T] = java.util.Set[T]

  object JavaList {
    def apply[T](values: T*): JavaList[T] = {
      val list = new java.util.ArrayList[T]()
      list.addAll(values.asJava)
      list
    }
  }

  implicit class Java8Stage[T](val future: Future[T]) extends AnyVal {
    @inline def asJava: CompletionStage[T] = FutureConverters.toJava(future)
    @inline def asDone(implicit ec: ExecutionContext): Future[Done] = future.map(_ => Done)
  }

  implicit class Java8Callable[T](val f: () => T) extends AnyVal {
    @inline def asJava: Callable[T] = new Callable[T] {
      def call(): T = f()
    }
  }

  implicit class Java8Optional[T](val option: Option[T]) extends AnyVal {
    @inline def asJava: Optional[T] = OptionConverters.toJava(option)
  }

  implicit class ScalaCompatibility[T](val future: CompletionStage[T]) extends AnyVal {
    @inline def asScala: Future[T] = FutureConverters.toScala(future)
  }

  implicit class RichFuture(val future: Future.type) extends AnyVal {
    @inline def from[T](futures: Future[T]*)(implicit ec: ExecutionContext): Future[Seq[T]] = future.sequence(futures.toSeq)
  }

  @inline implicit def class2tag[T](classOf: Class[T]): ClassTag[T] = ClassTag(classOf)

  @inline def async[T](doAsync: ExecutionContext => Future[T])(implicit runtime: RedisRuntime): CompletionStage[T] = {
    doAsync {
      // save the HTTP context if any and restore it later for orElse clause
      play.core.j.HttpExecutionContext.fromThread(runtime.context)
    }.asJava
  }

  @inline def classTagKey(key: String): String = s"classTag::$key"

  @inline def classTagOf(value: Any): String = {
    if (value == null) "" else value.getClass.getCanonicalName
  }

  @inline def classTagFrom[T](tag: String)(implicit environment: Environment): ClassTag[T] = {
    if (tag == "") ClassTag.Null.asInstanceOf[ClassTag[T]]
    else ClassTag(classTagNameToClass(tag, environment))
  }

  implicit class CacheKey(val key: String) extends AnyVal {
    @inline def withClassTag: Seq[String] = Seq(key, classTagKey(key))
  }

  // $COVERAGE-OFF$
  /** java primitives are serialized into their type names instead of classes */
  def classTagNameToClass(name: String, environment: Environment): Class[_] = name match {
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
