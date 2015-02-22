package play.plugin.redis

import scala.concurrent._
import scala.reflect.ClassTag
import scala.util._

import play.api.Logger

/**
 * <p>Advanced non-blocking API. It extends basic [[play.api.cache.CacheAPI]] and adds additional functionality built
 * on its improved interface [[play.plugin.redis.CacheAPI]].</p>
 *
 * @author Karel Cemus
 */
trait ExtendedCacheAPI {

  protected val log: Logger

  /** Implementation of basic API */
  protected val cacheAPI: CacheAPI

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
  def get[ T ]( key: String )( implicit classTag: ClassTag[ T ], context: ExecutionContext ): Future[ Option[ T ] ] =
    cacheAPI.get( key ).map( _.flatMap( decode[ T ]( key, _ ).toOption ) )

  /** Retrieve a value from the cache, or set it from a default function. */
  def getOrElse[ T ]( key: String, expiration: Option[ Int ] )( orElse: () => Future[ T ] )( implicit classTag: ClassTag[ T ], context: ExecutionContext ): Future[ T ] =
    get( key ) flatMap {
      // cache hit, return the unwrapped value
      case Some( value ) => Future.successful( value )
      // cache miss, compute the value, store it into cache and return the value
      case None => orElse( ) flatMap ( future => set( key, future, expiration ).map( _ => future ) )
    }

  /** Set a value into the cache.  */
  def set[ T ]( key: String, value: T, expiration: Option[ Int ] )( implicit classTag: ClassTag[ T ], context: ExecutionContext ): Future[ Try[ String ] ] =
    encode( key, value ) match {
      case Success( string ) => cacheAPI.set( key, string, expiration.getOrElse( duration( key ) ) )
      case Failure( ex ) => Future.successful( Failure( ex ) )
    }

  /** Retrieve a value from the cache, or set it from a default function. */
  def setIfNotExists[ T ]( key: String, expiration: Option[ Int ] )( orElse: () => Future[ T ] )( implicit classTag: ClassTag[ T ], context: ExecutionContext ): Future[ Try[ String ] ] =
    cacheAPI.exists( key ) flatMap {
      // hit, value exists, do nothing
      case true => Future.successful( Success( key ) )
      // miss, value is not set
      case false => orElse( ) flatMap ( set( key, _, expiration ) )
    }

  /** remove key from cache */
  def remove( key: String )( implicit context: ExecutionContext ): Future[ Try[ String ] ] = cacheAPI.remove( key )

  /** invalidate cache */
  def invalidate( )( implicit context: ExecutionContext ): Future[ Try[ String ] ] = cacheAPI.invalidate( )

  /** computes expiration for given key, possibly uses default value */
  protected def duration( key: String ): Int
}
