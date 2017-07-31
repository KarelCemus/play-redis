package play.api.cache.redis.impl

import javax.inject._

import scala.language.implicitConversions

import play.api.Environment
import play.api.cache.redis._
import play.api.inject._

private[ impl ] object RedisRecoveryPolicyResolver {

  def bindings = Seq(
    bind[ RecoveryPolicy ].qualifiedWith( "log-and-fail" ).to[ LogAndFailPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-and-default" ).to[ LogAndDefaultPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-condensed-and-fail" ).to[ LogCondensedAndFailPolicy ],
    bind[ RecoveryPolicy ].qualifiedWith( "log-condensed-and-default" ).to[ LogCondensedAndDefaultPolicy ]
  )
}

/**
  * Configures low-level classes communicating with the redis server.
  *
  * @author Karel Cemus
  */
trait ImplementationModule {
  import ImplementationModule._

  def manager: RedisInstanceManager

  private def bindCache( implicit name: String ) = Seq[ Binding[ _ ] ](
    // play-redis APIs
    bind[ CacheApi ].qualified.to( new NamedSyncRedisProvider( name ) ),
    bind[ CacheAsyncApi ].qualified.to( new NamedAsyncRedisProvider( name ) ),
    // scala api defined by Play
    bind[ play.api.cache.CacheApi ].qualified.to( new NamedScalaSyncCacheProvider( name ) ),
    bind[ play.api.cache.SyncCacheApi ].qualified.to( new NamedScalaSyncCacheProvider( name ) ),
    bind[ play.api.cache.AsyncCacheApi ].qualified.to( new NamedAsyncRedisProvider( name ) ),
    // java api defined by Play
    bind[ play.cache.CacheApi ].qualified.to( new NamedJavaSyncCacheProvider( name ) ),
    bind[ play.cache.SyncCacheApi ].qualified.to( new NamedJavaSyncCacheProvider( name ) ),
    bind[ play.cache.AsyncCacheApi ].qualified.to( new NamedJavaRedisProvider( name ) ),
  )

  private def bindDefault( implicit name: String ) = Seq[ Binding[ _ ] ](
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
    bind[ play.cache.AsyncCacheApi ].to( bind[ play.cache.AsyncCacheApi ].qualified ),
  )

  def implementationBindings = {
    manager.flatMap {
      cache => bindCache( cache.name )
    }.toSeq ++ bindDefault( "play" ) ++ RedisRecoveryPolicyResolver.bindings
  }
}

object ImplementationModule {
  implicit class QualifiedBinding[ T ]( val binding: BindingKey[ T ] ) extends AnyVal {
    def qualified( implicit name: String ) = binding.qualifiedWith( name )
  }
}

abstract class NamedCacheProvider[ T ]( name: String )( f: Injector => T ) extends Provider[ T ] {
  @Inject var injector: Injector = _
  def get = f( injector )
}

class NamedAsyncRedisProvider( name: String ) extends NamedCacheProvider( name )( { injector: Injector =>
  def connector = bind[ RedisConnector ].qualifiedWith( name )
  def instance = injector instanceOf bind[ RedisInstance ].qualifiedWith( name )
  def policy = bind[ RecoveryPolicy ].qualifiedWith( instance.recovery )

  new AsyncRedis( name, injector instanceOf connector, injector instanceOf policy )
} )

class NamedSyncRedisProvider( name: String ) extends NamedCacheProvider( name )( { injector: Injector =>
  def connector = bind[ RedisConnector ].qualifiedWith( name )
  def instance = injector instanceOf bind[ RedisInstance ].qualifiedWith( name )
  def policy = bind[ RecoveryPolicy ].qualifiedWith( instance.recovery )

  new SyncRedis( name, injector instanceOf connector, injector instanceOf policy )
} )

class NamedJavaRedisProvider( name: String ) extends NamedCacheProvider( name )( { injector =>
  def connector = injector instanceOf bind[ RedisConnector ].qualifiedWith( name )
  def internal = injector instanceOf bind[ CacheAsyncApi ].qualifiedWith( name )
  def environment = injector instanceOf bind[ Environment ]

  new JavaRedis( name, internal, environment, connector )
} )

class NamedJavaSyncCacheProvider( name: String ) extends NamedCacheProvider( name )( { injector =>
  def internal = injector instanceOf bind[ play.cache.AsyncCacheApi ].qualifiedWith( name )

  new play.cache.DefaultSyncCacheApi( internal )
} )

class NamedScalaSyncCacheProvider( name: String ) extends NamedCacheProvider( name )( { injector =>
  def internal = injector instanceOf bind[ play.api.cache.AsyncCacheApi ].qualifiedWith( name )

  new play.api.cache.DefaultSyncCacheApi( internal )
} )

/**
  * Components for compile-time dependency injection.
  * It binds components from impl package
  *
  * @author Karel Cemus
  */
private[ redis ] trait ImplementationComponents {

  def environment: Environment

  /** overwrite to provide custom recovery policy */
  def recoveryPolicy: PartialFunction[ String, RecoveryPolicy ] = {
    case "log-and-fail" => new LogAndFailPolicy
    case "log-and-default" => new LogAndDefaultPolicy
    case "log-condensed-and-fail" => new LogCondensedAndFailPolicy
    case "log-condensed-and-default" => new LogCondensedAndDefaultPolicy
  }

  private[ redis ] def redisConnectorFor( instance: RedisInstance ): RedisConnector

  private implicit def instance2connector( instance: RedisInstance ): RedisConnector = redisConnectorFor( instance )
  private implicit def instance2policy( instance: RedisInstance ): RecoveryPolicy = recoveryPolicy( instance.recovery )

  // play-redis APIs
  private def asyncRedis( instance: RedisInstance ) = new AsyncRedis( instance.name, redis = instance, policy = instance )

  def syncRedisCacheApi( instance: RedisInstance ): CacheApi = new SyncRedis( instance.name, redis = instance, policy = instance )
  def asyncRedisCacheApi( instance: RedisInstance ): CacheAsyncApi = asyncRedis( instance )

  // scala api defined by Play
  def asyncCacheApi( instance: RedisInstance ): play.api.cache.AsyncCacheApi = asyncRedis( instance )
  private def defaultSyncCache( instance: RedisInstance ) = new play.api.cache.DefaultSyncCacheApi( asyncCacheApi( instance ) )
  @deprecated( message = "Use syncCacheApi or asyncCacheApi.", since = "Play 2.6.0." )
  def defaultCacheApi( instance: RedisInstance ): play.api.cache.CacheApi = defaultSyncCache( instance )
  def syncCacheApi( instance: RedisInstance ): play.api.cache.SyncCacheApi = defaultSyncCache( instance )

  // java api defined by Play
  def javaAsyncCacheApi( instance: RedisInstance ): play.cache.AsyncCacheApi = new JavaRedis( instance.name, asyncRedis( instance ), environment = environment, connector = instance )
  private def javaDefaultSyncCache( instance: RedisInstance ) = new play.cache.DefaultSyncCacheApi( javaAsyncCacheApi( instance ) )
  @deprecated( message = "Use javaSyncCacheApi or javaAsyncCacheApi.", since = "Play 2.6.0." )
  def javaCacheApi( instance: RedisInstance ): play.cache.CacheApi = javaDefaultSyncCache( instance )
  def javaSyncCacheApi( instance: RedisInstance ): play.cache.SyncCacheApi = javaDefaultSyncCache( instance )
}
