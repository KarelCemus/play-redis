package play.api.cache.redis.configuration

/**
  * Simple implementation for tests
  */
case class RedisInstanceManagerTest( default: String )( providers: RedisInstanceProvider* ) extends RedisInstanceManager {

  def caches = providers.map( _.name ).toSet

  def instanceOfOption( name: String ) = providers.find( _.name == name )

  def defaultInstance = providers.find( _.name == default ) getOrElse {
    throw new RuntimeException( "Default instance is not defined." )
  }
}

abstract class WithRedisInstanceManager( hocon: String ) extends WithConfiguration( hocon ) {

  private implicit val loader = RedisInstanceManager

  protected val manager = configuration.get[ RedisInstanceManager ]( "play.cache.redis" )
}
