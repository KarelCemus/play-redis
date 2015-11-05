package play.api.cache.redis

/**
 * Redis cache configuration providing settings of the cache instance to be used
 *
 * @author Karel Cemus
 */
trait Config extends Implicits {

  /** cache configuration root */
  protected def config = com.typesafe.config.ConfigFactory.load( ).getConfig( "play.cache.redis" )

  /** default invocation context of all cache commands */
  protected val invocationContext = config.getString( "dispatcher" )

  /** timeout of cache requests */
  protected implicit val timeout = akka.util.Timeout( config.getDuration( "timeout" ) )

  /** duration to wait before redis answers */
  protected def synchronizationTimeout = config.getDuration( "wait" )

  /** host with redis server */
  protected def host = config.getString( "host" )

  /** port redis listens on */
  protected def port = config.getInt( "port" )

  /** redis database to work with */
  protected def database = config.getInt( "database" )
}
