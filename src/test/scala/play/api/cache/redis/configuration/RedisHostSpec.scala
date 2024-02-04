package play.api.cache.redis.configuration

import play.api.ConfigLoader
import play.api.cache.redis.test._

class RedisHostSpec extends UnitSpec with ImplicitOptionMaterialization {

  implicit private val loader: ConfigLoader[RedisHost] = RedisHost

  "host with database, username, and password" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis {
        |  host:     localhost
        |  port:     6378
        |  database: 1
        |  username: my-user
        |  password: something
        |}
      """.stripMargin
    }
    configuration.get[RedisHost]("play.cache.redis") mustEqual RedisHost(
      host = "localhost",
      port = 6378,
      database = 1,
      username = "my-user",
      password = "something",
    )
  }

  "host with database, password but without a username" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis {
        |  host:     localhost
        |  port:     6378
        |  database: 1
        |  password: something
        |}
      """.stripMargin
    }
    configuration.get[RedisHost]("play.cache.redis") mustEqual RedisHost(
      host = "localhost",
      port = 6378,
      database = 1,
      username = None,
      password = "something",
    )
  }

  "host without database and password" in {
    val configuration = Helpers.configuration.fromHocon {
      """play.cache.redis {
        |  host:     localhost
        |  port:     6378
        |}
      """.stripMargin
    }
    configuration.get[RedisHost]("play.cache.redis") mustEqual RedisHost("localhost", 6378, database = 0)
  }

  "host from connection string" in {
    RedisHost.fromConnectionString("redis://redis:something@localhost:6378") mustEqual RedisHost("localhost", 6378, username = "redis", password = "something")
    RedisHost.fromConnectionString("redis://localhost:6378") mustEqual RedisHost("localhost", 6378)
    // test invalid string
    assertThrows[IllegalArgumentException] {
      RedisHost.fromConnectionString("redis:/localhost:6378")
    }
  }

}
