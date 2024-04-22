package play.api.cache.redis.connector

import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.{ClusterClientOptions, ClusterTopologyRefreshOptions, RedisClusterClient}
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.masterreplica.MasterReplica
import io.lettuce.core.{ClientOptions, ReadFrom, RedisClient, RedisURI}
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
private[connector] class RedisCommandsProvider(
  instance: RedisInstance,
)(implicit
  lifecycle: ApplicationLifecycle,
) extends Provider[RedisClusterAsyncCommands[String, String]] {

  lazy val get: RedisClusterAsyncCommands[String, String] = instance match {
    case cluster: RedisCluster           => new RedisCommandsCluster(cluster).get
    case standalone: RedisStandalone     => new RedisCommandsStandalone(standalone).get
    case sentinel: RedisSentinel         => new RedisCommandsSentinel(sentinel).get
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
  *   application lifecycle to trigger on stop hook
  * @param configuration
  *   configures clusters
  */
private[connector] class RedisCommandsStandalone(
  configuration: RedisStandalone,
)(implicit
  val lifecycle: ApplicationLifecycle,
) extends Provider[RedisClusterAsyncCommands[String, String]]
  with AbstractRedisCommands {

  import RedisClientFactory._

  private val redisUri = RedisURI.Builder
    .redis(configuration.host)
    .withPort(configuration.port)
    .withDatabase(configuration.database)
    .withCredentials(configuration.username, configuration.password)
    .build()

  private val redisClient: RedisClient =
    RedisClient.create(clientResources(), redisUri)
      .withOptions(_.setOptions)(
        ClientOptions.builder()
          .withDefaults()
          .withTimeout(configuration.timeout.redis)
          .build(),
      )

  val client: RedisAsyncCommands[String, String] =
    redisClient.connect().withTimeout(configuration.timeout.connection).async()

  // $COVERAGE-OFF$
  override def start(): Unit =
    log.info(s"Redis cache started. It is connected to $redisUri")

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cache ...")
    redisClient.shutdown()
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
  */
private[connector] class RedisCommandsCluster(
  configuration: RedisCluster,
)(implicit
  val lifecycle: ApplicationLifecycle,
) extends Provider[RedisClusterAsyncCommands[String, String]]
  with AbstractRedisCommands {

  import RedisClientFactory._

  private val redisUris: Seq[RedisURI] =
    configuration.nodes.map { case RedisHost(host, port, database, password, username) =>
      RedisURI.Builder
        .redis(host)
        .withPort(port)
        .withDatabase(database)
        .withCredentials(username, password)
        .build()
    }

  private val redisClient: RedisClusterClient =
    RedisClusterClient.create(clientResources(), redisUris.asJava)
      .withOptions(_.setOptions)(
        ClusterClientOptions.builder()
          .withDefaults()
          .withTimeout(configuration.timeout.redis)
          .topologyRefreshOptions(
            ClusterTopologyRefreshOptions.builder
              .enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT, RefreshTrigger.PERSISTENT_RECONNECTS)
              .adaptiveRefreshTriggersTimeout(Duration.ofNanos(TimeUnit.SECONDS.toNanos(30)))
              .build,
          )
          .build(),
      )

  val client: RedisClusterAsyncCommands[String, String] =
    redisClient.connect().withTimeout(configuration.timeout.connection).async()

  // $COVERAGE-OFF$
  override def start(): Unit = {
    def servers: String = redisUris.map(_.toString).mkString(", ")
    log.info(s"Redis cluster cache started. It is connected to $servers")
  }

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis cluster cache ...")
    redisClient.shutdown()
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
  */
private[connector] class RedisCommandsSentinel(
  configuration: RedisSentinel,
)(implicit
  val lifecycle: ApplicationLifecycle,
) extends Provider[RedisClusterAsyncCommands[String, String]]
  with AbstractRedisCommands {

  import RedisClientFactory._

  private val sentinel: RedisHost = configuration.sentinels.head

  private val redisUri: RedisURI =
    RedisURI.Builder
      .sentinel(sentinel.host, sentinel.port)
      .withDatabase(configuration.database)
      .withCredentials(configuration.username, configuration.password)
      .withSentinels(configuration.sentinels)
      .build()

  private val redisClient: RedisClient =
    RedisClient.create(clientResources(), redisUri)
      .withOptions(_.setOptions)(
        ClientOptions.builder()
          .withDefaults()
          .withTimeout(configuration.timeout.redis)
          .build(),
      )

  val client: RedisAsyncCommands[String, String] =
    redisClient.connect().withTimeout(configuration.timeout.connection).async()

  // $COVERAGE-OFF$
  override def start(): Unit =
    log.info(s"Redis sentinel cache started. It is connected to $redisUri")

  override def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis sentinel cache ...")
    redisClient.shutdown()
    log.info("Redis sentinel cache stopped.")
  }
  // $COVERAGE-ON$

}

/**
  * Creates a connection to master and slaves nodes.
  *
  * @param lifecycle
  *   application lifecycle to trigger on stop hook
  * @param configuration
  *   configures master-slaves
  */
//noinspection DuplicatedCode
private[connector] class RedisCommandsMasterSlaves(
  configuration: RedisMasterSlaves,
)(implicit
  val lifecycle: ApplicationLifecycle,
) extends Provider[RedisClusterAsyncCommands[String, String]]
  with AbstractRedisCommands {

  import RedisClientFactory._

  private val redisUri: RedisURI =
    RedisURI.Builder
      .redis(configuration.master.host)
      .withPort(configuration.master.port)
      .withDatabase(configuration.master.database.orElse(configuration.database))
      .withCredentials(
        configuration.master.username.orElse(configuration.username),
        configuration.master.password.orElse(configuration.password),
      )
      .build()

  private val redisClient: RedisClient =
    RedisClient.create(clientResources())
      .withOptions(_.setOptions)(
        ClientOptions.builder()
          .withDefaults()
          .withTimeout(configuration.timeout.redis)
          .build(),
      )

  val client: RedisAsyncCommands[String, String] =
    MasterReplica.connect(redisClient, StringCodec.UTF8, redisUri)
      .withReadFrom(ReadFrom.MASTER_PREFERRED)
      .async()

  // $COVERAGE-OFF$
  def start(): Unit =
    log.info(s"Redis master-slaves cache started. It is connected to $redisUri")

  def stop(): Future[Unit] = Future successful {
    log.info("Stopping the redis master-slaves cache ...")
    redisClient.shutdown()
    log.info("Redis master-slaves cache stopped.")
  }
  // $COVERAGE-ON$

}
