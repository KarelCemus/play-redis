package play.api.cache.redis.connector

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import play.api.cache.redis.ConnectionSettings
import play.api.inject.ApplicationLifecycle

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
private[ connector ] class RedisActorProvider @Inject( )( settings: ConnectionSettings, system: ActorSystem, lifecycle: ApplicationLifecycle ) extends Provider[ RedisActor ] {
  override def get( ): RedisActor = {
    import settings._
    // internal brando connector
    val internal = system actorOf Redis( host, port, database = database, auth = password )
    // stashing brando connector, queuing messages when disconnected
    val stashing = system actorOf StashingRedis( internal )
    // actor wrapper with rich API
    val actor = new RedisActor( stashing, host, port, database )( system )
    // start the actor
    actor.start( )
    // listen on system stop
    lifecycle.addStopHook( actor.stop _ )
    // return created instance
    actor
  }
}

/**
  * Rich akka actor providing additional functionality and syntax sugar. This proxy restricts API.
  *
  * @author Karel Cemus
  */
private[ connector ] class RedisActor( brando: ActorRef, host: String, port: Int, database: Int )( implicit system: ActorSystem ) {

  /** actor handler */
  private val actor = new AskableActorRef( brando )

  protected def log = Logger( "play.api.cache.redis" )

  /** execute the given request, we expect some data in return */
  def ?[ T ]( command: String, key: String, params: String* )( implicit timeout: Timeout, context: ExecutionContext ): ExpectedFuture[ T ] =
    new ExpectedFuture[ T ]( actor ask Request( command, key +: params: _* ), Some( key ), s"$command ${ key +: params.headOption.toList mkString " " }" )

  /** executes the request but does NOT expect data in return */
  def !( command: String, params: String* )( implicit timeout: Timeout, context: ExecutionContext ): ExpectedFuture[ Unit ] =
    new ExpectedFuture[ Unit ]( actor ask Request( command, params: _* ), None, s"${ command +: params.headOption.toList mkString " " }" )

  /** starts the actor */
  def start( ) = {
    log.info( s"Redis cache actor started. It is connected to $host:$port?database=$database" )
  }

  /** stops the actor */
  def stop( ) = Future.successful {
    log.info( "Stopping the redis cache actor ..." )
    system.stop( actor.actorRef )
    log.info( "Redis cache stopped." )
  }
}
