package play.api.cache.redis

import javax.inject.{Inject, Provider, Singleton}

import scala.language.implicitConversions

import play.api.inject._
import play.api.{Configuration, Environment}

import redis.RedisCommands

package configuration {

  /**
    * Extracts the configuration and binds with DI for upper layers.
    */
  private[ redis ] trait RedisConfigurationModule {

    def configuration: Configuration

    private lazy val manager = configuration.get( "play.cache.redis" )( RedisInstanceManager )

    def caches = manager.caches

    private def instanceOf( name: String ): RedisInstanceBinder = manager.instanceOf( name )

    def bindNamedConfiguration( name: String ): Seq[ Binding[ _ ] ] = instanceOf( name ).toBinding
  }
}

package connector {

  /**
    * Configures low-level classes communicating with the redis server.
    */
  private[ redis ] trait RedisConnectorModule {

    def bindSharedConnector: Seq[ Binding[ _ ] ] = Seq(
      bind[ AkkaSerializer ].to[ AkkaSerializerImpl ]
    )

    def bindNamedConnector( name: String ): Seq[ Binding[ _ ] ] = Seq(
      bind[ RedisCommands ].qualifiedWith( name ).to( new RedisCommandsProvider( name ) ),
      bind[ RedisConnector ].qualifiedWith( name ).to( new RedisConnectorProvider( name ) )
    )

    def bindDefaultConnector( name: String ): Seq[ Binding[ _ ] ] = Seq(
      bind[ RedisCommands ].to( bind[ RedisCommands ].qualifiedWith( name ) ),
      bind[ RedisConnector ].to( bind[ RedisConnector ].qualifiedWith( name ) )
    )
  }
}

package impl {

  /**
    * Configures low-level classes communicating with the redis server.
    */
  private[ redis ] trait ImplementationModule {
    import ImplementationModule._
    import NamedCacheProvider._

    def bindNamedCache( implicit name: String ) = Seq[ Binding[ _ ] ](
      // play-redis APIs
      bind[ CacheApi ].qualified.to( NamedSyncRedisProvider( name ) ),
      bind[ CacheAsyncApi ].qualified.to( NamedAsyncRedisProvider( name ) ),
      // scala api defined by Play
      bind[ play.api.cache.CacheApi ].qualified.to( NamedScalaSyncCacheProvider( name ) ),
      bind[ play.api.cache.SyncCacheApi ].qualified.to( NamedScalaSyncCacheProvider( name ) ),
      bind[ play.api.cache.AsyncCacheApi ].qualified.to( NamedAsyncRedisProvider( name ) ),
      // java api defined by Play
      bind[ play.cache.CacheApi ].qualified.to( NamedJavaSyncCacheProvider( name ) ),
      bind[ play.cache.SyncCacheApi ].qualified.to( NamedJavaSyncCacheProvider( name ) ),
      bind[ play.cache.AsyncCacheApi ].qualified.to( NamedJavaRedisProvider( name ) )
    )

    def bindDefaultCache( implicit name: String ): Seq[ Binding[ _ ] ] = Seq(
      // play-redis APIs
      bind[ CacheApi ].to( bind[ CacheApi ].qualified ),
      bind[ CacheAsyncApi ].to( bind[ CacheAsyncApi ].qualified ),
      // scala api defined by Play
      bind[ play.api.cache.CacheApi ].to( bind[ play.api.cache.CacheApi ].qualified ),
      bind[ play.api.cache.SyncCacheApi ].to( bind[ play.api.cache.SyncCacheApi ].qualified ),
      bind[ play.api.cache.AsyncCacheApi ].to( bind[ play.api.cache.AsyncCacheApi ].qualified ),
      // java api defined by Play
      bind[ play.cache.CacheApi ].to( bind[ play.cache.CacheApi ].qualified ),
      bind[ play.cache.SyncCacheApi ].to( bind[ play.cache.SyncCacheApi ].qualified ),
      bind[ play.cache.AsyncCacheApi ].to( bind[ play.cache.AsyncCacheApi ].qualified )
    )
  }

  object ImplementationModule {
    implicit class QualifiedBinding[ T ]( val binding: BindingKey[ T ] ) extends AnyVal {
      def qualified( implicit name: String ) = binding.qualifiedWith( name )
    }
  }

  class NamedCacheProvider[ T ]( name: String )( f: Injector => T ) extends Provider[ T ] {
    @Inject var injector: Injector = _
    lazy val get = f( injector )
  }

  object NamedCacheProvider {

    @inline private implicit def implicitBinding[ T ]( bindingKey: BindingKey[ T ] )( implicit injector: Injector ): T = injector.instanceOf( bindingKey )

    @inline private implicit def implicitInjection[ T ]( implicit bindingKey: BindingKey[ T ], injector: Injector ): T = implicitBinding( bindingKey )

    def NamedAsyncRedisProvider( name: String ) = provider( name ) { implicit injector =>
      def connector = bind[ RedisConnector ].qualifiedWith( name )
      implicit def runtime = bind[ RedisRuntime ].qualifiedWith( name )

      new AsyncRedis( connector )
    }

    def NamedSyncRedisProvider( name: String ) = provider( name ) { implicit injector =>
      def connector = bind[ RedisConnector ].qualifiedWith( name )
      implicit def runtime = bind[ RedisRuntime ].qualifiedWith( name )

      new SyncRedis( connector )
    }

    def NamedJavaRedisProvider( name: String ) = provider( name ) { implicit injector =>
      def internal = bind[ CacheAsyncApi ].qualifiedWith( name )
      def environment = bind[ Environment ]
      implicit def runtime = bind[ RedisRuntime ].qualifiedWith( name )

      new JavaRedis( internal, environment )
    }

    def NamedJavaSyncCacheProvider( name: String ) = provider( name ) { implicit injector =>
      def internal = bind[ play.cache.AsyncCacheApi ].qualifiedWith( name )

      new play.cache.DefaultSyncCacheApi( internal )
    }

    def NamedScalaSyncCacheProvider( name: String ) = provider( name ) { implicit injector =>
      def internal = bind[ play.api.cache.AsyncCacheApi ].qualifiedWith( name )

      new play.api.cache.DefaultSyncCacheApi( internal )
    }

    @inline private def provider[ T ]( name: String )( f: Injector => T ): NamedCacheProvider[ T ] = new NamedCacheProvider( name )( f )
  }
}


private[ redis ] object RecoveryPolicyBinder {

  def bindings = Seq(
    bind[ RecoveryPolicy ].qualifiedWith( "log-and-fail" ).to[ LogAndFailPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-and-default" ).to[ LogAndDefaultPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-condensed-and-fail" ).to[ LogCondensedAndFailPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-condensed-and-default" ).to[ LogCondensedAndDefaultPolicy ]
  )
}

/** Play framework module implementing play.api.cache.CacheApi for redis-server key/value storage. For more details
  * see README.
  *
  * @author Karel Cemus
  */
@Singleton
class RedisCacheModule extends Module {

  override def bindings( environment: Environment, config: play.api.Configuration ) = {
    // play-redis consists of several layers and sub-modules, each defining it's own bindings
    val module = new connector.RedisConnectorModule with configuration.RedisConfigurationModule with impl.ImplementationModule {
      val configuration = config
      def defaultCache = config.get[ String ]( "play.cache.redis.default-cache" )
      def bindDefault = config.get[ Boolean ]( "play.cache.redis.bind-default" )
    }

    def defaultCache = config.get[ String ]( "play.cache.redis.default-cache" )
    def bindDefault = config.get[ Boolean ]( "play.cache.redis.bind-default" )
    def caches = module.caches

    def bindShared = module.bindSharedConnector ++ RecoveryPolicyBinder.bindings

    def bindNamed( name: String ) = module.bindNamedConfiguration( name ) ++ module.bindNamedConnector( name ) ++ module.bindNamedCache( name ) ++ Seq(
      bind[ RedisRuntime ].qualifiedWith( name ).to( new NamedRedisRuntimeProvider( name ) )
    )
    def bindCaches = caches.flatMap( bindNamed )

    def bindDefaults( name: String ) = module.bindDefaultConnector( name ) ++ module.bindDefaultCache( name ) ++ Seq(
      bind[ RedisRuntime ].to( new NamedRedisRuntimeProvider( name ) )
    )
    def bindDefaultCache = if ( bindDefault ) bindDefaults( defaultCache ) else Seq.empty[ Binding[ _ ] ]

    bindShared ++ bindDefaultCache ++ bindCaches
  }
}
