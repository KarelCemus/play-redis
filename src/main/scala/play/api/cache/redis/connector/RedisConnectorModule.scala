package play.api.cache.redis.connector

import play.api.inject._
import play.api.{Configuration, Environment}

/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
object RedisConnectorModule extends Module {

  override def bindings( environment: Environment, configuration: Configuration ): Seq[ Binding[ _ ] ] = Seq(
    // binds akka serializer to its implementation
    bind[ AkkaSerializer ].to[ AkkaSerializerImpl ],
    // redis connector implementing the protocol
    bind[ RedisConnector ].to[ RedisConnectorImpl ]
  )
}

/**
  * Components for compile-time dependency injection.
  * It binds components for connector package
  *
  * @author Karel Cemus
  */
private[ redis ] trait RedisConnectorComponents {

  import play.api.cache.redis._
  import akka.actor.ActorSystem

  def actorSystem: ActorSystem
  def applicationLifecycle: ApplicationLifecycle
  def redisConfiguration: RedisConfiguration

  private lazy val akkaSerializer: AkkaSerializer = new AkkaSerializerImpl( actorSystem )

  lazy val redisConnector: RedisConnector = new RedisConnectorImpl( akkaSerializer, redisConfiguration, applicationLifecycle )( actorSystem )
}
