package play.api.cache.redis.connector

import scala.concurrent.{ExecutionContext, Future}

import play.api.cache.redis.exception._

/**
  * The extended future implements advanced response handling.
  * It unifies maintenance of unexpected responses
  *
  * @author Karel Cemus
  */
private[ connector ] class ExpectedFuture[ T ]( future: Future[ T ], key: String, cmd: => String ) {

  /** received an unexpected response */
  private def onUnexpected( key: Option[ String ], cmd: => String ): PartialFunction[ Any, Nothing ] = {
    case _ => unexpected( key, cmd )
  }

  /** execution failed with an exception */
  private def onException( key: Option[ String ], cmd: => String ): PartialFunction[ Throwable, Nothing ] = {
    case ex => failed( key, cmd, ex )
  }

  /** handles both expected and unexpected responses and failure recovery */
  def expects[ U ]( expected: PartialFunction[ T, U ] )( implicit context: ExecutionContext ): Future[ U ] = {
    future map ( expected orElse onUnexpected( Some( key ), cmd ) ) recover onException( Some( key ), cmd )
  }

  def withParameter( param: String ) = withParameters( param )

  def withParameters( params: String ) = new ExpectedFuture( future, key, s"$key $params" )
}

/**
  * Constructs expected future from provided parameters, this serves as syntax sugar
  */
private[ connector ] class ExpectedFutureBuilder[ T ]( future: Future[ T ] ) {

  def executing( key: String, cmd: => String ): ExpectedFuture[ T ] = new ExpectedFuture[ T ]( future, key, cmd )

  def executing( key: String ): ExpectedFuture[ T ] = new ExpectedFuture[ T ]( future, key, key )
}
