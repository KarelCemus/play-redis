package play.api.cache.redis.impl

import play.api.cache.redis.CacheApi

/** Java version of play.api.CacheApi */
trait JavaCacheApi extends play.cache.CacheApi {

  import java.util.concurrent.Callable

  import scala.concurrent.duration._
  import scala.reflect.ClassTag

  protected def internal: CacheApi

  protected def classLoader: ClassLoader

  override def set( key: String, value: scala.Any, duration: Int ): Unit =
    set( key, value, duration.seconds )

  override def set( key: String, value: scala.Any ): Unit =
    set( key, value, Duration.Inf )

  def set( key: String, value: scala.Any, duration: Duration ): Unit = {
    internal.set( key, value, duration )
    internal.set( s"classTag::$key", if ( value == null ) "" else value.getClass.getCanonicalName, duration )
  }

  override def get[ T ]( key: String ): T =
    getOrElse[ T ]( key, None )

  override def getOrElse[ T ]( key: String, callable: Callable[ T ], duration: Int ): T =
    getOrElse[ T ]( key, Some( callable ), duration.seconds )

  override def getOrElse[ T ]( key: String, callable: Callable[ T ] ): T =
    getOrElse[ T ]( key, Some( callable ), Duration.Inf )

  def getOrElse[ T ]( key: String, callable: Option[ Callable[ T ] ], duration: Duration = 0.seconds ): T =
    play.libs.Scala.orNull {
      // load classTag
      internal.get[ String ]( s"classTag::$key" ).map[ ClassTag[ T ] ] {
        name => if ( name == null ) ClassTag.Null.asInstanceOf[ ClassTag[ T ] ] else ClassTag( classLoader.loadClass( name ) )
      }.flatMap {
        implicit tag => internal.get[ T ]( key )
      }.orElse {
        // value not found, store new value and return it
        callable.map( _.call( ) ).map { value =>
          set( key, value, duration )
          value
        }
      }
    }

  override def remove( key: String ): Unit =
    internal.remove( key )
}
