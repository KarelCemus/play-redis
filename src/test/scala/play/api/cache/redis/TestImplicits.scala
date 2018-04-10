package play.api.cache.redis

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util._

import play.api.cache.redis.configuration._

/**
  * @author Karel Cemus
  */
private[ redis ] trait TestImplicits {

  val defaultCacheName = "play"
  val localhost = "localhost"
  val defaultPort = 6379

  val defaults = RedisSettingsTest( "akka.actor.default-dispatcher", "lazy", RedisTimeouts( 1.second ), "log-and-default", "standalone" )

  val defaultInstance = RedisStandalone( defaultCacheName, RedisHost( localhost, defaultPort ), defaults )

  implicit def implicitlyAny2Some[ T ]( value: T ): Option[ T ] = Some( value )

  implicit def implicitlyAny2future[ T ]( value: T ): Future[ T ] = Future.successful( value )

  implicit def implicitlyAny2success[ T ]( value: T ): Try[ T ] = Success( value )

  implicit def implicitlyAny2failure( ex: Throwable ): Try[ Nothing ] = Failure( ex )
}
