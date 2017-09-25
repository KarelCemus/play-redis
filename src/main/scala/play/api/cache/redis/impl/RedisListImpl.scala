package play.api.cache.redis.impl

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of List API using redis-server cache implementation.</p> */
private[ impl ] class RedisListImpl[ Elem: ClassTag, Result[ _ ] ]( key: String, redis: RedisConnector )( implicit builder: Builders.ResultBuilder[ Result ], runtime: RedisRuntime ) extends RedisList[ Elem, Result ] {

  // implicit ask timeout and execution context
  import dsl._

  @inline
  private def This: This = this

  def prepend( element: Elem ) = prependAll( element )

  def append( element: Elem ) = appendAll( element )

  def +:( element: Elem ) = prependAll( element )

  def :+( element: Elem ) = appendAll( element )

  def ++:( elements: Traversable[ Elem ] ) = prependAll( elements.toSeq: _* )

  def :++( elements: Traversable[ Elem ] ) = appendAll( elements.toSeq: _* )

  private def prependAll( elements: Elem* ) =
    redis.listPrepend( key, elements: _* ).map( _ => This ).recoverWithDefault( This )

  private def appendAll( elements: Elem* ) =
    redis.listAppend( key, elements: _* ).map( _ => This ).recoverWithDefault( This )

  def apply( index: Int ) = redis.listSlice[ Elem ]( key, index, index ).map {
    _.headOption getOrElse ( throw new NoSuchElementException( s"Element at index $index is missing." ) )
  }.recoverWithDefault {
    throw new NoSuchElementException( s"Element at index $index is missing." )
  }

  def get( index: Int ) =
    redis.listSlice[ Elem ]( key, index, index ).map( _.headOption ).recoverWithDefault( None )

  def headPop = redis.listHeadPop[ Elem ]( key ).recoverWithDefault( None )

  def size = redis.listSize( key ).recoverWithDefault( 0 )

  def insertBefore( pivot: Elem, element: Elem ) =
    redis.listInsert( key, pivot, element ).recoverWithDefault( None )

  def set( position: Int, element: Elem ) =
    redis.listSetAt( key, position, element ).map( _ => This ).recoverWithDefault( This )

  def isEmpty =
    redis.listSize( key ).map( _ == 0 ).recoverWithDefault( true )

  def nonEmpty =
    redis.listSize( key ).map( _ > 0 ).recoverWithDefault( false )

  def view = ListView

  object ListView extends RedisListView {
    def slice( start: Int, end: Int ) = redis.listSlice[ Elem ]( key, start, end ).recoverWithDefault( Seq.empty )
  }

  def modify = ListModifier

  object ListModifier extends RedisListModification {

    def collection = This

    def clear( ) =
      redis.remove( key ).map {
        _ => this: this.type
      }.recoverWithDefault( this )

    def slice( start: Int, end: Int ) =
      redis.listTrim( key, start, end ).map {
        _ => this: this.type
      }.recoverWithDefault( this )
  }

  def remove( element: Elem, count: Int ) =
    redis.listRemove( key, element, count ).map( _ => This ).recoverWithDefault( This )

  def removeAt( position: Int ) =
    redis.listSetAt( key, position, "play-redis:DELETED" ).flatMap {
      _ => redis.listRemove( key, "play-redis:DELETED", count = 0 )
    }.map( _ => This ).recoverWithDefault( This )
}
