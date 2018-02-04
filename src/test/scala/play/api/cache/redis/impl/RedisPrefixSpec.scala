package play.api.cache.redis.impl

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.mutable.Specification

/**
  * <p>Test of cache to be sure that keys are differentiated, expires etc.</p>
  */
class RedisPrefixSpec extends Specification with Redis { outer =>
  import RedisRuntime._

  private type Cache = RedisCache[ SynchronousResult ]

  private val workingConnector = injector.instanceOf[ RedisConnector ]

  def runtime( prefix: Option[ String ] ) =  RedisRuntime( "play", 3.minutes, ExecutionContext.Implicits.global, FailThrough, invocation = LazyInvocation, prefix )

  val unprefixed = new RedisCache( workingConnector, Builders.SynchronousBuilder )( runtime( prefix = None ) )

  "RedisPrefix" should {

    "apply when defined" in {
      val cache = new RedisCache( workingConnector, Builders.SynchronousBuilder )( runtime( prefix = Some( "prefixed"  ) ) )
      workingConnector.get[ String ]( "prefixed:prefix-test:defined" ).sync must beNone
      cache.set( "prefix-test:defined", "value" )
      workingConnector.get[ String ]( "prefixed:prefix-test:defined" ).sync must beSome( "value" )
    }

    "not apply when is empty" in {
      val cache = new RedisCache( workingConnector, Builders.SynchronousBuilder )( runtime( prefix = None ) )
      workingConnector.get[ String ]( "prefix-test:defined" ).sync must beNone
      cache.set( "prefix-test:defined", "value" )
      workingConnector.get[ String ]( "prefix-test:defined" ).sync must beSome( "value" )
    }
  }
}
