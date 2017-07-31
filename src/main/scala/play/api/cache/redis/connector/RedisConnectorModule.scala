package play.api.cache.redis.connector

import play.api.Configuration
import play.api.cache.redis._
import play.api.cache.redis.configuration.{RedisCluster, RedisStandalone}
import play.api.inject._

import redis.RedisCommands

/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
trait RedisConnectorModule {

  def configuration: Configuration

  def manager: RedisInstanceManager

  private def bindings: Seq[ Binding[ _ ] ] = manager.flatMap( cache =>
    Seq(
      bind[ RedisCommands ].qualifiedWith( cache.name ).to( new RedisCommandsProvider( cache.name ) ),
      bind[ RedisConnector ].qualifiedWith( cache.name ).to( new RedisConnectorProvider( cache.name ) )
    )
  ).toSeq

  private def defaultBindings( name: String ): Seq[ Binding[ _ ] ] = manager.flatMap( cache =>
    Seq(
      bind[ RedisCommands ].to( bind[ RedisCommands ].qualifiedWith( name ) ),
      bind[ RedisConnector ].to( bind[ RedisConnector ].qualifiedWith( name ) )
    )
  ).toSeq

  private[ redis ] def connectorBindings = Seq(
    // binds akka serializer to its implementation
    bind[ AkkaSerializer ].to[ AkkaSerializerImpl ]
  ) ++ bindings ++ defaultBindings( "play" )
}

/**
  * Components for compile-time dependency injection.
  * It binds components for connector package
  *
  * @author Karel Cemus
  */
private[ redis ] trait RedisConnectorComponents {
  import akka.actor.ActorSystem

  def actorSystem: ActorSystem
  def applicationLifecycle: ApplicationLifecycle

  private lazy val akkaSerializer: AkkaSerializer = new AkkaSerializerImpl( actorSystem )

  private[ redis ] def redisCommandsFor( instance: RedisInstance ): RedisCommands = instance match {
    case standalone: RedisStandalone => new RedisCommandsStandalone( standalone )( actorSystem, applicationLifecycle ).get
    case cluster: RedisCluster => new RedisCommandsCluster( cluster )( actorSystem, applicationLifecycle ).get
  }

  private[ redis ] def redisConnectorFor( instance: RedisInstance ) =
    new RedisConnectorImpl( akkaSerializer, instance, redisCommandsFor( instance ) )( actorSystem )
}
