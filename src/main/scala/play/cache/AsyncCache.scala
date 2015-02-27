package play.cache

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

import play.api._
import play.cache.api._

/**
 * <p>Object to cache access</p>
 */
class Cache20 extends CacheAPI20 {

  protected var internal: CacheAPI20 = null

  /** looks up ExtendedCachePlugin and reassigns it into internal variable */
  def reload( ): CacheAPI20 = {
    // reload cache api
    internal = Play.current.plugin[ CachePlugin20 ] match {
      case Some( plugin ) => plugin.api
      case None => throw new Exception( "There is no cache plugin registered. Make sure at least one play.plugin.redis.ExtendedRedisCachePlugin implementation is enabled." )
    }
    internal
  }

  /** Retrieve a value from the cache for the given type */
  override def get[ T ]( key: String )( implicit classTag: ClassTag[ T ] ): Future[ Option[ T ] ] = internal.get( key )( classTag )

  /** Retrieve a value from the cache, or set it from a default function. */
  override def getOrElse[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ T ] = internal.getOrElse( key, expiration )( orElse )( classTag )

  /** Set a value into the cache.  */
  override def set[ T ]( key: String, value: T, expiration: Option[ Int ] = None )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] = internal.set( key, value, expiration )( classTag )

  /** Retrieve a value from the cache, or set it from a default function. */
  override def setIfNotExists[ T ]( key: String, expiration: Option[ Int ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] = internal.setIfNotExists( key, expiration )( orElse )( classTag )

  /** remove key from cache */
  override def remove( key: String ): Future[ Try[ String ] ] = internal.remove( key )

  /** invalidate cache */
  override def invalidate( ): Future[ Try[ String ] ] = internal.invalidate( )
}

object AsyncCache extends Cache20