package play.plugin.redis

import scala.concurrent._
import scala.reflect.ClassTag
import scala.util._

import play.api.Logger

/** Partial implementation of ExtendedCacheAPI in terms of more specific operations */
trait ExtendedCacheImpl extends ExtendedCacheAPI {

  protected val log = Logger( "play.redis" )

  /** Implementation of basic API */
  protected val cacheAPI: CacheAPI

  /** default invocation context of all cache commands */
  protected implicit var context: ExecutionContext

  /** encode given object to string */
  protected def encode[ T ]( value: T )( implicit classTag: ClassTag[ T ] ): Try[ String ]

  /** encode given value and handle error if occurred */
  private def encode[ T ]( key: String, value: T )( implicit classTag: ClassTag[ T ] ): Try[ String ] =
    encode( value ) recoverWith {
      case ex =>
        log.error( s"[Cache] Serialization for key '$key' failed. Cause: '$ex'." )
        Failure( ex )
    }

  /** decode given value from string to object */
  protected def decode[ T ]( value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ]

  /** decode given value and handle error if occurred */
  private def decode[ T ]( key: String, value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] =
    decode( value ) recoverWith {
      case ex =>
        log.error( s"[Cache] Deserialization for key '$key' failed. Cause: '$ex'." )
        Failure( ex )
    }

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
  override def setIfNotExists[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: () => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] =
    cacheAPI.exists( key ) flatMap {
      // hit, value exists, do nothing
      case true => Future.successful( Success( key ) )
      // miss, value is not set
      case false => orElse( ) flatMap ( set( key, _, expiration ) )
    }

  /** remove key from cache */
  override def remove( key: String ): Future[ Try[ String ] ] = cacheAPI.remove( key )

  /** invalidate cache */
  override def invalidate( ): Future[ Try[ String ] ] = cacheAPI.invalidate( )

  /** computes expiration for given key, possibly uses default value */
  protected def duration( key: String ): Int
}
