package play.api.cache.redis.configuration

/**
  * Abstraction over clusters and standalone instances. This trait
  * encapsulates a common settings and simplifies pattern matching.
  */
sealed trait RedisInstance extends RedisSettings {
  /** name of the redis instance */
  def name: String
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any) = equalsAsInstance(obj)
  /** trait-specific equals, invokable from children */
  protected def equalsAsInstance(obj: scala.Any) = obj match {
    case that: RedisInstance => this.name == that.name && equalsAsSettings(that)
    case _                   => false
  }
  // $COVERAGE-ON$
}

/**
  * Type of Redis Instance - a cluster. It encapsulates common settings of the instance
  * and the list of cluster nodes.
  */
trait RedisCluster extends RedisInstance {
  /** nodes definition when cluster is defined */
  def nodes: List[RedisHost]
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any) = obj match {
    case that: RedisCluster => equalsAsInstance(that) && this.nodes == that.nodes
    case _                  => false
  }
  /** to string */
  override def toString = s"Cluster[${nodes mkString ","}]"
  // $COVERAGE-ON$
}

object RedisCluster {

  def apply(name: String, nodes: List[RedisHost], settings: RedisSettings) =
    create(name, nodes, settings)

  @inline
  private def create(_name: String, _nodes: List[RedisHost], _settings: RedisSettings) =
    new RedisCluster with RedisDelegatingSettings {
      val name = _name
      val nodes = _nodes
      val settings = _settings
    }
}

/**
  * A type of Redis Instance - a standalone instance. It encapsulates
  * common settings of the instance and provides a connection settings.
  */
trait RedisStandalone extends RedisInstance with RedisHost {
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any) = obj match {
    case that: RedisStandalone => equalsAsInstance(that) && equalsAsHost(that)
    case _                     => false
  }
  /** to string */
  override def toString = database match {
    case Some(database) => s"Standalone($name@$host:$port?db=$database)"
    case None           => s"Standalone($name@$host:$port)"
  }
  // $COVERAGE-ON$
}

object RedisStandalone {

  def apply(name: String, host: RedisHost, settings: RedisSettings) =
    create(name, host, settings)

  @inline
  private def create(_name: String, _host: RedisHost, _settings: RedisSettings) =
    new RedisStandalone with RedisDelegatingSettings with RedisDelegatingHost {
      val name = _name
      val innerHost = _host
      val settings = _settings
    }
}

/**
  * Type of Redis Instance - a sentinel. It encapsulates common settings of
  * the instance, name of the master group, and the list of sentinel nodes.
  */
trait RedisSentinel extends RedisInstance {

  def sentinels: List[RedisHost]
  def masterGroup: String
  def password: Option[String]
  def database: Option[Int]

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: RedisSentinel => equalsAsInstance(that) && this.sentinels == that.sentinels
  }
  /** to string */
  override def toString = s"Sentinel[${sentinels mkString ","}]"
}

object RedisSentinel {

  def apply(name: String, masterGroup: String,
      sentinels: List[RedisHost],
      settings: RedisSettings,
      password: Option[String] = None,
      database: Option[Int] = None): RedisSentinel with RedisDelegatingSettings =
    create(name, masterGroup, password, database, sentinels, settings)

  @inline
  private def create(_name: String, _masterGroup: String, _password: Option[String], _database: Option[Int],
      _sentinels: List[RedisHost], _settings: RedisSettings) =
    new RedisSentinel with RedisDelegatingSettings {
      val name = _name
      val masterGroup = _masterGroup
      val password = _password
      val database = _database
      val sentinels = _sentinels
      val settings = _settings
    }

}
