package play.api.cache.redis.impl

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of Sorted Set API using redis-server cache implementation.</p> */
private[ impl ] class RedisSetImpl[ Elem: ClassTag, Result[ _ ] ]( key: String, redis: RedisConnector )( implicit builder: Builders.ResultBuilder[ Result ], policy: RecoveryPolicy ) extends RedisSet[ Elem, Result ] with Implicits {

  // implicit ask timeout and execution context
  import redis.{context, timeout}

  @inline
  private def This: This = this

  def add( element: Elem* ) = redis.setAdd( key, element.map( _ -> 0D ): _* ).map( _ => This ).recoverWithDefault( This )

  def contains( element: Elem ) = redis.setRank( key, element ).map( _.nonEmpty ).recoverWithDefault( false )

  def remove( element: Elem* ) = redis.setRemove( key, element: _* ).map( _ => This ).recoverWithDefault( This )

  def toSet = redis.setSlice[ Elem ]( key, 0, -1 ).recoverWithDefault( Set.empty )

  def size = redis.setSize( key ).recoverWithDefault( 0 )

  def isEmpty = redis.setSize( key ).map( _ == 0 ).recoverWithDefault( true )

  def nonEmpty = redis.setSize( key ).map( _ > 0 ).recoverWithDefault( false )
}
