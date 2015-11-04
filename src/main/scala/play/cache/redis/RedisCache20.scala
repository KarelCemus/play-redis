//package play.cache.redis
//
//import javax.inject._
//
//import scala.concurrent.duration.Duration
//import scala.concurrent.duration.Duration._
//import scala.concurrent.duration._
//import scala.concurrent.{ExecutionContext, Future}
//import scala.language.implicitConversions
//import scala.reflect.ClassTag
//import scala.util._
//
//import play.api._
//import play.api.libs.concurrent.Akka
//import play.cache.api._
//
//import akka.serialization._
//import com.typesafe.config.ConfigFactory
//
///**
// * <p>Implementation of ExtendedCacheAPI using Akka serializers and Redis connector.</p>
// */
//@Singleton
//class RedisCache20 @Inject() ( protected val cacheAPI: CacheAPI )( implicit app: Application ) extends CacheAPI20 {
//
//  protected val log = Logger( "play.cache.redis" )
//
//  protected def config = ConfigFactory.load( ).getConfig( "play.cache.redis" )
//
//  protected val expiration = config.getConfig( "expiration" )
//
//  implicit def asFiniteDuration(d: java.time.Duration) = scala.concurrent.duration.Duration.fromNanos(d.toNanos)
//
//  /** by default, values expires in .. */
//  protected val DefaultExpiration: Duration = expiration.getDuration( "default" )
//
//  /** default invocation context of all cache commands */
//  protected implicit var context: ExecutionContext = Akka.system.dispatchers.lookup( config.getString( "dispatcher" ) )
//
//  /** in production mode serializer is just one, in development mode it is reloaded */
//  private val serializer: Serialization = SerializationExtension( Akka.system )
//
//  /** encode given object to string */
//  protected def encode[ T ]( value: T )( implicit classTag: ClassTag[ T ] ): Try[ String ] = value match {
//    // null is special case
//    case null => throw new UnsupportedOperationException( "Null is not supported by redis cache connector." )
//    // AnyVal is not supported by default, have to be implemented manually
//    case v if classTag.runtimeClass.isPrimitive => Success( v.toString )
//    // AnyRef is supported by Akka serializers, but it does not consider classTag, thus it is done manually
//    case anyRef: AnyRef =>
//      // serialize the object with the respect to the class tag
//      Try( serializer.findSerializerFor( classTag.runtimeClass ).toBinary( anyRef ) ).map( toBase64 )
//    // if none of the cases above matches, throw an exception
//    case _ => throw new UnsupportedOperationException( s"Type ${value.getClass} is not supported by redis cache connector." )
//  }
//
//  /** encode given value and handle error if occurred */
//  private def encode[ T ]( key: String, value: T )( implicit classTag: ClassTag[ T ] ): Try[ String ] =
//    encode( value ) recoverWith {
//      case ex =>
//        log.error( s"Serialization for key '$key' failed. Cause: '$ex'." )
//        Failure( ex )
//    }
//
//  /** produces BASE64 encoded string from an array of bytes */
//  private def toBase64( bytes: Array[ Byte ] ): String = new sun.misc.BASE64Encoder( ).encode( bytes )
//
//  /** decode given value from string to object */
//  protected def decode[ T ]( value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] = ( value match {
//    // AnyVal is not supported by default, have to be implemented manually
//    case "" => Success( null )
//    case boolean if classTag == ClassTag.Boolean => Try( boolean.toBoolean )
//    case byte if classTag == ClassTag.Byte => Try( byte.toByte )
//    case char if classTag == ClassTag.Char => Try( char.charAt( 0 ) )
//    case short if classTag == ClassTag.Short => Try( short.toShort )
//    case int if classTag == ClassTag.Int => Try( int.toInt )
//    case long if classTag == ClassTag.Long => Try( long.toLong )
//    case float if classTag == ClassTag.Float => Try( float.toFloat )
//    case double if classTag == ClassTag.Double => Try( double.toDouble )
//    // AnyRef is supported by Akka serializers
//    case anyRef =>
//      serializer.deserialize( toBinary( anyRef ), classTag.runtimeClass )
//  } ).asInstanceOf[ Try[ T ] ]
//
//  /** decode given value and handle error if occurred */
//  private def decode[ T ]( key: String, value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] =
//    decode( value ) recoverWith {
//      case ex =>
//        log.error( s"Deserialization for key '$key' failed. Cause: '$ex'." )
//        Failure( ex )
//    }
//
//  /** consumes BASE64 string and returns array of bytes */
//  private def toBinary( base64: String ): Array[ Byte ] = new sun.misc.BASE64Decoder( ).decodeBuffer( base64 )
//
//  /** Retrieve a value from the cache for the given type */
//  override def get[ T ]( key: String )( implicit classTag: ClassTag[ T ] ): Future[ Option[ T ] ] =
//    cacheAPI.get( key ).map( _.flatMap( decode[ T ]( key, _ ).toOption ) )
//
//  /** Retrieve a value from the cache, or set it from a default function. */
//  override def getOrElse[ T ]( key: String, expiration: Option[ Duration ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ T ] =
//    get( key ) flatMap {
//      // cache hit, return the unwrapped value
//      case Some( value ) => Future.successful( value )
//      // cache miss, compute the value, store it into cache and return the value
//      case None => orElse flatMap ( future => set( key, future, expiration ).map( _ => future ) )
//    }
//
//  /** Set a value into the cache.  */
//  override def set[ T ]( key: String, value: T, expiration: Option[ Duration ] = None )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] =
//    if ( value == null ) remove( key )
//    else encode( key, value ) match {
//      case Success( string ) => cacheAPI.set( key, string, expiration.getOrElse( duration( key ) ) )
//      case Failure( ex ) => Future.successful( Failure( ex ) )
//    }
//
//  /** Retrieve a value from the cache, or set it from a default function. */
//  override def setIfNotExists[ T ]( key: String, expiration: Option[ Duration ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] =
//    cacheAPI.exists( key ) flatMap {
//      // hit, value exists, do nothing
//      case true => Future.successful( Success( key ) )
//      // miss, value is not set
//      case false => orElse flatMap ( set( key, _, expiration ) )
//    }
//
//  /** remove key from cache */
//  override def remove( keys: String* ): Future[ Try[ String ] ] = cacheAPI.remove( keys: _* )
//
//  /** invalidate cache */
//  override def invalidate( ): Future[ Try[ String ] ] = cacheAPI.invalidate( )
//
//
//  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration */
//  override def expire( key: String, expiration: Duration ): Unit = cacheAPI.expire( key, expiration )
//
//  protected def duration( key: String ): Duration = {
//    // drop prefix from 'prefix:key' and look up the key in the configuration
//    lookUp( key.drop( key.lastIndexOf( ':' ) + 1 ) )
//  }
//
//  /** computes expiration for given key, possibly uses default value */
//  @scala.annotation.tailrec
//  private def lookUp( key: String ): Duration = key match {
//    // look up configuration "play.cache.redis.expiration.key" or "play.cache.redis.expiration.partOfTheKey"
//    case hit if expiration.hasPath( hit ) => expiration.getDuration( hit )
//    // key is not in configuration, drop its appendix and try it again
//    case miss if key.lastIndexOf( '.' ) > -1 => lookUp( miss.substring( 0, miss.lastIndexOf( '.' ) ) )
//    // no specific configuration for this key, use default expiration
//    case _ => DefaultExpiration
//  }
//}
