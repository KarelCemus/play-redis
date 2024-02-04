package play.api.cache.redis.connector

import org.apache.pekko.actor.ActorSystem
import play.api.cache.redis._
import play.api.inject.ApplicationLifecycle

import javax.inject.Provider

/** Provides an instance of named redis connector */
private[redis] class RedisConnectorProvider(instance: RedisInstance, serializer: PekkoSerializer)(implicit system: ActorSystem, lifecycle: ApplicationLifecycle, runtime: RedisRuntime) extends Provider[RedisConnector] {

  private[connector] lazy val commands = new RedisCommandsProvider(instance).get

  lazy val get = new RedisConnectorImpl(serializer, commands)
}
