package play.api.cache.redis

import akka.actor.{ActorSystem, CoordinatedShutdown}

trait Shutdown extends RedisCacheComponents {

  def shutdown() = Shutdown.run
}

object Shutdown {

  def run(implicit system: ActorSystem) = CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)
}
