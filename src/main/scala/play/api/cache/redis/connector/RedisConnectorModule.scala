package play.api.cache.redis.connector

import play.api.cache.redis.RedisConnector
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

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
