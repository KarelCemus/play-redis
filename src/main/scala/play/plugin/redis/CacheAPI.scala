package play.plugin.redis

import scala.concurrent._
import scala.util.Try

import play.api.Logger

/**
 * <p>Non-blocking cache API, inspired by basic Play [[play.api.cache.CacheAPI]].</p>
 */
trait CacheAPI {

  protected val log: Logger

  /** Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  def get( key: String )( implicit context: ExecutionContext ): Future[ Option[ String ] ]

  /** Determines whether value exists in cache.
    *
    * @param key cache storage key
    * @return record existence, true if exists, otherwise false
    */
  def exists( key: String )( implicit context: ExecutionContext ): Future[ Boolean ]

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key cache storage key
    * @param value value to store
    * @param expiration record duration in seconds
    * @return operation success
    */
  def set( key: String, value: String, expiration: Int )( implicit context: ExecutionContext ): Future[ Try[ String ] ]

  /** Remove a value from the cache
    * @param key cache storage key
    * @return operation success
    */
  def remove( key: String )( implicit context: ExecutionContext ): Future[ Try[ String ] ]

  /** Remove all keys in cache
    *
    * @return operation success
    */
  def invalidate( )( implicit context: ExecutionContext ): Future[ Try[ String ] ]
}
