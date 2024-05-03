package play.api.cache.redis.test

import com.typesafe.config.Config
import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import play.api.inject._
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.libs.concurrent.{ActorSystemProvider, CoordinatedShutdownProvider}
import play.api.{Configuration, Environment}

import javax.inject.Singleton

trait DefaultInjector {

  def env: Environment = Environment.simple()

  def newInjector: GuiceInjectorBuilder =
    new GuiceInjectorBuilder()
      .bindings(
        bind[Environment] to env,
        bind[play.Environment].toProvider[EnvironmentProvider].in(classOf[Singleton]),
        bind[ConfigurationProvider].to(new ConfigurationProvider(Configuration.load(env))),
        bind[Configuration].toProvider[ConfigurationProvider],
        bind[Config].toProvider[ConfigProvider],
        bind[ApplicationLifecycle].to(bind[DefaultApplicationLifecycle]),
        bind[ActorSystem].toProvider[ActorSystemProvider],
        bind[CoordinatedShutdown].toProvider[CoordinatedShutdownProvider],
      )

}
