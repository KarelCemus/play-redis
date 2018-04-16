package play.api.cache.redis.configuration

/**
  * Abstraction over clusters and standalone instances. This trait
  * encapsulates a common settings and simplifies pattern matching.
  *
  * @author Karel Cemus
  */
sealed trait RedisInstance extends RedisSettings {
  /** name of the redis instance */
  def name: String
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals( obj: scala.Any ) = equalsAsInstance( obj )
  /** trait-specific equals, invokable from children */
  protected def equalsAsInstance( obj: scala.Any ) = obj match {
    case that: RedisInstance => this.name == that.name && equalsAsSettings( that )
    case _ => false
  }
  // $COVERAGE-ON$
}

/**
  * Type of Redis Instance - a cluster. It encapsulates common settings of the instance
  * and the list of cluster nodes.
  *
  * @author Karel Cemus
  */
trait RedisCluster extends RedisInstance {
  /** nodes definition when cluster is defined */
  def nodes: List[ RedisHost ]
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals( obj: scala.Any ) = obj match {
    case that: RedisCluster => equalsAsInstance( that ) && this.nodes == that.nodes
    case _ => false
  }
  /** to string */
  override def toString = s"Cluster[${ nodes mkString "," }]"
  // $COVERAGE-ON$
}

object RedisCluster {

  def apply( name: String, nodes: List[ RedisHost ], settings: RedisSettings ) =
    create( name, nodes, settings )

  @inline
  private def create( _name: String, _nodes: List[ RedisHost ], _settings: RedisSettings ) =
    new RedisCluster with RedisDelegatingSettings {
      val name = _name
      val nodes = _nodes
      val settings = _settings
    }
}

/**
  * A type of Redis Instance - a standalone instance. It encapsulates
  * common settings of the instance and provides a connection settings.
  *
  * @author Karel Cemus
  */
trait RedisStandalone extends RedisInstance with RedisHost {
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals( obj: scala.Any ) = obj match {
    case that: RedisStandalone => equalsAsInstance( that ) && equalsAsHost( that )
    case _ => false
  }
  /** to string */
  override def toString = database match {
    case Some( database ) => s"Standalone($name@$host:$port?db=$database)"
    case None => s"Standalone($name@$host:$port)"
  }
  // $COVERAGE-ON$
}

object RedisStandalone {

  def apply( name: String, host: RedisHost, settings: RedisSettings ) =
    create( name, host, settings )

  @inline
  private def create( _name: String, _host: RedisHost, _settings: RedisSettings ) =
    new RedisStandalone with RedisDelegatingSettings with RedisDelegatingHost {
      val name = _name
      val innerHost = _host
      val settings = _settings
    }
}
