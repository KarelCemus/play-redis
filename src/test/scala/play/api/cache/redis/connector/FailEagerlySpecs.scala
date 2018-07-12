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
class FailEagerlySpecs( implicit ee: ExecutionEnv ) extends Specification with WithApplication {

  import FailEagerlySpecs._
  import Implicits._
  import MockitoImplicits._

  "FailEagerly" should {

    "not fail regular requests when disconnected" in {
      val impl = new FailEagerlyImpl
      val cmd = mock[ RedisCommandTest ].returning returns "response"
      // run the test
      impl.isConnected must beFalse
      impl.send[ String ]( cmd ) must beEqualTo( "response" ).await
    }

    "do fail long running requests when disconnected" in {
      val impl = new FailEagerlyImpl
      val cmd = mock[ RedisCommandTest ].returning returns Future.after( seconds = 3, "response" )
      // run the test
      impl.isConnected must beFalse
      impl.send[ String ]( cmd ) must throwA[ redis.actors.NoConnectionException.type ].awaitFor( 5.seconds )
    }

    "not fail long running requests when connected " in {
      val impl = new FailEagerlyImpl
      val cmd = mock[ RedisCommandTest ].returning returns Future.after( seconds = 3, "response" )
      impl.markConnected()
      // run the test
      impl.isConnected must beTrue
      impl.send[ String ]( cmd ) must beEqualTo( "response" ).awaitFor( 5.seconds )
    }
  }
}

object FailEagerlySpecs {

  import redis.RedisCommand
  import redis.protocol.RedisReply

  trait RedisCommandTest extends RedisCommand[ RedisReply, String ] {
    def returning: Future[ String ]
  }

  class FailEagerlyBase( implicit system: ActorSystem ) extends RequestTimeout {
    protected implicit val scheduler = system.scheduler
    implicit val executionContext = system.dispatcher

    def send[ T ]( redisCommand: RedisCommand[ _ <: RedisReply, T ] ) = {
      redisCommand.asInstanceOf[ RedisCommandTest ].returning.asInstanceOf[ Future[ T ] ]
    }
  }

  class FailEagerlyImpl( implicit system: ActorSystem ) extends FailEagerlyBase with FailEagerly {

    def connectionTimeout = Some( 300.millis )

    def isConnected = connected

    def markConnected( ) = connected = true

    def markDisconnected( ) = connected = false
  }
}
