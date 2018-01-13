package play.api.cache.redis

import javax.inject._

import scala.language.implicitConversions
import scala.reflect.ClassTag

import play.api.{Environment, Logger}
import play.api.inject._
import play.cache._

/** Play framework module implementing play.api.cache.CacheApi for redis-server key/value storage. For more details
  * see README.
  *
  * @author Karel Cemus
  */
@Singleton
class RedisCacheModule extends Module {

  override def bindings( environment: Environment, config: play.api.Configuration ) = {
    def bindDefault = config.get[ Boolean ]( "play.cache.redis.bind-default" )

    // read the config and get the configuration of the redis
    val manager = config.get( "play.cache.redis" )( configuration.RedisInstanceManager )

    // bind all caches
    val caches = manager.flatMap( GuiceProvider.bindings )
    // common settings
    val commons = Seq(
      // bind serializer
      bind[ connector.AkkaSerializer ].toProvider[ connector.AkkaSerializerProvider ],
      bind[ configuration.RedisInstanceResolver ].to[ GuiceRedisInstanceResolver ]
    )
    // bind recovery resolver
    val recovery = RecoveryPolicyResolver.bindings
    // default bindings
    val defaults = if ( bindDefault ) GuiceProvider.defaults( manager.defaultInstance ) else Seq.empty

    // return all bindings
    commons ++ caches ++ recovery ++ defaults
  }
}

trait ProviderImplicits {

  // Creates a named cache qualifier
  private def named( name: String ): NamedCache = {
    new NamedCacheImpl( name )
  }

  protected def bindNamed[ T: ClassTag ]( name: String ): BindingKey[ T ] =
    bind[ T ].qualifiedWith( named( name ) )
}

trait GuiceProviderImplicits extends ProviderImplicits {
  def injector: Injector
  protected implicit def implicitInjection[ X ]( key: BindingKey[ X ] ): X = injector instanceOf key
}

object GuiceProvider extends ProviderImplicits {

  @inline private def provider[ T ]( f: impl.RedisCaches => T )( implicit name: CacheName ): Provider[ T ] = new NamedCacheInstanceProvider( f )

  @inline private def deprecatedProvider[ T ]( f: impl.RedisCaches => T )( implicit name: CacheName ): Provider[ T ] = new DeprecatedNamedCacheInstanceProvider( f )

  @inline private def namedBinding[ T: ClassTag ]( f: impl.RedisCaches => T )( implicit name: CacheName ): Seq[ Binding[ T ] ] = Seq(
    bindNamed[ T ]( name ).to( provider( f ) ),
    bind[ T ].qualifiedWith( name ).to( deprecatedProvider( f ) )
  )

  def bindings( instance: RedisInstanceProvider ) = {
    implicit val name = new CacheName( instance.name )

    Seq[ Binding[ _ ] ](
      // bind implementation of all caches
      bindNamed[ impl.RedisCaches ]( name ).to( new GuiceRedisCacheProvider( instance ) )
    ) ++ Seq(
      // expose a single-implementation providers
      namedBinding( _.sync ),
      namedBinding( _.async ),
      namedBinding( _.scalaAsync ),
      namedBinding( _.scalaSync ),
      namedBinding( _.javaSync ),
      namedBinding( _.javaAsync )
    ).flatten
  }

  def defaults( instance: RedisInstanceProvider ) = {
    implicit val name = new CacheName( instance.name )
    @inline def defaultBinding[ T: ClassTag ]( implicit cacheName: CacheName ): Binding[ T ] = bind[ T ].to( bindNamed[ T ]( name ) )

    Seq(
      // bind implementation of all caches
      defaultBinding[ impl.RedisCaches ],
      // expose a single-implementation providers
      defaultBinding[ CacheApi ],
      defaultBinding[ CacheAsyncApi ],
      defaultBinding[ play.api.cache.SyncCacheApi ],
      defaultBinding[ play.api.cache.AsyncCacheApi ],
      defaultBinding[ play.cache.SyncCacheApi ],
      defaultBinding[ play.cache.AsyncCacheApi ]
    )
  }
}

class GuiceRedisCacheProvider( instance: RedisInstanceProvider ) extends Provider[ RedisCaches ] with GuiceProviderImplicits {
  @Inject() var injector: Injector = _
  lazy val get = new impl.RedisCachesProvider(
    instance = instance.resolved( bind[ configuration.RedisInstanceResolver ] ),
    serializer = bind[ connector.AkkaSerializer ],
    environment = bind[ Environment ]
  )(
    system = bind[ akka.actor.ActorSystem ],
    lifecycle = bind[ ApplicationLifecycle ],
    recovery = bind[ RecoveryPolicyResolver ]
  ).get
}

class NamedCacheInstanceProvider[ T ]( f: RedisCaches => T )( implicit name: CacheName ) extends Provider[ T ] with GuiceProviderImplicits {
  @Inject() var injector: Injector = _
  lazy val get = f( bindNamed[ RedisCaches ]( name ) )
}

@deprecated( "Use @NamedCache instead of @Named to inject named caches.", since = "2.1.0" )
class DeprecatedNamedCacheInstanceProvider[ T ]( f: RedisCaches => T )( implicit name: CacheName ) extends Provider[ T ] with GuiceProviderImplicits {
  @Inject() var injector: Injector = _
  lazy val get = {
    Logger( "play.api.cache.redis.deprecation" ).warn( "Named caches annotated with @Named are deprecated and will be removed in the next release. Use @NamedCache instead. See changelog for more details." )
    f( bindNamed[ RedisCaches ]( name ) )
  }
}

class CacheName( val name: String ) extends AnyVal
object CacheName {
  implicit def name2string( name: CacheName ): String = name.name
}

@Singleton
class GuiceRedisInstanceResolver @Inject()( val injector: Injector ) extends configuration.RedisInstanceResolver with GuiceProviderImplicits {
  def resolve = {
    case name => bindNamed[ RedisInstance ]( name )
  }
}
