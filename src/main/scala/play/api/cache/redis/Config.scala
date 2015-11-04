package play.api.cache.redis

import scala.concurrent.ExecutionContext

import play.api.{Application, Logger}
import play.api.libs.concurrent.Akka

/**
 * Redis cache configuration providing settings of the cache instance to be used
 *
 * @author Karel Cemus
 */
trait Config extends Implicits {

  /** Play application instance to be used */
  protected implicit def application: Application

  /** cache configuration root */
  protected def config = com.typesafe.config.ConfigFactory.load( ).getConfig( "play.cache.redis" )

  /** default invocation context of all cache commands */
  protected implicit val context: ExecutionContext = Akka.system.dispatchers.lookup( config.getString( "dispatcher" ) )

  /** timeout of cache requests */
  protected implicit val timeout = akka.util.Timeout( config.getDuration( "timeout" ) )

  /** logger instance */
  protected val log = Logger( "play.api.cache.redis" )
}
