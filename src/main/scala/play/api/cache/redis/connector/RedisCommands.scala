package play.api.cache.redis.connector

import akka.actor.{ActorSystem, Scheduler}
import javax.inject._
import play.api.Logger
import play.api.cache.redis.configuration._
import play.api.inject.ApplicationLifecycle
import redis.{RedisClient => RedisStandaloneClient, RedisCluster => RedisClusterClient, _}

import scala.concurrent.Future

/**
  * Dispatches a provider of the redis commands implementation. Use with Guice
  * or some other DI container.
  */
private[connector] class RedisCommandsProvider(instance: RedisInstance)(implicit system: ActorSystem, lifecycle: ApplicationLifecycle) extends Provider[RedisCommands] {

  lazy val get = instance match {
    case cluster: RedisCluster => new RedisCommandsCluster( cluster ).get
    case standalone: RedisStandalone => new RedisCommandsStandalone( standalone ).get
    case sentinel: RedisSentinel => new RedisCommandsSentinel( sentinel ).get
  }
}

private[connector] trait AbstractRedisCommands {

  /** logger instance */
  protected def log = Logger("play.api.cache.redis")

  def lifecycle: ApplicationLifecycle

  /** an implementation of the redis commands */
  def client: RedisCommands

  lazy val get = client

  /** action invoked on the start of the actor */
  def start(): Unit

  /** stops the actor */
  def stop(): Future[Unit]

  // start the connector
  start()
  // listen on system stop
  lifecycle.addStopHook(() => stop())
}

/**
  * Creates a connection to the single instance of redis
  *
  * @param lifecycle     application lifecycle to trigger on stop hook
  * @param configuration configures clusters
  * @param system        actor system
  */
private[connector] class RedisCommandsStandalone(configuration: RedisStandalone)(implicit system: ActorSystem, val lifecycle: ApplicationLifecycle) extends Provider[RedisCommands] with AbstractRedisCommands {
  import configuration._

  val client: RedisStandaloneClient = new RedisStandaloneClient(
    host = host,
    port = port,
    db = database,
    password = password
  ) with FailEagerly with RedisRequestTimeout {

    protected val connectionTimeout = configuration.timeout.connection

    protected val timeout = configuration.timeout.redis

    protected implicit val scheduler = system.scheduler

    override def send[T](redisCommand: RedisCommand[_ <: protocol.RedisReply, T]) = super.send(redisCommand)

    override def onConnectStatus = (status: Boolean) => connected = status
  }

  // $COVERAGE-OFF$
  def start() = database.fold {
    log.info(s"Redis cache actor started. It is connected to $host:$port")
  } {
    database => log.info(s"Redis cache actor started. It is connected to $host:$port?database=$database")
  }

  def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cache actor ...")
    client.stop()
    log.info("Redis cache stopped.")
  }
  // $COVERAGE-ON$
}

/**
  * Creates a connection to the redis cluster.
  *
  * @param lifecycle     application lifecycle to trigger on stop hook
  * @param configuration configures clusters
  * @param system        actor system
  */
private[connector] class RedisCommandsCluster(configuration: RedisCluster)(implicit system: ActorSystem, val lifecycle: ApplicationLifecycle) extends Provider[RedisCommands] with AbstractRedisCommands {
  import HostnameResolver._
  import configuration._

  val client: RedisClusterClient = new RedisClusterClient(
    nodes.map {
      case RedisHost(host, port, database, password) => RedisServer(host.resolvedIpAddress, port, password, database)
    }
  ) with RedisRequestTimeout {
    protected val timeout = configuration.timeout.redis

    protected implicit val scheduler = system.scheduler
  }

  // $COVERAGE-OFF$
  def start() = {
    def servers = nodes.map {
      case RedisHost(host, port, Some(database), _) => s" $host:$port?database=$database"
      case RedisHost(host, port, None, _)           => s" $host:$port"
    }

    log.info(s"Redis cluster cache actor started. It is connected to ${servers mkString ", "}")
  }

  def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cluster cache actor ...")
    client.stop()
    log.info("Redis cluster cache stopped.")
  }
  // $COVERAGE-ON$
}

/**
  * Creates a connection to multiple redis sentinels.
  *
  * @author Tomas Cerny (tmscer@gmail.com)
  *
  * @param lifecycle     application lifecycle to trigger on stop hook
  * @param configuration configures sentinels
  * @param system        actor system
  */
private[ connector ] class RedisCommandsSentinel( configuration: RedisSentinel )( implicit system: ActorSystem, val lifecycle: ApplicationLifecycle ) extends Provider[ RedisCommands ] with AbstractRedisCommands {
  import HostnameResolver._

  val client: SentinelMonitoredRedisClient with RedisRequestTimeout = new SentinelMonitoredRedisClient(
    configuration.sentinels.map {
      case RedisHost(host, port, _, _) => (host.resolvedIpAddress, port)
    },
    master = configuration.masterGroup,
    password = configuration.password,
    db = configuration.database
  ) with RedisRequestTimeout {
    protected val connectionTimeout = configuration.timeout.connection

    protected val timeout = configuration.timeout.redis

    protected implicit val scheduler = system.scheduler

    override def send[ T ]( redisCommand: RedisCommand[ _ <: protocol.RedisReply, T ] ) = super.send( redisCommand )
  }

  // $COVERAGE-OFF$
  def start( ): Unit = {
    log.info( s"Redis sentinel cache actor started. It is connected to ${ configuration.toString }" )
  }

  def stop( ): Future[ Unit ] = Future successful {
    log.info( "Stopping the redis sentinel cache actor ..." )
    client.stop()
    log.info( "Redis sentinel cache stopped." )
  }
  // $COVERAGE-ON$
}
