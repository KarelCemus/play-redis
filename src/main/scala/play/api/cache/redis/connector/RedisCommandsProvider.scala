package play.api.cache.redis.connector

import javax.inject._

import scala.concurrent.Future

import play.api.Logger
import play.api.cache.redis.RedisConfiguration
import play.api.cache.redis.configuration.ClusterHost
import play.api.inject.ApplicationLifecycle

import akka.actor.ActorSystem
import redis._

/**
  * Dispatches a provider of the redis commands implementation.
  *
  * @author Karel Cemus
  */
@Singleton
class RedisCommandsProvider @Inject()( lifecycle: ApplicationLifecycle, configuration: RedisConfiguration )( implicit system: ActorSystem ) extends Provider[ RedisCommands ] {

  val provider = if ( configuration.cluster.isEmpty ) instance else cluster

  private def instance = new RedisCommandsInstance( lifecycle, configuration )

  private def cluster = new RedisCommandsCluster( lifecycle, configuration )

  def get( ) = provider.get()
}

private[ connector ] trait AbstractRedisCommands {

  /** logger instance */
  protected def log = Logger( "play.api.cache.redis" )

  def lifecycle: ApplicationLifecycle

  /** an implementation of the redis commands */
  def client: RedisCommands

  def get( ) = client

  /** action invoked on the start of the actor */
  def start( ): Unit

  /** stops the actor */
  def stop( ): Future[ Unit ]

  // start the connector
  start()
  // listen on system stop
  lifecycle.addStopHook( stop _ )
}


/**
  * Creates a connection to the single instance of redis
  *
  * @param lifecycle     application lifecycle to trigger on stop hook
  * @param configuration configures clusters
  * @param system        actor system
  */
private[ connector ] class RedisCommandsInstance( val lifecycle: ApplicationLifecycle, configuration: RedisConfiguration )( implicit system: ActorSystem ) extends Provider[ RedisCommands ] with AbstractRedisCommands {

  val client = RedisClient(
    host = configuration.host,
    port = configuration.port,
    db = Some( configuration.database ),
    password = configuration.password
  )

  def start( ) = {
    import configuration.{database, host, port}
    log.info( s"Redis cache actor started. It is connected to $host:$port?database=$database" )
  }

  def stop( ): Future[ Unit ] = Future successful {
    log.info( "Stopping the redis cache actor ..." )
    client.stop()
    log.info( "Redis cache stopped." )
  }
}


/**
  * Creates a connection to the redis cluster.
  *
  * @param lifecycle     application lifecycle to trigger on stop hook
  * @param configuration configures clusters
  * @param system        actor system
  */
private[ connector ] class RedisCommandsCluster( val lifecycle: ApplicationLifecycle, configuration: RedisConfiguration )( implicit system: ActorSystem ) extends Provider[ RedisCommands ] with AbstractRedisCommands {

  val client = RedisCluster(
    configuration.cluster.map {
      case ClusterHost( host, port, password, database ) => RedisServer( host, port, password, database )
    }
  )

  def start( ) = {
    def servers = configuration.cluster.map {
      case ClusterHost( host, port, _, Some( database ) ) => s" $host:$port?database=$database"
      case ClusterHost( host, port, _, None ) => s" $host:$port"
    }

    log.info( s"Redis cluster cache actor started. It is connected to ${ servers mkString ", " }" )
  }

  def stop( ): Future[ Unit ] = Future successful {
    log.info( "Stopping the redis cluster cache actor ..." )
    client.stop()
    log.info( "Redis cluster cache stopped." )
  }
}
