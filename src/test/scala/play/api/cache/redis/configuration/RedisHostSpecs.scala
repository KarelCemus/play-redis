package play.api.cache.redis.configuration

import play.api.cache.redis._

import org.specs2.mutable.Spec

/**
  * @author Karel Cemus
  */
class RedisHostSpecs extends Spec {
  import Implicits._

  private implicit val loader = RedisHost

  "host with database and password" in new WithConfiguration(
    """
      |play.cache.redis {
      |  host:     localhost
      |  port:     6378
      |  database: 1
      |  password: something
      |}
    """
  ) {
    configuration.get[ RedisHost ]( "play.cache.redis" ) mustEqual RedisHost( "localhost", 6378, database = 1, password = "something" )
  }

  "host without database and password" in new WithConfiguration(
    """
      |play.cache.redis {
      |  host:     localhost
      |  port:     6378
      |}
    """
  ) {
    configuration.get[ RedisHost ]( "play.cache.redis" ) mustEqual RedisHost( "localhost", 6378, database = 0 )
  }

  "host from connection string" in {
    RedisHost.fromConnectionString( "redis://redis:something@localhost:6378" ) mustEqual RedisHost( "localhost", 6378, password = "something" )
    RedisHost.fromConnectionString( "redis://localhost:6378" ) mustEqual RedisHost( "localhost", 6378 )
    // test invalid string
    RedisHost.fromConnectionString( "redis:/localhost:6378" ) must throwA[ IllegalArgumentException ]
  }
}
