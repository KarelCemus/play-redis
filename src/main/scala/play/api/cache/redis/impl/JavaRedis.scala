package play.api.cache.redis.impl

import javax.inject.{Inject, Singleton}

import play.api.Environment
import play.api.cache.redis.CacheApi

/**
  * @author Karel Cemus
  */
@Singleton
class JavaRedis @Inject( )( protected val internal: CacheApi, environment: Environment ) extends JavaCacheApi {
  override protected def classLoader: ClassLoader = environment.classLoader
}
