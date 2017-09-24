package play.api.cache.redis.configuration

import scala.collection.JavaConverters._

import play.api.inject._

import com.typesafe.config.{Config, ConfigOrigin}

/**
  * @author Karel Cemus
  */
private[ configuration ] trait RedisInstanceBinder {
  def name: String
  protected def self = asBindingKey
  def toBinding: List[ Binding[ RedisInstance ] ]
  def asBindingKey: BindingKey[ RedisInstance ] = bind[ RedisInstance ].qualifiedWith( name )
  def toDefaultBinding = List( bind[ RedisInstance ].to( self ) )
  def instanceOption: Option[ RedisInstance ]
}

private[ configuration ] class RedisInstanceSelfBinder( val instance: RedisInstance ) extends RedisInstanceBinder {
  val name = instance.name
  def instanceOption = Some( instance )
  def toBinding = List( self toInstance instance )
}

private[ configuration ] class RedisInstanceCustomBinder( val name: String ) extends RedisInstanceBinder {
  def toBinding = List.empty
  def instanceOption = None
}

private[ configuration ] object RedisInstanceBinder extends RedisConfigInstanceLoader[ RedisInstanceBinder ] {
  import RedisConfigLoader._

  def load( config: Config, path: String, name: String )( implicit defaults: RedisSettings ) = {
    config.getOption( path / "source", _.getString ) getOrElse defaults.source match {
      // required static configuration of the standalone instance using application.conf
      case "standalone" => RedisInstanceStandalone
      // required static configuration of the cluster using application.conf
      case "cluster" => RedisInstanceCluster
      // required possibly environmental configuration of the standalone instance
      case "connection-string" => RedisInstanceEnvironmental
      // supplied custom configuration
      case "custom" => RedisInstanceCustom
      // found but unrecognized
      case other => invalidConfiguration( config.getConfig( path / "source" ).origin(), other )
    }
  }.load( config, path, name )

  /** helper indicating invalid configuration */
  @throws[ IllegalStateException ]
  private def invalidConfiguration( source: ConfigOrigin, value: String ): Nothing = throw new IllegalStateException(
    s"""
       |Unrecognized configuration provider '$value' in ${ source.filename() } at ${ source.lineNumber() }.
       |Expected values are 'standalone', 'cluster', 'connection-string', and 'custom'.
    """.stripMargin
  )
}

/**
  * Statically configured single standalone redis instance
  */
private[ configuration ] object RedisInstanceStandalone extends RedisConfigInstanceLoader[ RedisInstanceBinder ] {
  def load( config: Config, path: String, instanceName: String )( implicit defaults: RedisSettings ) = new RedisInstanceSelfBinder (
    RedisStandalone.apply(
      name = instanceName,
      host = RedisHost.load( config, path ),
      settings = RedisSettings.withFallback( defaults ).load( config, path )
    )
  )
}

/**
  * Statically configured redis cluster
  */
private[ configuration ] object RedisInstanceCluster extends RedisConfigInstanceLoader[ RedisInstanceBinder ] {
  import RedisConfigLoader._

  def load( config: Config, path: String, instanceName: String )( implicit defaults: RedisSettings ) = new RedisInstanceSelfBinder(
    RedisCluster.apply(
      name = instanceName,
      nodes = config.getConfigList( path / "cluster" ).asScala.map( config => RedisHost.load( config ) ).toList,
      settings = RedisSettings.withFallback( defaults ).load( config, path )
    )
  )
}

/**
  * Reads a configuration from the connection string, possibly from an environmental variable.
  * This instance configuration is designed to work in PaaS environments such as Heroku.
  */
private[ configuration ] object RedisInstanceEnvironmental extends RedisConfigInstanceLoader[ RedisInstanceBinder ] {
  import RedisConfigLoader._

  def load( config: Config, path: String, instanceName: String )( implicit defaults: RedisSettings ) = new RedisInstanceSelfBinder(
    RedisStandalone.apply(
      name = instanceName,
      host = RedisHost.fromConnectionString( config getString path./( "connection-string" ) ),
      settings = RedisSettings.withFallback( defaults ).load( config, path )
    )
  )
}

/**
  * This binder indicates that the user provides his own configuration of this named cache.
  */
private[ configuration ] object RedisInstanceCustom extends RedisConfigInstanceLoader[ RedisInstanceBinder ] {
  def load( config: Config, path: String, instanceName: String )( implicit defaults: RedisSettings ) = new RedisInstanceCustomBinder(
    name = instanceName
  )
}
