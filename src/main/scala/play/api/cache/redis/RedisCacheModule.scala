package play.api.cache.redis

import javax.inject._

import scala.language.implicitConversions
import scala.reflect.ClassTag

import play.api.inject._
import play.cache._

/**
  * Play framework module implementing play.api.cache.CacheApi for redis-server key/value storage. For more details
  * see README.
  */
@Singleton
class RedisCacheModule extends Module {

  override def bindings(environment: play.api.Environment, config: play.api.Configuration) = {
    def bindDefault = config.get[Boolean]("play.cache.redis.bind-default")

    // read the config and get the configuration of the redis
    val manager = config.get("play.cache.redis")(configuration.RedisInstanceManager)

    // bind all caches
    val caches = manager.flatMap(GuiceProvider.bindings)
    // common settings
    val commons = Seq(
      // bind serializer
      bind[connector.AkkaSerializer].toProvider[connector.AkkaSerializerProvider],
      bind[configuration.RedisInstanceResolver].to[GuiceRedisInstanceResolver]
    )
    // bind recovery resolver
    val recovery = RecoveryPolicyResolver.bindings
    // default bindings
    val defaults = if (bindDefault) GuiceProvider.defaults(manager.defaultInstance) else Seq.empty

    // return all bindings
    commons ++ caches ++ recovery ++ defaults
  }
}

trait ProviderImplicits {
  implicit def key2rich[T](key: BindingKey[T]): RichBindingKey[T] = new RichBindingKey[T](key)
}

private[redis] class RichBindingKey[T](val key: BindingKey[T]) extends AnyVal {
  @inline def named(name: String) = key.qualifiedWith(new NamedCacheImpl(name))
}

trait GuiceProviderImplicits extends ProviderImplicits {
  def injector: Injector
  protected implicit def implicitInjection[X](key: BindingKey[X]): X = injector instanceOf key
}

object GuiceProvider extends ProviderImplicits {

  private class QualifiedBindingKey[T](key: BindingKey[T], f: impl.RedisCaches => T) {
    @inline private def provider(f: impl.RedisCaches => T)(implicit name: CacheName): Provider[T] = new NamedCacheInstanceProvider(f)
    def toBindings(implicit name: CacheName): Binding[_] = key.named(name).to(provider(f))
  }

  private def namedBinding[T: ClassTag](f: impl.RedisCaches => T) = new QualifiedBindingKey(bind[T], f)

  def bindings(instance: RedisInstanceProvider) = {
    implicit val name = new CacheName(instance.name)

    Seq[Binding[_]](
      // bind implementation of all caches
      bind[impl.RedisCaches].named(name).to(new GuiceRedisCacheProvider(instance))
    ) ++ Seq[QualifiedBindingKey[_]](
        // expose a single-implementation providers
        namedBinding(_.sync),
        namedBinding(_.async),
        namedBinding(_.scalaAsync),
        namedBinding(_.scalaSync),
        namedBinding(_.javaSync),
        namedBinding[play.cache.AsyncCacheApi](_.javaAsync),
        namedBinding[play.cache.redis.AsyncCacheApi](_.javaAsync)
      ).map(_.toBindings)
  }

  def defaults(instance: RedisInstanceProvider) = {
    implicit val name = new CacheName(instance.name)
    @inline def defaultBinding[T: ClassTag](implicit cacheName: CacheName): Binding[T] = bind[T].to(bind[T].named(name))

    Seq(
      // bind implementation of all caches
      defaultBinding[impl.RedisCaches],
      // expose a single-implementation providers
      defaultBinding[CacheApi],
      defaultBinding[CacheAsyncApi],
      defaultBinding[play.cache.redis.AsyncCacheApi],
      // scala default api
      defaultBinding[play.api.cache.SyncCacheApi],
      defaultBinding[play.api.cache.AsyncCacheApi],
      // java default api
      defaultBinding[play.cache.SyncCacheApi],
      defaultBinding[play.cache.AsyncCacheApi]
    )
  }
}

class GuiceRedisCacheProvider(instance: RedisInstanceProvider) extends Provider[RedisCaches] with GuiceProviderImplicits {
  @Inject() var injector: Injector = _
  lazy val get: RedisCaches = new impl.RedisCachesProvider(
    instance = instance.resolved(bind[configuration.RedisInstanceResolver]),
    serializer = bind[connector.AkkaSerializer],
    environment = bind[play.api.Environment]
  )(
    system = bind[akka.actor.ActorSystem],
    lifecycle = bind[ApplicationLifecycle],
    recovery = bind[RecoveryPolicyResolver]
  ).get
}

class NamedCacheInstanceProvider[T](f: RedisCaches => T)(implicit name: CacheName) extends Provider[T] with GuiceProviderImplicits {
  @Inject() var injector: Injector = _
  lazy val get = f(bind[RedisCaches].named(name))
}

class CacheName(val name: String) extends AnyVal
object CacheName {
  implicit def name2string(name: CacheName): String = name.name
}

@Singleton
class GuiceRedisInstanceResolver @Inject() (val injector: Injector) extends configuration.RedisInstanceResolver with GuiceProviderImplicits {
  def resolve = {
    case name => bind[RedisInstance].named(name)
  }
}
