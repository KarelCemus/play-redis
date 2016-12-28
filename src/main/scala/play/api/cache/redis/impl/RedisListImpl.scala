package play.api.cache.redis.impl

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag

import play.api.cache.redis._

/** <p>Implementation of List API using redis-server cache implementation.</p> */
private[ impl ] class RedisListImpl[ Elem: ClassTag, Result[ _ ] ]( key: String, redis: RedisConnector )( implicit builder: Builders.ResultBuilder[ Result ], policy: RecoveryPolicy ) extends RedisList[ Elem, Result ] with Implicits {

  // implicit ask timeout and execution context
  // import redis.{context, timeout}
  def prepend( element: Elem ) = ???

  def append( element: Elem ) = ???

  def +:( element: Elem ) = ???

  def :+( element: Elem ) = ???

  def ++:( element: Traversable[ Elem ] ) = ???

  def :++( element: Traversable[ Elem ] ) = ???

  def apply( index: Int ) = ???

  def get( index: Int ) = ???

  def headPop = ???

  def size = ???

  def insert( position: Int, element: Elem* ) = ???

  def set( position: Int, element: Elem ) = ???

  def view = ???

  def modify = ???
}
