package play.api.cache.redis.logging

import akka.event.Logging.{InitializeLogger, LoggerInitialized}
import akka.event.slf4j.Slf4jLogger

/**
  * This logger fixes initialization issues; it fixes race conditions between
  * Akka and Slf4j, this ensures that Slf4j is initialized first.
  */
class RedisLogger extends Slf4jLogger {

  private def doReceive: PartialFunction[Any, Unit] = {
    case InitializeLogger(_) ⇒ sender() ! LoggerInitialized
  }

  override def receive = {
    doReceive.orElse(super.receive)
  }
}
