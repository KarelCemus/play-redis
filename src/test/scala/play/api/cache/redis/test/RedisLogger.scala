package play.api.cache.redis.test

import akka.event.Logging.{InitializeLogger, LoggerInitialized}
import akka.event.slf4j.Slf4jLogger

/**
  * This logger fixes initialization issues; it fixes race conditions between
  * Akka and Slf4j, this ensures that Slf4j is initialized first.
  */
class RedisLogger extends Slf4jLogger {

  private val doReceive: PartialFunction[Any, Unit] = { case InitializeLogger(_) =>
    sender() ! LoggerInitialized
  }

  override def receive: PartialFunction[Any, Unit] =
    doReceive.orElse(super.receive)

}
