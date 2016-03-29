package play.api.cache.redis

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
class RedisActorProvider @Inject( )( settings: ConnectionSettings, system: ActorSystem ) extends Provider[ RedisActor ] {
  override def get( ): RedisActor = {
    import settings._
    // internal brando connector
    val internal = system actorOf StashingRedis {
      system actorOf Redis( host, port, database = database, auth = password )
    }
    // rich connector modifying API
    new RedisActor( internal )( system )
  }
}

/**
  * Rich akka actor providing additional functionality and syntax sugar. This proxy restricts API.
  *
  * @author Karel Cemus
  */
private[ redis ] class RedisActor( brando: ActorRef )( implicit system: ActorSystem ) {

  /** actor handler */
  private val actor = new AskableActorRef( brando )

  /** syntax sugar for querying the storage */
  def ?( request: Request )( implicit timeout: Timeout, context: ExecutionContext ): Future[ Any ] = actor ask request map Success.apply recover {
    case ex => Failure( ex ) // execution failed, recover
  }

  /** stops the actor */
  def stop( ) = {
    system.stop( actor.actorRef )
  }
}
