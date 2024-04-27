package play.api.cache.redis.connector

import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.{ClusterClientOptions, ClusterTopologyRefreshOptions, RedisClusterClient}
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.masterreplica.{MasterReplica, StatefulRedisMasterReplicaConnection}
import io.lettuce.core.{ReadFrom, RedisClient, RedisURI}
import play.api.Logger
import play.api.cache.redis.configuration._
import play.api.inject.ApplicationLifecycle

import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.SeqHasAsJava
import java.time.{Duration => JavaDuration}

/**
 * Dispatches a provider of the redis commands implementation. Use with Guice
 * or some other DI container.
 */
private[connector] class RedisCommandsProvider(instance: RedisInstance)(implicit lifecycle: ApplicationLifecycle) extends Provider[RedisClusterAsyncCommands[String, String]] {

  lazy val get: RedisClusterAsyncCommands[String, String] = instance match {
    case cluster: RedisCluster => new RedisCommandsCluster(cluster).get
    case standalone: RedisStandalone => new RedisCommandsStandalone(standalone).get
    case sentinel: RedisSentinel => new RedisCommandsSentinel(sentinel).get
    case masterSlaves: RedisMasterSlaves => new RedisCommandsMasterSlaves(masterSlaves).get
  }

}

private[connector] trait AbstractRedisCommands {

  /** logger instance */
  protected def log: Logger = Logger("play.api.cache.redis")

  def lifecycle: ApplicationLifecycle

  /** an implementation of the redis commands */
  def client: RedisClusterAsyncCommands[String, String]

  lazy val get: RedisClusterAsyncCommands[String, String] = client

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
 * application lifecycle to trigger on stop hook
 * @param configuration
 * configures clusters
 */
private[connector] class RedisCommandsStandalone(configuration: RedisStandalone)(implicit val lifecycle: ApplicationLifecycle) extends Provider[RedisClusterAsyncCommands[String, String]] with AbstractRedisCommands {

  import configuration._

  private val redisUri = RedisURI.Builder.redis(host).withPort(port)
  redisUri.withDatabase(database.getOrElse(0))
  if (username.nonEmpty && password.nonEmpty) {
    redisUri.withAuthentication(username.get, password.get.toCharArray)
  } else if (password.nonEmpty) {
    redisUri.withPassword(password.get.toCharArray)
  }

  private val redisClient: RedisClient = RedisClient.create(
    ScalaRedisClientResources.clientResources(),
    redisUri.build())

  private val connectionTimeout: Option[FiniteDuration] = configuration.timeout.connection
  if (connectionTimeout.nonEmpty) {
    redisClient.connect().setTimeout(JavaDuration.ofNanos(connectionTimeout.get.toNanos))
  }

  redisClient.setOptions(ScalaRedisClientResources.clientOption(configuration.timeout.redis))

  val client: RedisAsyncCommands[String, String] = redisClient.connect().async()


  // $COVERAGE-OFF$
  override def start(): Unit = database.fold {
    log.info(s"Redis cache actor started. It is connected to $host:$port")
  } { database =>
    log.info(s"Redis cache actor started. It is connected to $host:$port?database=$database")
  }

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cache actor ...")
    redisClient.shutdown()
    log.info("Redis cache stopped.")
  }
  // $COVERAGE-ON$

}

/**
 * Creates a connection to the redis cluster.
 *
 * @param lifecycle
 * application lifecycle to trigger on stop hook
 * @param configuration
 * configures clusters
 */
private[connector] class RedisCommandsCluster(configuration: RedisCluster)(implicit val lifecycle: ApplicationLifecycle) extends Provider[RedisClusterAsyncCommands[String, String]] with AbstractRedisCommands {

  import configuration._

  private val redisClient: RedisClusterClient = RedisClusterClient.create(ScalaRedisClientResources.clientResources(),
    nodes.map {
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
  private val topologyRefreshOptions: ClusterTopologyRefreshOptions =
    ClusterTopologyRefreshOptions.builder.enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT,
      RefreshTrigger.PERSISTENT_RECONNECTS).adaptiveRefreshTriggersTimeout(
      Duration.ofNanos(TimeUnit.SECONDS.toNanos(30))
    ).build
  private val connectionTimeout: Option[FiniteDuration] = configuration.timeout.connection
  if (connectionTimeout.nonEmpty) {
    redisClient.connect().setTimeout(JavaDuration.ofNanos(connectionTimeout.get.toNanos))
  }
  redisClient.setOptions(ClusterClientOptions.builder().autoReconnect(true) //Auto-Reconnect
    .pingBeforeActivateConnection(true) //PING before activating connection
    .timeoutOptions(
      ScalaRedisClientResources.timeoutOptions(configuration.timeout.redis)
    ).topologyRefreshOptions(topologyRefreshOptions).build)

  val client: RedisClusterAsyncCommands[String, String] = redisClient.connect().async()


  // $COVERAGE-OFF$
  override def start(): Unit = {
    def servers: Seq[String] = nodes.map {
      case RedisHost(host, port, Some(database), _, _) => s" $host:$port?database=$database"
      case RedisHost(host, port, None, _, _) => s" $host:$port"
    }

    log.info(s"Redis cluster cache actor started. It is connected to ${servers mkString ", "}")
  }

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cluster cache actor ...")
    redisClient.shutdown()
    log.info("Redis cluster cache stopped.")
  }
  // $COVERAGE-ON$

}

/**
 * Creates a connection to multiple redis sentinels.
 *
 * @param lifecycle
 * application lifecycle to trigger on stop hook
 * @param configuration
 * configures sentinels
 */
private[connector] class RedisCommandsSentinel(configuration: RedisSentinel)(implicit val lifecycle: ApplicationLifecycle) extends Provider[RedisClusterAsyncCommands[String, String]] with AbstractRedisCommands {
  val sentinel: RedisHost = configuration.sentinels.head
  private val redisUri: RedisURI.Builder = RedisURI.Builder
    .sentinel(sentinel.host, sentinel.port)
  if (configuration.database.nonEmpty) {
    redisUri.withDatabase(configuration.database.get)
  }
  if (configuration.username.nonEmpty && configuration.password.nonEmpty) {
    redisUri.withAuthentication(configuration.username.get, configuration.password.get.toCharArray)
  } else if (configuration.password.nonEmpty) {
    redisUri.withPassword(configuration.password.get.toCharArray)
  }
  configuration.sentinels.foreach {
    case RedisHost(host, port, _, _, _) =>
      redisUri.withSentinel(host, port)
  }
  private val redisClient: RedisClient = RedisClient.create(ScalaRedisClientResources.clientResources(), redisUri.build())
  private val connectionTimeout: Option[FiniteDuration] = configuration.timeout.connection
  if (connectionTimeout.nonEmpty) {
    redisClient.connect().setTimeout(JavaDuration.ofNanos(connectionTimeout.get.toNanos))
  }
  redisClient.setOptions(ScalaRedisClientResources.clientOption(configuration.timeout.redis))
  val client: RedisAsyncCommands[String, String] = redisClient.connect().async()


  // $COVERAGE-OFF$
  override def start(): Unit =
    log.info(s"Redis sentinel cache actor started. It is connected to ${configuration.toString}")

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis sentinel cache actor ...")
    redisClient.shutdown()
    log.info("Redis sentinel cache stopped.")
  }
  // $COVERAGE-ON$

}

/**
 * Creates a connection to master and slaves nodes.
 *
 * @param lifecycle
 * application lifecycle to trigger on stop hook
 * @param configuration
 * configures master-slaves
 */
//noinspection DuplicatedCode
private[connector] class RedisCommandsMasterSlaves(configuration: RedisMasterSlaves)(implicit val lifecycle: ApplicationLifecycle) extends Provider[RedisClusterAsyncCommands[String, String]] with AbstractRedisCommands {

  private val redisClient: RedisClient = RedisClient.create(ScalaRedisClientResources.clientResources())


  private val redisUri = RedisURI.Builder.redis(configuration.master.host)
    .withPort(configuration.master.port)
  private val db = if (configuration.master.database.isEmpty) configuration.database else configuration.master.database
  if (db.nonEmpty) {
    redisUri.withDatabase(db.get)
  }
  val username: Option[String] = if (configuration.master.username.isEmpty) configuration.username else configuration.master.username
  val password: Option[String] = if (configuration.master.password.isEmpty) configuration.password else configuration.master.password
  if (username.nonEmpty && password.nonEmpty) {
    redisUri.withAuthentication(username.get, password.get.toCharArray)
  } else if (password.nonEmpty) {
    redisUri.withPassword(password.get.toCharArray)
  }
  private val connectionTimeout: Option[FiniteDuration] = configuration.timeout.connection
  if (connectionTimeout.nonEmpty) {
    redisClient.connect().setTimeout(JavaDuration.ofNanos(connectionTimeout.get.toNanos))
  }
  redisClient.setOptions(ScalaRedisClientResources.clientOption(configuration.timeout.redis))
  val connection: StatefulRedisMasterReplicaConnection[String, String] = MasterReplica.connect(redisClient, StringCodec.UTF8,
    redisUri.build())
  connection.setReadFrom(ReadFrom.MASTER_PREFERRED)
  val client: RedisAsyncCommands[String, String] = connection.async()

  // $COVERAGE-OFF$
  def start(): Unit =
    log.info(s"Redis master-slaves cache actor started. It is connected to ${configuration.toString}")

  def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis master-slaves cache actor ...")
    redisClient.shutdown()
    log.info("Redis master-slaves cache stopped.")
  }
  // $COVERAGE-ON$

}
