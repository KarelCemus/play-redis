package play.api.cache.redis.test

import org.apache.pekko.event.Logging.{InitializeLogger, LoggerInitialized}
import org.apache.pekko.event.slf4j.Slf4jLogger

/**
  * This logger fixes initialization issues; it fixes race conditions between
  * Pekko and Slf4j, this ensures that Slf4j is initialized first.
  */
class RedisLogger extends Slf4jLogger {

  private val doReceive: PartialFunction[Any, Unit] = { case InitializeLogger(_) =>
    sender() ! LoggerInitialized
  }

  override def receive: PartialFunction[Any, Unit] =
    doReceive.orElse(super.receive)

}
