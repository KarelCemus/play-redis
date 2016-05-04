package play.api.cache.redis

import scala.concurrent.duration._

import play.api.cache.redis.configuration.{EnvironmentConfigurationProvider, StaticConfiguration}

import org.specs2.mutable.Specification

/**
 * <p>This test verifies reading of the configuration to deliver proper redis connection settings.</p>
 */
class ConfigurationSpec extends Specification {

  "Local configuration" should {

    val configuration = new StaticConfiguration( )

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

    "read password" in {
      configuration.password must beNone
    }
  }

  "Heroku configuration" should {

    val configuration = new EnvironmentConfigurationProvider( "undefined" ) {
      override protected def url = Some( "redis://h:my-password@redis.server:1234" )
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

    "read password" in {
      configuration.password must beSome( "my-password" )
    }

    "without password" in {
      new EnvironmentConfigurationProvider( "undefined" ) {
        override protected def url = Some( "redis://redis.server:1234" )
      }.get( ).password must beNone
    }
  }
}
