package play.cache.redis

import java.util.Date
import javax.inject._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

import play.api.cache.CacheApi

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

/**
 * CacheAPI implementation for backward compatibility with play.api.cache.CacheAPI
 */
@Singleton
class RedisCacheAdapter @Inject() ( redis: play.cache.api.CacheAPI20 ) extends CacheApi {

  implicit def asFiniteDuration(d: java.time.Duration) = scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  val timeout: Duration = ConfigFactory.load( ).getDuration( "play.cache.redis.timeout" )

  override def set( key: String, any: Any, expiration: Duration = Duration.Inf ): Unit = any match {

    case value: Boolean => store( key, value, expiration )

    case value: Byte => store( key, value, expiration )
    case value: Short => store( key, value, expiration )
    case value: Int => store( key, value, expiration )
    case value: Long => store( key, value, expiration )

    case value: Float => store( key, value, expiration )
    case value: Double => store( key, value, expiration )

    case value: Char => store( key, value, expiration )
    case value: String => store( key, value, expiration )

    case value: Date => store( key, value, expiration )
    case value: DateTime => store( key, value, expiration )

    case value: AnyRef => store( key, value, expiration )( ClassTag( value.getClass ) )

    case _ => throw new UnsupportedOperationException(
      s"""
       |RedisAdapter for play.api.Cache supports only limited amount of
       |types. ${ any.getClass.getName } is not supported. Please use
       |play.cache.api.CacheAPI instead.
       |""".stripMargin
    )
  }

  override def get[ T: ClassTag ]( key: String ): Option[ T ] =
    this ! redis.get[ ClassTag[ T ] ]( s"type:$key" ) flatMap { classTag =>
      // load class tag and if found try to load and convert the value
      this ! redis.get( key )( classTag )
    }

  override def remove( key: String ): Unit = this ! redis.remove( key )

  protected def ![ T ]( future: Future[ T ] ): T = Await.result( future, timeout )

  protected def store[ T ]( key: String, value: T, expiration: Duration )( implicit classTag: ClassTag[ T ] ): Any = {
    val expire = Some( expiration ).filter( _ != 0 )
    this ! redis.set( key, value, expire )
    this ! redis.set( s"type:$key", classTag, expire )
  }

  override def getOrElse[ A: ClassTag ]( key: String, expiration: Duration )( orElse: => A ): A = ??? // TODO
}
