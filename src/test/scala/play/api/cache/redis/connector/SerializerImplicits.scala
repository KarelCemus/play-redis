package play.api.cache.redis.connector

import scala.reflect.ClassTag

object SerializerImplicits {

  implicit class ValueEncoder(val any: Any) extends AnyVal {
    def encoded(implicit serializer: PekkoSerializer): String = serializer.encode(any).get
  }

  implicit class StringDecoder(val string: String) extends AnyVal {
    def decoded[T: ClassTag](implicit serializer: PekkoSerializer): T = serializer.decode[T](string).get
  }

  implicit class StringOps(val string: String) extends AnyVal {
    def removeAllWhitespaces = string.replaceAll("\\s", "")
  }

  /** Plain test object to be cached */
  case class SimpleObject(key: String, value: Int)
}
