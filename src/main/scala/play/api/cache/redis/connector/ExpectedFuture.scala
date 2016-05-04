package play.api.cache.redis.connector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import play.api.cache.redis.exception._

/**
  * The extended future implements advanced response handling.
  * It unifies maintenance of unexpected responses
  *
  * @author Karel Cemus
  */
private[ connector ] class ExpectedFuture[ T ]( future: Future[ Any ], cmd: => String ) {

  /** received an unexpected response */
  private def onUnexpected: PartialFunction[ Any, T ] = {
    case Success( _ ) => unexpected( "???", cmd ) // TODO provide the key
    case Failure( ex ) => failed( "???", cmd, ex ) // TODO provide the key
    case _ => unexpected( "???", cmd ) // TODO provide the key
  }

  /** execution failed with an exception */
  private def onException: PartialFunction[ Throwable, T ] = {
    case ex => failed( "???", cmd, ex ) // TODO provide the key
  }

  /** handles both expected and unexpected responses and failure recovery */
  def expects( expected: PartialFunction[ Any, T ] )( implicit context: ExecutionContext ): Future[ T ] =
    future map ( expected orElse onUnexpected ) recover onException
}
