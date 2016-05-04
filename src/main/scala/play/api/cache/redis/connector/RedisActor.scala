package play.api.cache.redis.connector

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import play.api.cache.redis.ConnectionSettings

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import brando.{Redis, Request, StashingRedis}


/**
  * Constructs an actor directly communicating with the redis server. Internally, it uses
  * connection settings and Brando connector but encapsulates it with additional logic.
  *
  * @author Karel Cemus
  */
@Singleton
private[ connector ] class RedisActorProvider @Inject( )( settings: ConnectionSettings, system: ActorSystem ) extends Provider[ RedisActor ] {
  override def get( ): RedisActor = {
    import settings._
    // internal brando connector
    val internal = system actorOf Redis( host, port, database = database, auth = password )
    // stashing brando connector, queuing messages when disconnected
    val stashing = system actorOf StashingRedis( internal )
    // actor wrapper with rich API
    new RedisActor( stashing )( system )
  }
}

/**
  * Rich akka actor providing additional functionality and syntax sugar. This proxy restricts API.
  *
  * @author Karel Cemus
  */
private[ connector ] class RedisActor( brando: ActorRef )( implicit system: ActorSystem ) {

  /** actor handler */
  private val actor = new AskableActorRef( brando )

  /** syntax sugar for querying the storage */
  private def execute( request: Request )( implicit timeout: Timeout, context: ExecutionContext ): Future[ Any ] = actor ask request map Success.apply recover {
    case ex => Failure( ex ) // execution failed, recover
  }

  /** execute the given request, we expect some data in return */
  def ?[ T ]( command: String, key: String, params: String* )( implicit timeout: Timeout, context: ExecutionContext ): ExpectedFuture[ T ] =
    new ExpectedFuture[ T ]( this execute Request( command, key +: params: _* ), s"$command ${ key +: params.headOption.toList mkString " " }" )

  /** executes the request but does NOT expect data in return */
  def !( command: String, params: String* )( implicit timeout: Timeout, context: ExecutionContext ): ExpectedFuture[ Unit ] =
    new ExpectedFuture[ Unit ]( this execute Request( command, params: _* ), s"${ command +: params.headOption.toList mkString " " }" )

  /** stops the actor */
  def stop( ) = {
    system.stop( actor.actorRef )
  }
}
