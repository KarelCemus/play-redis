package play.api.cache.redis.configuration

import org.specs2.mutable.Specification

class RedisHostSpec extends Specification {

  // implicitly expose RedisHost config loader
  implicit val loader = RedisHost

  "RedisHost" should "read" >> {

    "standalone" in new WithConfiguration(
      """
        |instance {
        | host: localhost
        | port: 6379
        |}
      """.stripMargin
    ) {
      config.get[ RedisHost ]( "instance" ) mustEqual RedisHost( host = "localhost", port = 6379 )
    }

    "standalone with password" in new WithConfiguration(
      """
        |instance {
        | host: localhost
        | port: 6379
        | password: "my password"
        |}
      """.stripMargin
    ) {
      config.get[ RedisHost ]( "instance" ) mustEqual RedisHost( host = "localhost", port = 6379, password = Some( "my password" ) )
    }

    "standalone with database" in new WithConfiguration(
      """
        |instance {
        | host: localhost
        | port: 6379
        | database: 1
        |}
      """.stripMargin
    ) {
      config.get[ RedisHost ]( "instance" ) mustEqual RedisHost( host = "localhost", port = 6379, database = Some( 1 ) )
    }

    "connection string" in {
      RedisHost.fromConnectionString( "redis://localhost:6379" ) mustEqual RedisHost( host = "localhost", port = 6379 )
      RedisHost.fromConnectionString( "redis://redis:my-password@localhost:6379" ) mustEqual RedisHost( host = "localhost", port = 6379, password = Some( "my-password" ) )
    }
  }
}
