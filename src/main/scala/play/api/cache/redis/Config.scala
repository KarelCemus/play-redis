package play.api.cache.redis

/**
 * Redis cache configuration providing settings of the cache instance to be used
 *
 * @author Karel Cemus
 */
trait Config extends Implicits {

  /** configuration of the connection */
  protected def configuration: Configuration

  /** default invocation context of all cache commands */
  protected val invocationContext = configuration.invocationContext

  /** timeout of cache requests */
  protected implicit val timeout = akka.util.Timeout( configuration.timeout )

  /** host with redis server */
  protected def host = configuration.host

  /** port redis listens on */
  protected def port = configuration.port

  /** redis database to work with */
  protected def database = configuration.database
}
