package play.api.cache.redis.configuration

import play.api.Configuration

import com.typesafe.config._
import org.specs2.execute._
import org.specs2.specification._

abstract class WithConfiguration( hocon: String ) extends Around with Scope {

  protected val config = {
    val reference = ConfigFactory.load()
    val local = ConfigFactory.parseString( hocon.stripMargin )
    local.withFallback( reference )
  }

  protected val configuration = Configuration( config )

  override def around[ T: AsResult ]( t: => T ): Result = {
    AsResult.effectively( t )
  }
}
