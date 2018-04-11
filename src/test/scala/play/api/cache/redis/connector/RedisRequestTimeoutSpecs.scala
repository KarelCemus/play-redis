package play.api.cache.redis.connector

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.cache.redis._

import akka.actor.ActorSystem
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

/**
  * @author Karel Cemus
  */
class RedisRequestTimeoutSpecs( implicit ee: ExecutionEnv ) extends Specification with WithApplication {

  import Implicits._
  import MockitoImplicits._
  import RedisRequestTimeoutSpecs._

  "RedisRequestTimeout" should {

    "fail long running requests when connected but timeout defined" in {
      val impl = new RedisRequestTimeoutImpl( timeout = 1.second )
      val cmd = mock[ RedisCommandTest ].returning returns Future.after( seconds = 3, "response" )
      // run the test
      impl.send[ String ]( cmd ) must throwA[ redis.actors.NoConnectionException.type ].awaitFor( 5.seconds )
    }
  }
}

object RedisRequestTimeoutSpecs {

  import redis.RedisCommand
  import redis.protocol.RedisReply

  trait RedisCommandTest extends RedisCommand[ RedisReply, String ] {
    def returning: Future[ String ]
  }

  class RequestTimeoutBase( implicit system: ActorSystem ) extends RequestTimeout {
    protected implicit val scheduler = system.scheduler
    implicit val executionContext = system.dispatcher

    def send[ T ]( redisCommand: RedisCommand[ _ <: RedisReply, T ] ) = {
      redisCommand.asInstanceOf[ RedisCommandTest ].returning.asInstanceOf[ Future[ T ] ]
    }
  }

  class RedisRequestTimeoutImpl( val timeout: Option[ FiniteDuration ] )( implicit system: ActorSystem ) extends RequestTimeoutBase with RedisRequestTimeout
}
