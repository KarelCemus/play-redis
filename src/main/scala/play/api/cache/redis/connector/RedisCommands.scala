package play.api.cache.redis.connector

import akka.actor.{ActorSystem, Scheduler}
import play.api.Logger
import play.api.cache.redis.configuration._
import play.api.inject.ApplicationLifecycle
import redis.{RedisClient => RedisStandaloneClient, RedisCluster => RedisClusterClient, _}

import javax.inject._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Dispatches a provider of the redis commands implementation. Use with Guice
  * or some other DI container.
  */
private[connector] class RedisCommandsProvider(instance: RedisInstance)(implicit system: ActorSystem, lifecycle: ApplicationLifecycle) extends Provider[RedisCommands] {

  lazy val get: RedisCommands = instance match {
    case cluster: RedisCluster       => new RedisCommandsCluster(cluster).get
    case standalone: RedisStandalone => new RedisCommandsStandalone(standalone).get
    case sentinel: RedisSentinel     => new RedisCommandsSentinel(sentinel).get
  }

}

private[connector] trait AbstractRedisCommands {

  /** logger instance */
  protected def log: Logger = Logger("play.api.cache.redis")

  def lifecycle: ApplicationLifecycle

  /** an implementation of the redis commands */
  def client: RedisCommands

  lazy val get: RedisCommands = client

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
  * @param lifecycle
  *   application lifecycle to trigger on stop hook
  * @param configuration
  *   configures clusters
  * @param system
  *   actor system
  */
private[connector] class RedisCommandsStandalone(configuration: RedisStandalone)(implicit system: ActorSystem, val lifecycle: ApplicationLifecycle) extends Provider[RedisCommands] with AbstractRedisCommands {
  import configuration._

  val client: RedisStandaloneClient = new RedisStandaloneClient(
    host = host,
    port = port,
    db = database,
    username = username,
    password = password,
  ) with FailEagerly with RedisRequestTimeout {

    protected val connectionTimeout: Option[FiniteDuration] = configuration.timeout.connection

    protected val timeout: Option[FiniteDuration] = configuration.timeout.redis

    implicit protected val scheduler: Scheduler = system.scheduler

    override def send[T](redisCommand: RedisCommand[? <: protocol.RedisReply, T]): Future[T] = super.send(redisCommand)

    override def onConnectStatus: Boolean => Unit = (status: Boolean) => connected = status
  }

  // $COVERAGE-OFF$
  override def start(): Unit = database.fold {
    log.info(s"Redis cache actor started. It is connected to $host:$port")
  } { database =>
    log.info(s"Redis cache actor started. It is connected to $host:$port?database=$database")
  }

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cache actor ...")
    client.stop()
    log.info("Redis cache stopped.")
  }
  // $COVERAGE-ON$

}

/**
  * Creates a connection to the redis cluster.
  *
  * @param lifecycle
  *   application lifecycle to trigger on stop hook
  * @param configuration
  *   configures clusters
  * @param system
  *   actor system
  */
private[connector] class RedisCommandsCluster(configuration: RedisCluster)(implicit system: ActorSystem, val lifecycle: ApplicationLifecycle) extends Provider[RedisCommands] with AbstractRedisCommands {

  import HostnameResolver._

  import configuration._

  val client: RedisClusterClient = new RedisClusterClient(
    nodes.map { case RedisHost(host, port, database, username, password) =>
      RedisServer(host.resolvedIpAddress, port, username, password, database)
    },
  ) with RedisRequestTimeout {

    protected val timeout: Option[FiniteDuration] = configuration.timeout.redis

    implicit protected val scheduler: Scheduler = system.scheduler
  }

  // $COVERAGE-OFF$
  override def start(): Unit = {
    def servers: Seq[String] = nodes.map {
      case RedisHost(host, port, Some(database), _, _) => s" $host:$port?database=$database"
      case RedisHost(host, port, None, _, _)           => s" $host:$port"
    }

    log.info(s"Redis cluster cache actor started. It is connected to ${servers mkString ", "}")
  }

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cluster cache actor ...")
    Option(client).foreach(_.stop())
    log.info("Redis cluster cache stopped.")
  }
  // $COVERAGE-ON$

}

/**
  * Creates a connection to multiple redis sentinels.
  *
  * @param lifecycle
  *   application lifecycle to trigger on stop hook
  * @param configuration
  *   configures sentinels
  * @param system
  *   actor system
  */
private[connector] class RedisCommandsSentinel(configuration: RedisSentinel)(implicit system: ActorSystem, val lifecycle: ApplicationLifecycle) extends Provider[RedisCommands] with AbstractRedisCommands {
  import HostnameResolver._

  val client: SentinelMonitoredRedisClient with RedisRequestTimeout = new SentinelMonitoredRedisClient(
    configuration.sentinels.map { case RedisHost(host, port, _, _, _) =>
      (host.resolvedIpAddress, port)
    },
    master = configuration.masterGroup,
    username = configuration.username,
    password = configuration.password,
    db = configuration.database,
  ) with RedisRequestTimeout {

    protected val timeout: Option[FiniteDuration] = configuration.timeout.redis

    implicit protected val scheduler: Scheduler = system.scheduler

    override def send[T](redisCommand: RedisCommand[? <: protocol.RedisReply, T]): Future[T] = super.send(redisCommand)
  }

  // $COVERAGE-OFF$
  override def start(): Unit =
    log.info(s"Redis sentinel cache actor started. It is connected to ${configuration.toString}")

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis sentinel cache actor ...")
    client.stop()
    log.info("Redis sentinel cache stopped.")
  }
  // $COVERAGE-ON$

}
