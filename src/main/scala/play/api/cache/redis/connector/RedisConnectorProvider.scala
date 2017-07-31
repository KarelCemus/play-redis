package play.api.cache.redis.connector

import javax.inject.{Inject, Provider}

import play.api.cache.redis.configuration.RedisInstance
import play.api.inject.{Injector, bind}

import akka.actor.ActorSystem
import redis.RedisCommands

/**
  * Provides an instance of named redis connector
  *
  * @author Karel Cemus
  */
class RedisConnectorProvider( name: String ) extends Provider[ RedisConnector ] {

  @Inject private var injector: Injector = _
  @Inject private var serializer: AkkaSerializer = _
  @Inject private implicit var system: ActorSystem = _

  private def instance = bind[ RedisInstance ].qualifiedWith( name )
  private def commands = bind[ RedisCommands ].qualifiedWith( name )

  lazy val get = new RedisConnectorImpl( serializer, injector instanceOf instance, injector instanceOf commands )
}
