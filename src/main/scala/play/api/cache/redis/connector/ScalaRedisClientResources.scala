package play.api.cache.redis.connector

import io.lettuce.core.{ClientOptions, TimeoutOptions}
import io.lettuce.core.resource.{ClientResources, NettyCustomizer}
import io.netty.channel.{Channel, ChannelDuplexHandler, ChannelHandlerContext}
import io.netty.handler.timeout.{IdleStateEvent, IdleStateHandler}

import scala.concurrent.duration.FiniteDuration
import java.time.{Duration => JavaDuration}

/**
 * Custom link pool
 */
object ScalaRedisClientResources {
  def clientResources(ioThreadPoolSize: Int = 8,
                      computationThreadPoolSize: Int = 8,
                      afterChannelTime: Int = 60 * 4
                     ): ClientResources = {
    val nettyCustomizer = new NettyCustomizer() {
      override def afterChannelInitialized(channel: Channel): Unit = {
        //此处事件必须小于超时时间
        channel.pipeline.addLast(new IdleStateHandler(afterChannelTime, 0, 0))
        channel.pipeline.addLast(new ChannelDuplexHandler() {
          @throws[Exception]
          override def userEventTriggered(ctx: ChannelHandlerContext, evt: Object): Unit = {
            if (evt.isInstanceOf[IdleStateEvent]) ctx.disconnect
          }
        })
      }
    }
    ClientResources.builder
      .ioThreadPoolSize(ioThreadPoolSize) //The number of threads in the I/O thread pools. The number defaults to the number of available processors that the runtime returns (which, as a well-known fact, sometimes does not represent the actual number of processors). Every thread represents an internal event loop where all I/O tasks are run. The number does not reflect the actual number of I/O threads because the client requires different thread pools for Network (NIO) and Unix Domain Socket (EPoll) connections. The minimum I/O threads are 3. A pool with fewer threads can cause undefined behavior.
      .computationThreadPoolSize(computationThreadPoolSize) //The number of threads in the computation thread pool. The number defaults to the number of available processors that the runtime returns (which, as a well-known fact, sometimes does not represent the actual number of processors). Every thread represents an internal event loop where all computation tasks are run. The minimum computation threads are 3. A pool with fewer threads can cause undefined behavior.
      .nettyCustomizer(nettyCustomizer)
      //Maintain connection to Redis every four minutes
      .build
  }

  def timeoutOptions(timeout: Option[FiniteDuration]): TimeoutOptions = {
    val timeoutOptions: TimeoutOptions = if (timeout.nonEmpty) {
      TimeoutOptions.builder()
        .timeoutCommands(true)
        .fixedTimeout(JavaDuration.ofNanos(timeout.get.toNanos))
        .build()
    } else {
      TimeoutOptions.builder()
        .build()
    }
    timeoutOptions
  }

  def clientOption(timeout: Option[FiniteDuration]): ClientOptions = {
    ClientOptions.builder()
      .autoReconnect(true) //Auto-Reconnect
      .pingBeforeActivateConnection(true) //PING before activating connection
      .timeoutOptions(timeoutOptions(timeout))
      .build()
  }

}