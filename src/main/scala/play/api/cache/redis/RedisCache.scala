package play.api.cache.redis

import javax.inject._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util._

import play.api._
import play.api.cache.CacheAsyncApi
import play.api.libs.concurrent.Akka

import akka.util.ByteString
import brando._

/**
 * <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p>
 */
@Singleton
class RedisCache @Inject( )( implicit val application: Application ) extends CacheAsyncApi with Config with AkkaSerializer {

  /** communication module to Redis cache */
  protected val redis: RedisRef = Akka.system actorOf Brando( host, port, database = Some( database ) )

  /** Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  override def get[ T: ClassTag ]( key: String ) = redis ? Request( "GET", key ) map {
    case Success( Some( response: ByteString ) ) =>
      log.trace( s"Hit on key '$key'." )
      decode[ T ]( key, response.utf8String ).toOption
    case Success( None ) =>
      log.debug( s"Miss on key '$key'." )
      None
    case Failure( ex ) =>
      log.error( s"GET command failed for key '$key'.", ex )
      None
    case _ =>
      log.error( s"Unrecognized answer from GET command for key '$key'." )
      None
  }

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    * If the value is null the key is removed from the storage.
    *
    * @param key cache storage key
    * @param value value to store
    * @param expiration record duration in seconds
    * @return promise
    */
  override def set[ T ]( key: String, value: T, expiration: Duration ): Future[ Unit ] =
    if ( value == null ) remove( key )
    else (expiration, encode( key, value )) match {
      case (Duration.Inf, Success( encoded: String )) => setEternally( key, encoded )
      case (temporal: Duration, Success( encoded: String )) => setTemporally( key, encoded, temporal )
      case (_, Failure( ex )) => Future( log.error( s"SET command failed. Encoding of the value for the key '$key' failed.", ex ) )
    }

  /** temporally stores already encoded value into the storage */
  private def setTemporally( key: String, value: String, expiration: Duration ): Future[ Unit ] =
    redis ? Request( "SETEX", key, expiration.toSeconds.toString, value ) map {
      case Success( Some( Ok ) ) => log.debug( s"Set on key '$key' on $expiration seconds." )
      case Success( None ) => log.warn( s"Set on key '$key' failed." )
      case Failure( ex ) => log.error( s"SETEX command failed for key '$key'.", ex )
      case _ => log.error( s"Unrecognized answer from SETEX command for key '$key'." )
    }

  /** eternally stores already encoded value into the storage */
  private def setEternally( key: String, value: String ): Future[ Unit ] =
    redis ? Request( "SET", key, value ) map {
      case Success( Some( Ok ) ) => log.debug( s"Set on key '$key' for infinite seconds." )
      case Success( None ) => log.warn( s"Set on key '$key' failed." )
      case Failure( ex ) => log.error( s"SET command failed for key '$key'.", ex )
      case _ => log.error( s"Unrecognized answer from SET command for key '$key'." )
    }

  /** refreshes expiration time on a given key, useful, e.g., when we want to refresh session duration
    * @param key cache storage key
    * @param expiration new expiration in seconds
    * @return promise
    */
  override def expire( key: String, expiration: Duration ): Future[ Unit ] = ???

  /** Retrieve a value from the cache. If is missing, set default value with
    * given expiration and return the value.
    *
    * @param key cache storage key
    * @param expiration expiration period in seconds.
    * @param orElse The default function to invoke if the value was not found in cache.
    * @return stored or default record, Some if exists, otherwise None
    */
  override def getOrElse[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => T ): Future[ T ] = ???

  /** Retrieve a value from the cache. If is missing, set default value with
    * given expiration and return the value.
    *
    * @param key cache storage key
    * @param expiration expiration period in seconds.
    * @param orElse The default function to invoke if the value was not found in cache.
    * @return stored or default record, Some if exists, otherwise None
    */
  override def getOrFuture[ T: ClassTag ]( key: String, expiration: Duration )( orElse: => Future[ T ] ): Future[ T ] = ???

  /** Remove a value under the given key from the cache
    * @param key cache storage key
    * @return promise
    */
  override def remove( key: String ): Future[ Unit ] = ???

  /** Remove all values from the cache
    * @param key1 cache storage key
    * @param key2 cache storage key
    * @param keys cache storage keys
    * @return promise
    */
  override def remove( key1: String, key2: String, keys: String* ): Future[ Unit ] = ???

  /** Remove all keys in cache
    *
    * @return promise
    */
  override def invalidate( ): Future[ Unit ] = ???

  /** Determines whether value exists in cache.
    *
    * @param key cache storage key
    * @return record existence, true if exists, otherwise false
    */
  override def exists( key: String ): Future[ Boolean ] = ???

  def start( ) = redis ? Request( "PING" ) map { _ =>
    log.info( s"Redis cache started. Actor is connected to $host:$port?database=$database" )
  }

  /** stops running brando actor */
  def stop( ) = {
    Akka.system.stop( redis.actor.actorRef )
    log.info( "Redis cache stopped." )
  }

  //
  //  /** Determines whether exists the value with given key */
  //  def exists( key: String ): Future[ Boolean ] =
  //    redis ? Request( "EXISTS", key ) map {
  //      case Success( Some( 1L ) ) =>
  //        log.trace( s"Key '$key' exists." )
  //        true
  //      case Success( Some( 0L ) ) =>
  //        log.trace( s"Key '$key' doesn't exist." )
  //        false
  //      case Failure( ex ) =>
  //        log.error( s"EXISTS command failed for key '$key'.", ex )
  //        false
  //      case _ =>
  //        log.error( s"Unrecognized answer from EXISTS command for key '$key'." )
  //        false
  //    }
  //
  //
  //  /** Remove all values from the cache */
  //  def remove( keys: String* ): Future[ Try[ String ] ] =
  //    redis ? Request( "DEL", keys: _* ) map {
  //      case Success( Some( 0 ) ) => // Nothing was removed
  //        log.debug( s"Remove on key '$keys' succeeded but nothing was removed." )
  //        Success( "OK" )
  //      case Success( Some( number ) ) => // Some entries were removed
  //        log.debug( s"Remove on key '$keys' removed $number values." )
  //        Success( "OK" )
  //      case Failure( ex ) =>
  //        log.error( s"DEL command failed for key '$keys'.", ex )
  //        Failure( new IllegalStateException( "DEL command failed.", ex ) )
  //      case _ =>
  //        log.error( s"Unrecognized answer from DEL command for key '$keys'." )
  //        Failure( new IllegalStateException( "DEL command failed." ) )
  //    }
  //
  //  /** Remove all keys in cache */
  //  def invalidate( ): Future[ Try[ String ] ] = {
  //    redis ? Request( "FLUSHDB" ) map {
  //      case Success( Some( Ok ) ) => // cache was invalidated
  //        log.info( "Invalidated." )
  //        Success( "OK" )
  //      case Success( None ) => // execution failed
  //        log.warn( "Invalidation failed." )
  //        Failure( new IllegalStateException( "Invalidation failed." ) )
  //      case Failure( ex ) =>
  //        log.error( s"Invalidation failed with an exception.", ex )
  //        Failure( new IllegalStateException( "Invalidation failed.", ex ) )
  //      case _ =>
  //        log.error( s"Unrecognized answer from invalidation command." )
  //        Failure( new IllegalStateException( "Invalidated failed." ) )
  //    }
  //  }
  //
  //
  //  def expire( key: String, expiration: Duration ) = {
  //    redis ? Request( "EXPIRE", key, expiration.toSeconds.toString ) map {
  //      case Success( Some( 1 ) ) => // expiration was set
  //        log.debug( s"Expiration set on key '$key'." )
  //        Success( "OK" )
  //      case Success( Some( 0 ) ) => // Nothing was removed
  //        log.debug( s"Expiration set on key '$key' failed. Key does not exist." )
  //        Success( "OK" )
  //      case Failure( ex ) =>
  //        log.error( s"EXPIRE command failed for key '$key'.", ex )
  //        Failure( new IllegalStateException( "EXPIRE command failed.", ex ) )
  //      case _ =>
  //        log.error( s"Unrecognized answer from EXPIRE command for key '$key'." )
  //        Failure( new IllegalStateException( "EXPIRE command failed." ) )
  //    }
  //  }
  //
}