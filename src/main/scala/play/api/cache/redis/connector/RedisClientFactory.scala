package play.api.cache.redis.connector

import io.lettuce.core._
import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection
import io.lettuce.core.resource.{ClientResources, NettyCustomizer}
import io.netty.channel.{Channel, ChannelDuplexHandler, ChannelHandlerContext}
import io.netty.handler.timeout.{IdleStateEvent, IdleStateHandler}
import play.api.cache.redis.configuration.{RedisHost, RedisSslSettings, RedisUriSslSettings}

import java.time.{Duration => JavaDuration}
import scala.concurrent.duration.FiniteDuration

private object RedisClientFactory {

  implicit class RichRedisConnection[Connection <: StatefulConnection[String, String]](
    private val thiz: Connection,
  ) extends AnyVal {

    def withTimeout(maybeTimeout: Option[FiniteDuration]): Connection = {
      maybeTimeout.foreach { timeout =>
        thiz.setTimeout(JavaDuration.ofNanos(timeout.toNanos))
      }
      thiz
    }

  }

  implicit class RichRedisMasterReplicaConnection[Connection <: StatefulRedisMasterReplicaConnection[String, String]](
    private val thiz: Connection,
  ) extends AnyVal {

    def withReadFrom(readFrom: ReadFrom): Connection = {
      thiz.setReadFrom(readFrom)
      thiz
    }

  }

  implicit class RichRedisURIBuilder[Builder <: RedisURI.Builder](
    private val thiz: Builder,
  ) extends AnyVal {

    def withDatabase(database: Option[Int]): Builder = {
      thiz.withDatabase(database.getOrElse(0)) // mutable
      thiz
    }

    def withSsl(settings: RedisUriSslSettings): RedisURI.Builder =
      if (settings.enabled) {
        thiz.withSsl(true)
        thiz.withVerifyPeer(settings.verifyPeerMode.value)
      } else thiz

    def withCredentials(
      username: Option[String],
      password: Option[String],
    ): Builder =
      (username, password) match {
        case (None, None)                     =>
          thiz
        case (Some(username), Some(password)) =>
          thiz.withAuthentication(username, password) // mutable
          thiz
        case (None, Some(password))           =>
          thiz.withPassword(password.toCharArray) // mutable
          thiz
        case (Some(username), None)           =>
          throw new IllegalArgumentException(s"Username is set to $username but password is missing")
      }

    def withSentinels(sentinels: Seq[RedisHost]): Builder = {
      sentinels.foreach {
        case RedisHost(host, port, _, _, None)           =>
          thiz.withSentinel(host, port) // mutable
        case RedisHost(host, port, _, _, Some(password)) =>
          thiz.withSentinel(host, port, password) // mutable
      }
      thiz
    }

  }

  implicit class RichClientOptionsBuilder[T <: ClientOptions.Builder](
    private val thiz: T,
  ) extends AnyVal {

    def withDefaults(): T = {
      // mutable calls
      thiz.autoReconnect(true)                // Auto-Reconnect
      thiz.pingBeforeActivateConnection(true) // PING before activating connection
      thiz
    }

    def withSslSettings(sslUriSettings: RedisUriSslSettings, maybeSslSettings: Option[RedisSslSettings]): T = {
      maybeSslSettings match {
        case Some(sslSettings) => if (sslUriSettings.enabled) thiz.sslOptions(sslSettings.toOptions) else ()
        case None              => ()
      }
      thiz
    }

    def withTimeout(maybeTimeout: Option[FiniteDuration]): T = {
      val options = maybeTimeout match {
        case Some(timeout) =>
          TimeoutOptions.builder()
            .timeoutCommands(true)
            .fixedTimeout(JavaDuration.ofNanos(timeout.toNanos))
            .build()
        case None          =>
          TimeoutOptions.builder().build()
      }

      thiz.timeoutOptions(options) // mutable call
      thiz
    }

  }

  implicit class RichRedisClient[Client <: AbstractRedisClient](
    private val thiz: Client,
  ) extends AnyVal {

    def withOptions[Options <: ClientOptions](
      f: Client => Options => Unit,
    )(
      options: Options,
    ): Client = {
      f(thiz)(options)
      thiz
    }

  }

  def newClientResources(
    ioThreadPoolSize: Int = 8,
    computationThreadPoolSize: Int = 8,
    afterChannelTime: Int = 60 * 4,
  ): ClientResources =
    ClientResources.builder
      // The number of threads in the I/O thread pools.
      // The number defaults to the number of available processors that
      // the runtime returns (which, as a well-known fact, sometimes does
      // not represent the actual number of processors). Every thread
      // represents an internal event loop where all I/O tasks are run.
      // The number does not reflect the actual number of I/O threads because
      // the client requires different thread pools for Network (NIO) and
      // Unix Domain Socket (EPoll) connections. The minimum I/O threads are 3.
      // A pool with fewer threads can cause undefined behavior.
      .ioThreadPoolSize(ioThreadPoolSize)
      // The number of threads in the computation thread pool. The number
      // defaults to the number of available processors that the runtime returns
      // (which, as a well-known fact, sometimes does not represent the actual
      // number of processors). Every thread represents an internal event loop
      // where all computation tasks are run. The minimum computation threads
      // are 3. A pool with fewer threads can cause undefined behavior.
      .computationThreadPoolSize(computationThreadPoolSize)
      // Maintain connection to Redis every four minutes
      .nettyCustomizer(
        new NettyCustomizer() {

          @SuppressWarnings(Array("org.wartremover.warts.IsInstanceOf"))
          override def afterChannelInitialized(channel: Channel): Unit = {
            val _ = channel.pipeline.addLast(new IdleStateHandler(afterChannelTime, 0, 0))
            val _ = channel.pipeline.addLast(new ChannelDuplexHandler() {
              @throws[Exception]
              override def userEventTriggered(ctx: ChannelHandlerContext, evt: Object): Unit =
                if (evt.isInstanceOf[IdleStateEvent]) {
                  val _ = ctx.disconnect().sync()
                }
            })
          }

        },
      )
      .build

}
