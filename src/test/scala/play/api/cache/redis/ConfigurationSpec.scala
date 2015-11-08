package play.api.cache.redis

import scala.concurrent.duration._

import org.specs2.mutable.Specification

/**
 * <p>This test verifies reading of the configuration to deliver proper redis connection settings.</p>
 */
class ConfigurationSpec extends Specification {

  "Local configuration" should {

    val configuration = new LocalConfiguration( )

    "read host" in {
      configuration.host must beEqualTo( "localhost" )
    }

    "read port" in {
      configuration.port must beEqualTo( 6379 )
    }

    "read database" in {
      configuration.database must beEqualTo( 1 )
    }

    "read context" in {
      configuration.invocationContext must beEqualTo( "akka.actor.default-dispatcher" )
    }

    "read timeout" in {
      configuration.timeout must beEqualTo( 1.second )
    }
  }

  "Heroku configuration" should {

    val configuration = new HerokuConfigurationProvider( ) {
      /** returns the connection url to redis server */
      override protected def url: Option[ String ] = Some( "redis://admin:my-password@redis.server:1234" )
    }.get( )

    "read host" in {
      configuration.host must beEqualTo( "redis.server" )
    }

    "read port" in {
      configuration.port must beEqualTo( 1234 )
    }

    "read database" in {
      configuration.database must beEqualTo( 1 )
    }

    "read context" in {
      configuration.invocationContext must beEqualTo( "akka.actor.default-dispatcher" )
    }

    "read timeout" in {
      configuration.timeout must beEqualTo( 1.second )
    }
  }
}
