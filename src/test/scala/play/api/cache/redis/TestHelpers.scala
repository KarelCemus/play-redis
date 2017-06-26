package play.api.cache.redis

import scala.concurrent.Await
import scala.reflect.ClassTag

import play.api.cache.redis.connector.AkkaSerializer

import akka.util.Timeout

/**
  * TODO: Remove and refactor
  *
  * this helper exists as part of optimization of implicit classes.
  * However, as the tests are about to be refactored, this is the
  * easiest solution.
  *
  * @author Karel Cemus
  */
object TestHelpers {

  implicit class ValueEncoder( val any: Any ) extends AnyVal {
    def encoded( implicit serializer: AkkaSerializer ): String = serializer.encode( any ).get
  }

  implicit class StringDecoder( val string: String ) extends AnyVal {
    def decoded[ T: ClassTag ]( implicit serializer: AkkaSerializer ): T = serializer.decode[ T ]( string ).get
  }

  implicit class Key( val key: String ) extends AnyVal

  /** waits for future responses and returns them synchronously */
  implicit class Synchronizer[ T ]( val future: AsynchronousResult[ T ] ) extends AnyVal{
    def sync( implicit timeout: Timeout ) = Await.result( future, timeout.duration )
  }
}
