package play.api.cache.redis.connector

import scala.concurrent.{ExecutionContext, Future}

import play.api.cache.redis._

/**
  * The extended future implements advanced response handling.
  * It unifies maintenance of unexpected responses
  *
  * @author Karel Cemus
  */
private[ connector ] trait ExpectedFuture[ T ] {

  protected def future: Future[ T ]

  protected def cmd: String

  /** received an unexpected response */
  protected def onUnexpected: PartialFunction[ Any, Nothing ]

  protected def onFailed( ex: Throwable ): Nothing

  /** execution failed with an exception */
  private def onException: PartialFunction[ Throwable, Nothing ] = {
    case ex: RedisException => throw ex
    case ex => onFailed( ex )
  }

  /** handles both expected and unexpected responses and failure recovery */
  def expects[ U ]( expected: PartialFunction[ T, U ] )( implicit context: ExecutionContext ): Future[ U ] = {
    future map ( expected orElse onUnexpected ) recover onException
  }
}

private[ connector ] class ExpectedFutureWithoutKey[ T ]( protected val future: Future[ T ], protected val cmd: String ) extends ExpectedFuture[ T ] {

  protected def onUnexpected: PartialFunction[ Any, Nothing ] = {
    case _ => unexpected( None, cmd )
  }

  protected def onFailed( ex: Throwable ): Nothing =
    failed( None, cmd, ex )

  def withKey( key: String ): ExpectedFutureWithKey[ T ] = new ExpectedFutureWithKey[ T ]( future, cmd, key, s"$cmd $key" )

  def withKeys( keys: Traversable[ String ] ): ExpectedFutureWithKey[ T ] = withKey( keys mkString " " )
}

private[ connector ] class ExpectedFutureWithKey[ T ]( protected val future: Future[ T ], protected val cmd: String, key: String, fullCommand: => String ) extends ExpectedFuture[ T ] {

  protected def onUnexpected: PartialFunction[ Any, Nothing ] = {
    case _ => unexpected( Some( key ), cmd )
  }

  protected def onFailed( ex: Throwable ): Nothing =
    failed( Some( key ), cmd, ex )

  def andParameter( param: => Any ): ExpectedFutureWithKey[ T ] = andParameters( param.toString )

  def andParameters( params: Traversable[ Any ] ): ExpectedFutureWithKey[ T ] = andParameters( params.mkString(" ") )

  def andParameters( params: => String ): ExpectedFutureWithKey[ T ] = new ExpectedFutureWithKey( future, cmd, key, s"$fullCommand $params" )

  def asCommand( commandOverride: => String ) = new ExpectedFutureWithKey( future, cmd, key, s"$cmd $commandOverride" )
}

/**
  * Constructs expected future from provided parameters, this serves as syntax sugar
  */
private[ connector ] class ExpectedFutureBuilder[ T ]( val future: Future[ T ] ) extends AnyVal {

  def executing( cmd: String ): ExpectedFutureWithoutKey[ T ] = new ExpectedFutureWithoutKey[ T ]( future, cmd )
}
