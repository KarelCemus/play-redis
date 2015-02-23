package play.plugin.redis

import scala.reflect.ClassTag
import scala.util._

import play.api._
import play.api.libs.concurrent.Akka

import akka.serialization.{Serialization, SerializationExtension}
import com.typesafe.config.ConfigFactory

/**
 * <p>Implementation of ExtendedCacheAPI using Akka serializers.</p>
 */
class ExtendedRedisCache( protected val cacheAPI: CacheAPI ) extends ExtendedCacheImpl {

  protected def config = ConfigFactory.load( ).getConfig( "play.redis" )

  protected val expiration = config.getConfig( "expiration" )

  /** by default, values expires in .. */
  private val DefaultExpiration: Int = expiration.getInt( "default" )

  /** in production mode serializer is just one, in development mode it is reloaded */
  private var serializer: Serialization = null

  /** produces BASE64 encoded string from an array of bytes */
  private def toBase64( bytes: Array[ Byte ] ): String = new sun.misc.BASE64Encoder( ).encode( bytes )

  /** consumes BASE64 string and returns array of bytes */
  private def toBinary( base64: String ): Array[ Byte ] = new sun.misc.BASE64Decoder( ).decodeBuffer( base64 )

  /** encode given object to string */
  override protected def encode[ T ]( value: T )( implicit classTag: ClassTag[ T ] ): Try[ String ] =
    serializer.serialize( value.asInstanceOf[ AnyRef ] ).map( toBase64 )

  /** decode given value from string to object */
  override protected def decode[ T ]( value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] =
    serializer.deserialize( toBinary( value ), classTag.runtimeClass.asInstanceOf[ Class[ T ] ] )

  def start( )( implicit app: Application ) = serializer = SerializationExtension( Akka.system )

  /** computes expiration for given key, possibly uses default value */
  @scala.annotation.tailrec
  override protected final def duration( key: String ): Int = key match {
    // look up configuration "play.redis.expiration.key" or "play.redis.expiration.partOfTheKey"
    case hit if expiration.hasPath( hit ) => expiration.getInt( hit )
    // key is not in configuration, drop its appendix and try it again
    case miss if key.lastIndexOf( '.' ) > -1 => duration( miss.substring( 0, miss.lastIndexOf( '.' ) ) )
    // no specific configuration for this key, use default expiration
    case _ => DefaultExpiration
  }
}
