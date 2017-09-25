package play.api.cache.redis.connector

import javax.inject.Provider

import play.api.cache.redis._
import play.api.inject.ApplicationLifecycle

import akka.actor.ActorSystem

/**
  * Provides an instance of named redis connector
  *
  * @author Karel Cemus
  */
private[ redis ] class RedisConnectorProvider( instance: RedisInstance, serializer: AkkaSerializer )( implicit system: ActorSystem, lifecycle: ApplicationLifecycle, runtime: RedisRuntime ) extends Provider[ RedisConnector ] {

  private lazy val commands = new RedisCommandsProvider( instance ).get

  lazy val get = new RedisConnectorImpl( serializer, commands )
}
