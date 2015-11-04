//package play.cache
//
//import javax.inject._
//
//import scala.concurrent.Future
//import scala.concurrent.duration.Duration
//import scala.reflect.ClassTag
//import scala.util.Try
//
//import play.api._
//import play.cache.api._
//
///**
// * <p>Object to cache access</p>
// */
//@Singleton
//class Cache20 @Inject() ( plugin: CachePlugin20 ) extends CacheAPI20 {
//
//  protected var internal: CacheAPI20 = plugin.api
//
//  /** looks up ExtendedCachePlugin and reassigns it into internal variable */
////  def reload( ): CacheAPI20 = {
////    // reload cache api
////    internal = {
////      val plugin = Play.current.injector.instanceOf[ CachePlugin20 ]
////      plugin.onStart()
////      plugin.api
////    }
////    internal
////  }
//
//  /** Retrieve a value from the cache for the given type */
//  override def get[ T ]( key: String )( implicit classTag: ClassTag[ T ] ): Future[ Option[ T ] ] = internal.get( key )( classTag )
//
//  /** Retrieve a value from the cache, or set it from a default function. */
//  override def getOrElse[ T ]( key: String, expiration: Option[ Duration ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ T ] = internal.getOrElse( key, expiration )( orElse )( classTag )
//
//  /** Set a value into the cache.  */
//  override def set[ T ]( key: String, value: T, expiration: Option[ Duration ] = None )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] = internal.set( key, value, expiration )( classTag )
//
//  /** Retrieve a value from the cache, or set it from a default function. */
//  override def setIfNotExists[ T ]( key: String, expiration: Option[ Duration ] = None )( orElse: => Future[ T ] )( implicit classTag: ClassTag[ T ] ): Future[ Try[ String ] ] = internal.setIfNotExists( key, expiration )( orElse )( classTag )
//
//  /** remove key from cache */
//  override def remove( keys: String* ): Future[ Try[ String ] ] = internal.remove( keys: _* )
//
//  /** invalidate cache */
//  override def invalidate( ): Future[ Try[ String ] ] = internal.invalidate( )
//
//  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration */
//  override def expire( key: String, expiration: Duration ): Unit = internal.expire( key, expiration )
//}
//
////object AsyncCache extends Cache20
