package play.api.cache.redis

import javax.inject.{Inject, Provider}

import play.api.cache.redis.configuration.{RedisInstanceManager, RedisInstanceResolver}
import play.api.cache.redis.connector.{AkkaSerializer, RedisConnectorProvider}
import play.api.inject._
import play.api.{Configuration, Environment}

import akka.actor.ActorSystem

/**
  * @author Karel Cemus
  */
class GuiceRedisConnectorProvider  @Inject()( serializer: AkkaSerializer, configuration: Configuration )( implicit resolver: RedisInstanceResolver, system: ActorSystem, lifecycle: ApplicationLifecycle ) extends Provider[ RedisConnector ] {

  private val instance = configuration.get( "play.cache.redis" )( RedisInstanceManager ).defaultInstance.resolved

  private implicit def runtime = new connector.RedisRuntime {
    def name = instance.name
    implicit def context = system.dispatchers.lookup( instance.invocationContext )
  }

  lazy val get = new RedisConnectorProvider( instance, serializer ).get
}

class TestModule extends Module {
  def bindings( environment: Environment, configuration: Configuration ) = Seq(
    bind[ connector.RedisConnector ].toProvider[ GuiceRedisConnectorProvider ]
  )
}
