package play.api.cache.redis

import scala.collection.convert.{DecorateAsJava, DecorateAsScala}

private[redis] trait JavaCompatibilityBase extends DecorateAsScala with DecorateAsJava

private[redis] object JavaCompatibilityBase extends JavaCompatibilityBase

