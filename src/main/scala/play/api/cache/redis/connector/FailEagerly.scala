package play.api.cache.redis.connector

import scala.concurrent.Future

import redis._


/**
  * Actor extension maintaining current connected status.
  * The operations are not invoked when the connection
  * is not established, the failed future is returned
  * instead.
  *
  */
trait FailEagerly extends redis.Request {

  protected var connected = false

  abstract override def send[ T ]( redisCommand: RedisCommand[ _ <: protocol.RedisReply, T ] ) = {
    // fails eagerly when the connection is not established
    if ( connected ) super.send( redisCommand )
    else Future.failed( redis.actors.NoConnectionException )
  }
}
