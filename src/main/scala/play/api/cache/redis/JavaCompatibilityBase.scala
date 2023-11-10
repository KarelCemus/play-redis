package play.api.cache.redis

import scala.collection.convert.{AsJavaExtensions, AsScalaExtensions}

private[redis] trait JavaCompatibilityBase extends AsJavaExtensions with AsScalaExtensions

private[redis] object JavaCompatibilityBase extends JavaCompatibilityBase
