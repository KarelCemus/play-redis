package play.api.cache.redis.configuration

import play.api.Configuration

import com.typesafe.config.ConfigFactory
import org.specs2.execute.AsResult
import org.specs2.mutable.Around
import org.specs2.specification.Scope

/**
  * A helper parses a configuration provided into the example
  *
  * @author Karel Cemus
  */
class WithConfiguration( hocon: String ) extends Around with Scope {

  val config = Configuration {
    ConfigFactory.parseString( hocon.stripMargin )
  }

  def around[ T ]( t: => T )( implicit evidence$6: AsResult[ T ] ) =
    AsResult.effectively( t )
}
