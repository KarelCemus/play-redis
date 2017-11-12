package play.api.cache.redis.impl

import java.util.concurrent.{Callable, CompletionStage}
import javax.inject.{Inject, Singleton}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

import play.api.Environment
import play.api.cache.redis._

/**
  * Implements Play Java version of play.api.CacheApi
  *
  * This acts as an adapter to Play Scala CacheApi, because Java Api is slightly different than Scala Api
  *
  * @author Karel Cemus
  */
@Singleton
private[ impl ] class JavaRedis @Inject()( internal: CacheAsyncApi, environment: Environment, connector: RedisConnector ) extends play.cache.AsyncCacheApi {

  import JavaRedis._

  def set( key: String, value: scala.Any, expiration: Int ): CompletionStage[ Done ] =
    set( key, value, expiration.seconds ).toJava

  def set( key: String, value: scala.Any ): CompletionStage[ Done ] =
    set( key, value, Duration.Inf ).toJava

  def set( key: String, value: scala.Any, duration: Duration ): Future[ Done ] = {
    import connector.context

    Future.sequence(
      Seq(
        // set the value
        internal.set( key, value, duration ),
        // and set its type to be able to read it
        internal.set( s"classTag::$key", if ( value == null ) "" else value.getClass.getCanonicalName, duration )
      )
    ).map {
      case Seq( done, _ ) => done
    }
  }

  def remove( key: String ): CompletionStage[ Done ] = internal.remove( key ).toJava

  def get[ T ]( key: String ): CompletionStage[ T ] =
    getOrElse[ T ]( key, None )

  def getOrElseUpdate[ T ]( key: String, block: Callable[ CompletionStage[ T ] ] ): CompletionStage[ T ] =
    getOrElse[ T ]( key, Some( block ) )

  def getOrElseUpdate[ T ]( key: String, block: Callable[ CompletionStage[ T ] ], expiration: Int ): CompletionStage[ T ] =
    getOrElse[ T ]( key, Some( block ), duration = expiration.seconds )

  def getOrElse[ T ]( key: String, callable: Option[ Callable[ CompletionStage[ T ] ] ], duration: Duration = Duration.Inf ): CompletionStage[ T ] = {
    import play.core.j.HttpExecutionContext
    // save the HTTP context if any and restore it later for orElse clause
    implicit val context = HttpExecutionContext.fromThread( connector.context )
    // get the tag and decode it
    def getClassTag = internal.get[ String ]( s"classTag::$key" )
    def decodeClassTag( name: String ): ClassTag[ T ] = if ( name == null ) ClassTag.Null.asInstanceOf[ ClassTag[ T ] ] else ClassTag( environment.classLoader.loadClass( name ) )
    def decodedClassTag( tag: Option[ String ] ) = tag.map( decodeClassTag )
    // if tag is defined, get Option[ value ] otherwise None
    def getValue = getClassTag.map( decodedClassTag ).flatMap {
      case Some( tag ) => internal.get[ T ]( key )( tag )
      case None => Future.successful( None )
    }
    // compute or else and save it into cache
    def orElse( callable: Callable[ CompletionStage[ T ] ] ) = callable.call().toScala
    def saveOrElse( value: T ) = set( key, value, duration )
    def savedOrElse( callable: Callable[ CompletionStage[ T ] ] ) = orElse( callable ).flatMap { value => saveOrElse( value ).map( _ => Some( value ) ) }

    getValue.flatMap {
      case Some( value ) => Future.successful( Some( value ) )
      case None => callable.fold[ Future[ Option[ T ] ] ]( Future successful None )( savedOrElse )
    }.map {
      play.libs.Scala.orNull( _ )
    }.toJava
  }

  def removeAll( ) = internal.invalidate().toJava
}

private[ impl ] object JavaRedis {
  import scala.compat.java8.FutureConverters

  private implicit class Java8Compatibility[ T ]( val future: Future[ T ] ) extends AnyVal {
    @inline def toJava: CompletionStage[ T ] = FutureConverters.toJava( future )
  }

  private implicit class ScalaCompatibility[ T ]( val future: CompletionStage[ T ] ) extends AnyVal {
    @inline def toScala: Future[ T ] = FutureConverters.toScala( future )
  }
}
