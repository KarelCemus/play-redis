package play.api.cache.redis.connector

import javax.inject._

import akka.actor.ActorSystem
import akka.util.Timeout
import play.api.Logger
import play.api.cache.redis.Configuration
import play.api.inject.ApplicationLifecycle
import scredis.Client

import scala.concurrent.{ExecutionContext, Future}

/**
  * Constructs an actor directly communicating with the redis server. Internally, it uses
  * connection settings and Brando connector but encapsulates it with additional logic.
  *
  * @author Karel Cemus
  */
@Singleton
private[ connector ] class RedisActorProvider @Inject( )( configuration: Configuration, system: ActorSystem, lifecycle: ApplicationLifecycle ) extends Provider[ RedisActor ] {

  override def get( ): RedisActor = {
    import configuration._
    // actor wrapper with rich API
    val actor = new RedisActor( host, port, database, password )( system )
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
private[ connector ] class RedisActor( host: String, port: Int, database: Int, auth: Option[ String ] )( implicit system: ActorSystem ) {

  protected def log = Logger( "play.api.cache.redis" )

  private val redis = Client(
    host = host,
    port = port,
    database = database,
    passwordOpt = auth   ,
    system
  )

  /** execute the given request, we expect some data in return */
  def ?[ T ]( key: String, f: Client => Future[ T ], command: => String )( implicit timeout: Timeout, context: ExecutionContext ): ExpectedFuture[ T ] =
    new ExpectedFuture[ T ]( f( redis ), Some( key ), command )

  /** executes the request but does NOT expect data in return */
  def !( f: Client => Future[ Unit ], command: => String )( implicit timeout: Timeout, context: ExecutionContext ): ExpectedFuture[ Unit ] =
    new ExpectedFuture[ Unit ]( f( redis ), None, command )

  /** starts the actor */
  def start( ) = {
    log.info( s"Redis cache actor started. It is connected to $host:$port?database=$database" )
  }

  /** stops the actor */
  def stop( ) = {
    log.info( "Stopping the redis cache actor ..." )
    redis.quit( ).map { result =>
      log.info( "Redis cache stopped." )
      result
    }
  }
}
