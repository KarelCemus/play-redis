package play.api.cache.redis.util

import com.typesafe.config.Config

object config {
  implicit class ConfigExt(conf: Config) {

    import scala.reflect.runtime.universe._

    def get[T: TypeTag](path: String): Option[T] = {
      if (conf.hasPathOrNull(path)) {
        if (conf.getIsNull(path)) {
          None
        } else {
          typeOf[T] match {
            case t if t =:= typeOf[String] =>
              Some(conf.getString(path).asInstanceOf[T])
            case t if t =:= typeOf[Int] =>
              Some(conf.getInt(path).asInstanceOf[T])
            case _ =>
              None
          }
        }
      } else {
        None
      }
    }
  }
}
