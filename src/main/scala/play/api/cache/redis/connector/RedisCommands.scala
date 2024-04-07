package play.api.cache.redis.connector

import akka.actor.ActorSystem
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger
import io.lettuce.core.cluster.{ClusterClientOptions, ClusterTopologyRefreshOptions, RedisClusterClient}
import io.lettuce.core.{RedisClient, RedisURI}
import play.api.Logger
import play.api.cache.redis.configuration._
import play.api.inject.ApplicationLifecycle

import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject._
import scala.concurrent.Future
import scala.jdk.CollectionConverters.SeqHasAsJava

/**
  * Dispatches a provider of the redis commands implementation. Use with Guice
  * or some other DI container.
  */
private[connector] class RedisCommandsProvider(instance: RedisInstance)(implicit
  system: ActorSystem,
  lifecycle: ApplicationLifecycle
) extends Provider[RedisAsyncCommands[String, String]] {

  lazy val get = instance match {
    case cluster: RedisCluster       => new RedisCommandsCluster(cluster).get
    case standalone: RedisStandalone => new RedisCommandsStandalone(standalone).get
    case sentinel: RedisSentinel     => new RedisCommandsSentinel(sentinel).get
  }
}

private[connector] trait AbstractRedisCommands {

  /** logger instance */
  protected def log = Logger("play.api.cache.redis")

  def lifecycle: ApplicationLifecycle

  /** an implementation of the redis commands */
  def client: RedisAsyncCommands[String, String]

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
private[connector] class RedisCommandsStandalone(configuration: RedisStandalone)(implicit
  system: ActorSystem,
  val lifecycle: ApplicationLifecycle
) extends Provider[RedisAsyncCommands[String, String]] with AbstractRedisCommands {

  import configuration._

  private val redisUri = RedisURI.Builder.redis(host).withPort(port)
  redisUri.withDatabase(database.getOrElse(0))
  if (username.nonEmpty && password.nonEmpty) {
    redisUri.withAuthentication(username.get, password.get.toCharArray)
  } else if (password.nonEmpty) {
    redisUri.withPassword(password.get.toCharArray)
  }
  private val redisClient: RedisClient = RedisClient.create(redisUri.build())
  val client: RedisAsyncCommands[String, String] = redisClient.connect().async()

  // $COVERAGE-OFF$
  def start(): Unit = database.fold {
    log.info(s"Redis cache actor started. It is connected to $host:$port")
  } {
    database => log.info(s"Redis cache actor started. It is connected to $host:$port?database=$database")
  }

  def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cache actor ...")
    redisClient.shutdown()
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
private[connector] class RedisCommandsCluster(configuration: RedisCluster)(implicit
  system: ActorSystem,
  val lifecycle: ApplicationLifecycle
) extends Provider[RedisAsyncCommands[String, String]] with AbstractRedisCommands {

  import configuration._

  private val redisClient: RedisClusterClient = RedisClusterClient.create(nodes.map {
    case RedisHost(host, port, database, password, username) =>
      val redisUri = RedisURI.Builder.redis(host).withPort(port)
      redisUri.withDatabase(database.getOrElse(0))
      if (username.nonEmpty && password.nonEmpty) {
        redisUri.withAuthentication(username.get, password.get.toCharArray)
      } else if (password.nonEmpty) {
        redisUri.withPassword(password.get.toCharArray)
      }
      redisUri.build()
  }.asJava)
  private val topologyRefreshOptions: ClusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder.enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT, RefreshTrigger.PERSISTENT_RECONNECTS).adaptiveRefreshTriggersTimeout(
    Duration.ofNanos(TimeUnit.SECONDS.toNanos(30))
  ).build

  redisClient.setOptions(ClusterClientOptions.builder.topologyRefreshOptions(topologyRefreshOptions).build)

  val client: RedisAsyncCommands[String, String] = redisClient.connect().async().asInstanceOf[RedisAsyncCommands[String, String]]

  // $COVERAGE-OFF$
  def start(): Unit = {
    def servers: List[String] = nodes.map {
      case RedisHost(host, port, Some(database), _, _) => s" $host:$port?database=$database"
      case RedisHost(host, port, None, _, _)           => s" $host:$port"
    }

    log.info(s"Redis cluster cache actor started. It is connected to ${servers mkString ", "}")
  }

  def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cluster cache actor ...")
    redisClient.shutdown()
    log.info("Redis cluster cache stopped.")
  }
  // $COVERAGE-ON$
}

/**
  * Creates a connection to multiple redis sentinels.
  *
  * @param lifecycle     application lifecycle to trigger on stop hook
  * @param configuration configures sentinels
  * @param system        actor system
  */
private[connector] class RedisCommandsSentinel(configuration: RedisSentinel)(implicit system: ActorSystem, val lifecycle: ApplicationLifecycle) extends Provider[RedisAsyncCommands[String, String]] with AbstractRedisCommands {

  val sentinel: RedisHost = configuration.sentinels.head
  val redisUri: RedisURI.Builder = RedisURI.Builder.sentinel(sentinel.host, sentinel.port)
  if (configuration.database.nonEmpty) {
    redisUri.withDatabase(configuration.database.get)
  }
  if (configuration.username.nonEmpty && configuration.password.nonEmpty) {
    redisUri.withAuthentication(configuration.username.get, configuration.password.get.toCharArray)
  } else if (configuration.password.nonEmpty) {
    redisUri.withPassword(configuration.password.get.toCharArray)
  }
  configuration.sentinels.map {
    case RedisHost(host, port, database, password, username) =>
      redisUri.withSentinel(host, port)
  }
  private val redisClient: RedisClient = RedisClient.create(redisUri.build())
  val client: RedisAsyncCommands[String, String] = redisClient.connect().async()

  // $COVERAGE-OFF$
  def start(): Unit = {
    log.info(s"Redis sentinel cache actor started. It is connected to ${configuration.toString}")
  }

  def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis sentinel cache actor ...")
    redisClient.shutdown()
    log.info("Redis sentinel cache stopped.")
  }
  // $COVERAGE-ON$
}
