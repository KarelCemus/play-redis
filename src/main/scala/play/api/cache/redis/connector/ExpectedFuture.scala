package play.api.cache.redis.connector

import scala.concurrent.{ExecutionContext, Future}

import play.api.cache.redis.exception._

/**
  * The extended future implements advanced response handling.
  * It unifies maintenance of unexpected responses
  *
  * @author Karel Cemus
  */
private[ connector ] class ExpectedFuture[ T ]( future: Future[ Any ], key: Option[ String ], cmd: => String ) {

  /** received an unexpected response */
  private def onUnexpected: PartialFunction[ Any, T ] = {
    case _ => unexpected( key, cmd )
  }

  /** execution failed with an exception */
  private def onException: PartialFunction[ Throwable, T ] = {
    case ex => failed( key, cmd, ex )
  }

  /** handles both expected and unexpected responses and failure recovery */
  def expects( expected: PartialFunction[ Any, T ] )( implicit context: ExecutionContext ): Future[ T ] =
    future map ( expected orElse onUnexpected ) recover onException
}
