package play.api.cache.redis.connector

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.pattern._
import redis._

/**
  * Actor extension maintaining current connected status.
  * The operations are not invoked when the connection
  * is not established, the failed future is returned
  * instead.
  *
  */
trait FailEagerly extends Request {

  protected var connected = false

  protected val scheduler: Scheduler

  /** max timeout of the future when the redis is disconnected */
  @inline
  private val failAfter = 300.millis

  abstract override def send[ T ]( redisCommand: RedisCommand[ _ <: protocol.RedisReply, T ] ) = {
    // proceed with the command
    @inline def continue = super.send( redisCommand )
    // fails eagerly when the connection is not established
    @inline def fail = after( failAfter, scheduler )( Future.failed( redis.actors.NoConnectionException ) )
    // first completed
    @inline def orFail = Future.firstCompletedOf( Seq( continue, fail ) )
    // based on connection status
    if ( connected ) continue else orFail
  }
}
