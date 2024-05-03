package play.api.cache.redis.connector

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps

sealed private trait RedisConnection extends Any {

  def close()(implicit ec: ExecutionContext): Future[Unit]

  def api: RedisClusterAsyncCommands[String, String]
}

private object RedisConnection {

  final private class StandaloneConnection(
    private val connection: StatefulRedisConnection[String, String],
  ) extends AnyVal
    with RedisConnection {

    override def close()(implicit ec: ExecutionContext): Future[Unit] =
      connection.closeAsync().asScala.map(_ => ())

    override def api: RedisClusterAsyncCommands[String, String] =
      connection.async()

  }

  final private class ClusterConnection(
    private val connection: StatefulRedisClusterConnection[String, String],
  ) extends AnyVal
    with RedisConnection {

    override def close()(implicit ec: ExecutionContext): Future[Unit] =
      connection.closeAsync().asScala.map(_ => ())

    override def api: RedisClusterAsyncCommands[String, String] =
      connection.async()

  }

  def fromStandalone(
    connection: StatefulRedisConnection[String, String],
  ): RedisConnection =
    new StandaloneConnection(connection)

  def fromCluster(
    connection: StatefulRedisClusterConnection[String, String],
  ): RedisConnection =
    new ClusterConnection(connection)

}
