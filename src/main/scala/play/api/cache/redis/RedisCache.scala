package play.api.cache.redis

import javax.inject._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util._

import play.api._
import play.api.libs.concurrent.Akka
import play.api.cache.CacheAsyncApi

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.ByteString
import brando._

/**
 * <p>Implementation of plain API using redis-server cache and Brando connector implementation.</p>
 */
@Singleton
class RedisCache @Inject() ( implicit val application: Application ) extends CacheAsyncApi with Config {

  /** communication module to Redis cache */
  private var redis: RedisRef = null
  
  /** Retrieve a value from the cache.
    *
    * @param key cache storage key
    * @return stored record, Some if exists, otherwise None
    */
  override def get[ T: ClassTag ]( key: String ): Future[ Option[ T ] ] = ???

  /** Set a value into the cache. Expiration time in seconds (0 second means eternity).
    *
    * @param key cache storage key
    * @param value value to store
    * @param expiration record duration in seconds
    * @return promise
    */
  override def set[ T ]( key: String, value: T, expiration: Duration ): Future[ Unit ] = ???

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






  //  /** Retrieve a value from the cache. */
  //  def get( key: String ): Future[ Option[ String ] ] =
  //    redis ? Request( "GET", key ) map {
  //      case Success( Some( response: ByteString ) ) =>
  //        log.trace( s"Hit on key '$key'." )
  //        Some( response.utf8String )
  //      case Success( None ) =>
  //        log.debug( s"Miss on key '$key'." )
  //        None
  //      case Failure( ex ) =>
  //        log.error( s"GET command failed for key '$key'.", ex )
  //        None
  //      case _ =>
  //        log.error( s"Unrecognized answer from GET command for key '$key'." )
  //        None
  //    }
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
  //  /** Set a value into the cache. */
  //  def set( key: String, value: String, expiration: Duration ): Future[ Try[ String ] ] = {
  ////    println( "====== " + redis.actor. )
  //    redis ? Request( "PING" )
  //    val request = if ( expiration.isFinite() ) Request( "SETEX", key, expiration.toSeconds.toString, value ) else Request( "SET", key, value )
  //    redis ? request map {
  //      case Success( Some( Ok ) ) =>
  //        log.debug( s"Set on key '$key' on $expiration seconds." )
  //        Success( "OK" )
  //      case Success( None ) =>
  //        log.warn( s"Set on key '$key' failed." )
  //        Failure( new IllegalStateException( "SETEX command failed." ) )
  //      case Failure( ex ) =>
  //        log.error( s"SETEX command failed for key '$key'.", ex )
  //        Failure( new IllegalStateException( "SETEX command failed.", ex ) )
  //      case _ =>
  //        log.error( s"Unrecognized answer from SETEX command for key '$key'." )
  //        Failure( new IllegalStateException( "SETEX command failed." ) )
  //    }
  //  }
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
  //  def start( ) = {
  //    println("------------------ redis started ------------------------")
  //    val host = config.getString( "host" )
  //    val port = config.getInt( "port" )
  //    val database = config.getInt( "database" )
  //    // create new brando actor
  //    val r = Redis( host, port, database = database )
  //    redis = Akka.system.actorOf( r )
  //    redis ? Request( "PING" )
  //    log.info( s"Started Redis cache actor. Actor is connected to $host:$port?database=$database" )
  //  }
  //
  //  def stop( ) = {
  //    println("-------------- stop ------------")
  //    // stop running brando actor
  //    if ( redis != null ) Akka.system.stop( redis.actor.actorRef )
  //    log.info( "Stopped Redis cache actor." )
  //  }
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