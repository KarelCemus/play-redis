package play.api.cache.redis.connector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import akka.actor.Scheduler
import akka.pattern.after
import redis._

/**
  *
  * Helper for manipulation with the request to the redis.
  * It defines the common variables and methods to avoid
  * code duplication
  *
  * @author Karel Cemus
  */
trait RequestTimeout extends Request {

  protected implicit val scheduler: Scheduler
}

object RequestTimeout {

  // fails
  @inline
  def fail( failAfter: FiniteDuration )( implicit scheduler: Scheduler, context: ExecutionContext ) = {
    after( failAfter, scheduler )( Future.failed( redis.actors.NoConnectionException ) )
  }

  // first completed
  @inline
  def invokeOrFail[ T ]( continue: => Future[ T ], failAfter: FiniteDuration )( implicit scheduler: Scheduler, context: ExecutionContext ): Future[ T ] = {
    Future.firstCompletedOf( Seq( continue, fail( failAfter ) ) )
  }
}


/**
  * Actor extension maintaining current connected status.
  * The operations are not invoked when the connection
  * is not established, the failed future is returned
  * instead.
  *
  * @author Karel Cemus
  */
trait FailEagerly extends RequestTimeout {
  import RequestTimeout._

  protected var connected = false

  /** max timeout of the future when the redis is disconnected */
  @inline
  private val failAfter = 300.millis

  abstract override def send[ T ]( redisCommand: RedisCommand[ _ <: protocol.RedisReply, T ] ) = {
    // proceed with the command
    @inline def continue = super.send( redisCommand )
    // based on connection status
    if ( connected ) continue else invokeOrFail( continue, failAfter )
  }
}


/**
  * Actor extension implementing a request timeout, if enabled.
  * This is due to no internal timeout provided by
  * the redis-scala to avoid never-completed futures.
  *
  * @author Karel Cemus
  */
trait RedisRequestTimeout extends RequestTimeout {
  import RequestTimeout._

  /** indicates the timeout on the redis request */
  protected def timeout: Option[ FiniteDuration ]

  abstract override def send[ T ]( redisCommand: RedisCommand[ _ <: protocol.RedisReply, T ] ) = {
    // proceed with the command
    @inline def continue = super.send( redisCommand )
    // based on connection status
    timeout.fold( continue )( invokeOrFail( continue, _ ) )
  }
}
