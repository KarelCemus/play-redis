package play.api.cache.redis

import scala.concurrent.duration._
import scala.language.implicitConversions

/**
  * @author Karel Cemus
  */
package object configuration {

  implicit private[ configuration ] def implicitlyAny2Some[ T ]( value: T ): Option[ T ] = Some( value )

  private[ configuration ] val defaultCacheName = "play"
  private[ configuration ] val localhost = "localhost"
  private[ configuration ] val defaultPort = 6379

  private[ configuration ] val defaults = RedisSettingsTest( "akka.actor.default-dispatcher", "lazy", RedisTimeouts( 1.second ), "log-and-default", "standalone" )
}
