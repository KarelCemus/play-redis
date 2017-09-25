package play.api.cache.redis

import javax.inject.{Inject, Provider, Singleton}

import scala.language.implicitConversions
import scala.reflect.ClassTag

import play.api.Environment
import play.api.inject._

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

trait GuiceProviderImplicits {
  def injector: Injector
  protected implicit def implicitInjection[ X ]( key: BindingKey[ X ] ): X = injector instanceOf key
}

object GuiceProvider {

  private def provider[ T ]( f: impl.RedisCaches => T )( implicit name: CacheName ): Provider[ T ] = new NamedCacheInstanceProvider( f )

  def bindings( instance: RedisInstanceProvider ) = {
    implicit val name = new CacheName( instance.name )

    Seq(
      // bind implementation of all caches
      bind[ impl.RedisCaches ].qualifiedWith( name ).to( new GuiceRedisCacheProvider( instance ) ),
      // expose a single-implementation providers
      bind[ CacheApi ].qualifiedWith( name ).to( provider( _.sync ) ),
      bind[ CacheAsyncApi ].qualifiedWith( name ).to( provider( _.async ) ),
      bind[ play.api.cache.SyncCacheApi ].qualifiedWith( name ).to( provider( _.scalaSync ) ),
      bind[ play.cache.SyncCacheApi ].qualifiedWith( name ).to( provider( _.javaSync ) ),
      bind[ play.cache.AsyncCacheApi ].qualifiedWith( name ).to( provider( _.javaAsync ) ),
    )
  }

  def defaults( instance: RedisInstanceProvider ) = {
    implicit val name = new CacheName( instance.name )
    def namedBinding[ T: ClassTag ]( implicit cacheName: CacheName ): Binding[ T ] = bind[ T ].to( bind[ T ].qualifiedWith( name ) )

    Seq(
      // bind implementation of all caches
      namedBinding[ impl.RedisCaches ],
      // expose a single-implementation providers
      namedBinding[ CacheApi ],
      namedBinding[ CacheAsyncApi ],
      namedBinding[ play.api.cache.SyncCacheApi ],
      namedBinding[ play.cache.SyncCacheApi ],
      namedBinding[ play.cache.AsyncCacheApi ]
    )
  }
}

class GuiceRedisCacheProvider( instance: RedisInstanceProvider ) extends Provider[ RedisCaches ] with GuiceProviderImplicits {
  @Inject() var injector: Injector = _
  lazy val get = new impl.RedisCachesProvider(
    instance = instance.resolved( bind[ configuration.RedisInstanceResolver ] ),
    serializer = bind[ connector.AkkaSerializer ],
    environment = bind[ Environment ],
    recovery = bind[ RecoveryPolicyResolver ]
  )(
    system = bind[ akka.actor.ActorSystem ],
    lifecycle = bind[ ApplicationLifecycle ]
  ).get
}

class NamedCacheInstanceProvider[ T ]( f: RedisCaches => T )( implicit name: CacheName ) extends Provider[ T ] with GuiceProviderImplicits {
  @Inject() var injector: Injector = _
  lazy val get = f( bind[ RedisCaches ].qualifiedWith( name ) )
}

class CacheName( val name: String ) extends AnyVal
object CacheName {
  implicit def name2string( name: CacheName ): String = name.name
}

@Singleton
class GuiceRedisInstanceResolver @Inject()( val injector: Injector ) extends configuration.RedisInstanceResolver with GuiceProviderImplicits {
  def resolve = {
    case name => bind[ RedisInstance ].qualifiedWith( name )
  }
}
