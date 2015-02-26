package play.cache.redis

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util._

import play.api._
import play.api.libs.concurrent.Akka
import play.cache.api._

import akka.serialization.{Serialization, SerializationExtension}
import com.typesafe.config.ConfigFactory

/**
 * <p>Implementation of ExtendedCacheAPI using Akka serializers and Redis connector.</p>
 */
class RedisCache20( protected val cacheAPI: CacheAPI )( implicit app: Application ) extends CacheAPI20 {

  protected val log = Logger( "play.redis" )

  protected def config = ConfigFactory.load( ).getConfig( "play.redis" )

  protected val expiration = config.getConfig( "expiration" )

  /** by default, values expires in .. */
  protected val DefaultExpiration: Int = expiration.getInt( "default" )

  /** default invocation context of all cache commands */
  protected implicit var context: ExecutionContext = Akka.system.dispatchers.lookup( config.getString( "dispatcher" ) )

  /** in production mode serializer is just one, in development mode it is reloaded */
  private val serializer: Serialization = SerializationExtension( Akka.system )

  /** encode given object to string */
  protected def encode[ T ]( value: T )( implicit classTag: ClassTag[ T ] ): Try[ String ] =
    serializer.serialize( value.asInstanceOf[ AnyRef ] ).map( toBase64 )

  /** encode given value and handle error if occurred */
  private def encode[ T ]( key: String, value: T )( implicit classTag: ClassTag[ T ] ): Try[ String ] =
    encode( value ) recoverWith {
      case ex =>
        log.error( s"Serialization for key '$key' failed. Cause: '$ex'." )
        Failure( ex )
    }

  /** produces BASE64 encoded string from an array of bytes */
  private def toBase64( bytes: Array[ Byte ] ): String = new sun.misc.BASE64Encoder( ).encode( bytes )

  /** decode given value from string to object */
  protected def decode[ T ]( value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] =
    serializer.deserialize( toBinary( value ), classTag.runtimeClass.asInstanceOf[ Class[ T ] ] )

  /** decode given value and handle error if occurred */
  private def decode[ T ]( key: String, value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] =
    decode( value ) recoverWith {
      case ex =>
        log.error( s"Deserialization for key '$key' failed. Cause: '$ex'." )
        Failure( ex )
    }

  /** consumes BASE64 string and returns array of bytes */
  private def toBinary( base64: String ): Array[ Byte ] = new sun.misc.BASE64Decoder( ).decodeBuffer( base64 )

  /** Retrieve a value from the cache for the given type */
  override def get[ T ]( key: String )( implicit classTag: ClassTag[ T ] ): Future[ Option[ T ] ] =
    cacheAPI.get( key ).map( _.flatMap( decode[ T ]( key, _ ).toOption ) )

  /** Retrieve a value from the cache, or set it from a default function. */
  override def getOrElse[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ T ] =
    get( key ) flatMap {
      // cache hit, return the unwrapped value
      case Some( value ) => Future.successful( value )
      // cache miss, compute the value, store it into cache and return the value
      case None => orElse flatMap ( future => set( key, future, expiration ).map( _ => future ) )
    }

  /** Set a value into the cache.  */
  override def set[ T ]( key: String, value: T, expiration: Option[ Int ] = None )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] =
    encode( key, value ) match {
      case Success( string ) => cacheAPI.set( key, string, expiration.getOrElse( duration( key ) ) )
      case Failure( ex ) => Future.successful( Failure( ex ) )
    }

  /** Retrieve a value from the cache, or set it from a default function. */
  override def setIfNotExists[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] =
    cacheAPI.exists( key ) flatMap {
      // hit, value exists, do nothing
      case true => Future.successful( Success( key ) )
      // miss, value is not set
      case false => orElse flatMap ( set( key, _, expiration ) )
    }

  /** remove key from cache */
  override def remove( key: String ): Future[ Try[ String ] ] = cacheAPI.remove( key )

  /** invalidate cache */
  override def invalidate( ): Future[ Try[ String ] ] = cacheAPI.invalidate( )

  /** computes expiration for given key, possibly uses default value */
  @scala.annotation.tailrec
  protected final def duration( key: String ): Int = key match {
    // look up configuration "play.redis.expiration.key" or "play.redis.expiration.partOfTheKey"
    case hit if expiration.hasPath( hit ) => expiration.getInt( hit )
    // key is not in configuration, drop its appendix and try it again
    case miss if key.lastIndexOf( '.' ) > -1 => duration( miss.substring( 0, miss.lastIndexOf( '.' ) ) )
    // no specific configuration for this key, use default expiration
    case _ => DefaultExpiration
  }
}
