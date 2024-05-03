package play.api.cache.redis.connector

import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.{ClusterClientOptions, ClusterTopologyRefreshOptions, RedisClusterClient}
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.masterreplica.MasterReplica
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.{AbstractRedisClient, ClientOptions, ReadFrom, RedisClient, RedisURI}
import play.api.Logger
import play.api.cache.redis.configuration._
import play.api.inject.ApplicationLifecycle

import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.jdk.FutureConverters.CompletionStageOps

/**
  * Dispatches a provider of the redis commands implementation. Use with Guice
  * or some other DI container.
  */
private[connector] class RedisCommandsProvider(
  instance: RedisInstance,
)(implicit
  lifecycle: ApplicationLifecycle,
  executionContext: ExecutionContext,
) extends Provider[RedisClusterAsyncCommands[String, String]] {

  lazy val get: RedisClusterAsyncCommands[String, String] = instance match {
    case cluster: RedisCluster           => new RedisCommandsCluster(cluster).get
    case standalone: RedisStandalone     => new RedisCommandsStandalone(standalone).get
    case sentinel: RedisSentinel         => new RedisCommandsSentinel(sentinel).get
    case masterSlaves: RedisMasterSlaves => new RedisCommandsMasterSlaves(masterSlaves).get
  }

}

abstract private[connector] class AbstractRedisCommands(
  protected val name: String,
)(implicit
  executionContext: ExecutionContext,
  lifecycle: ApplicationLifecycle,
) {

  /** logger instance */
  protected def log: Logger = Logger("play.api.cache.redis")

  protected def connectionString: String

  protected lazy val resources: ClientResources = RedisClientFactory.newClientResources()

  protected def client: AbstractRedisClient

  /** an implementation of the redis commands */
  protected def newConnection: RedisConnection

  private lazy val connection = newConnection

  lazy val get: RedisClusterAsyncCommands[String, String] = {
    // start the connector
    start()
    // listen on system stop
    lifecycle.addStopHook(() => stop())
    // make the client
    connection.api
  }

  // $COVERAGE-OFF$
  /** action invoked on the start of the redis client */
  def start(): Unit =
    log.info(s"Starting $name. It will connect to $connectionString")

  /** stops the client */
  final def stop(): Future[Unit] =
    for {
      _ <- Future.unit
      _ = log.info(s"Stopping $name ...")
      _ <- connection.close().recover { case ex =>
             log.warn("Error while closing the redis connection", ex)
           }
      _ <- client.shutdownAsync().asScala.map(_ => ()).recover { case ex =>
             log.warn("Error while shutting down the redis client", ex)
           }
      _ <- Future.apply(resources.shutdown().get()).map(_ => ()).recover { case ex =>
             log.warn("Error while shutting down client resources", ex)
           }
      _ = log.info(s"Stopped $name.")
    } yield ()
  // $COVERAGE-ON$

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
  executionContext: ExecutionContext,
  lifecycle: ApplicationLifecycle,
) extends AbstractRedisCommands("standalone redis")
  with Provider[RedisClusterAsyncCommands[String, String]] {

  import RedisClientFactory._

  private val redisUri: RedisURI =
    RedisURI.Builder
      .redis(configuration.host)
      .withPort(configuration.port)
      .withDatabase(configuration.database)
      .withCredentials(configuration.username, configuration.password)
      .build()

  override protected def connectionString: String =
    redisUri.toString

  override protected lazy val client: RedisClient =
    RedisClient.create(resources, redisUri)
      .withOptions(_.setOptions)(
        ClientOptions.builder()
          .withDefaults()
          .withTimeout(configuration.timeout.redis)
          .build(),
      )

  override protected def newConnection: RedisConnection =
    RedisConnection.fromStandalone(
      client.connect().withTimeout(configuration.timeout.connection),
    )

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
  lifecycle: ApplicationLifecycle,
  executionContext: ExecutionContext,
) extends AbstractRedisCommands("redis cluster")
  with Provider[RedisClusterAsyncCommands[String, String]] {

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

  override protected def connectionString: String = redisUris.map(_.toString).mkString(", ")

  override protected val client: RedisClusterClient =
    RedisClusterClient.create(resources, redisUris.asJava)
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

  val newConnection: RedisConnection =
    RedisConnection.fromCluster(
      client.connect().withTimeout(configuration.timeout.connection),
    )

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
  lifecycle: ApplicationLifecycle,
  executionContext: ExecutionContext,
) extends AbstractRedisCommands("redis sentinel")
  with Provider[RedisClusterAsyncCommands[String, String]] {

  import RedisClientFactory._

  private val sentinel: RedisHost =
    configuration.sentinels.head

  private val redisUri: RedisURI =
    RedisURI.Builder
      .sentinel(sentinel.host, sentinel.port)
      .withDatabase(configuration.database)
      .withCredentials(configuration.username, configuration.password)
      .withSentinels(configuration.sentinels)
      .build()

  override protected def connectionString: String = redisUri.toString

  override protected val client: RedisClient =
    RedisClient.create(resources, redisUri)
      .withOptions(_.setOptions)(
        ClientOptions.builder()
          .withDefaults()
          .withTimeout(configuration.timeout.redis)
          .build(),
      )

  val newConnection: RedisConnection =
    RedisConnection.fromStandalone(
      client.connect().withTimeout(configuration.timeout.connection),
    )

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
  lifecycle: ApplicationLifecycle,
  executionContext: ExecutionContext,
) extends AbstractRedisCommands("redis master-slaves")
  with Provider[RedisClusterAsyncCommands[String, String]] {

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

  override protected def connectionString: String = redisUri.toString

  override protected val client: RedisClient =
    RedisClient.create(resources)
      .withOptions(_.setOptions)(
        ClientOptions.builder()
          .withDefaults()
          .withTimeout(configuration.timeout.redis)
          .build(),
      )

  val newConnection: RedisConnection =
    RedisConnection.fromStandalone(
      MasterReplica.connect(client, StringCodec.UTF8, redisUri)
        .withReadFrom(ReadFrom.MASTER_PREFERRED),
    )

}
