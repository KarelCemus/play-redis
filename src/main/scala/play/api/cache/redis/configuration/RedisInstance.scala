package play.api.cache.redis.configuration

import play.api.cache.redis._

/**
  * Abstraction over clusters and standalone instances. This trait
  * encapsulates a common settings and simplifies pattern matching.
  */
sealed trait RedisInstance extends RedisSettings {
  /** name of the redis instance */
  def name: String
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any): Boolean = equalsAsInstance(obj)
  /** trait-specific equals, invokable from children */
  protected def equalsAsInstance(obj: scala.Any): Boolean = obj match {
    case that: RedisInstance => this.name === that.name && equalsAsSettings(that)
    case _                   => false
  }
  // $COVERAGE-ON$
}

/**
  * Type of Redis Instance - a cluster. It encapsulates common settings of the instance
  * and the list of cluster nodes.
  */
sealed trait RedisCluster extends RedisInstance {
  /** nodes definition when cluster is defined */
  def nodes: List[RedisHost]
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any): Boolean = obj match {
    case that: RedisCluster => equalsAsInstance(that) && this.nodes === that.nodes
    case _                  => false
  }
  /** to string */
  override def toString: String = s"Cluster[${nodes mkString ","}]"
  // $COVERAGE-ON$
}

object RedisCluster {

  def apply(name: String, nodes: List[RedisHost], settings: RedisSettings): RedisCluster =
    create(name, nodes, settings)

  @inline
  private def create(_name: String, _nodes: List[RedisHost], _settings: RedisSettings): RedisCluster =
    new RedisCluster with RedisDelegatingSettings {
      override val name: String = _name
      override val nodes: List[RedisHost] = _nodes
      override val settings: RedisSettings = _settings
    }
}

/**
  * A type of Redis Instance - a standalone instance. It encapsulates
  * common settings of the instance and provides a connection settings.
  */
sealed trait RedisStandalone extends RedisInstance with RedisHost {
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any): Boolean = obj match {
    case that: RedisStandalone => equalsAsInstance(that) && equalsAsHost(that)
    case _                     => false
  }
  /** to string */
  override def toString: String = database match {
    case Some(database) => s"Standalone($name@$host:$port?db=$database)"
    case None           => s"Standalone($name@$host:$port)"
  }
  // $COVERAGE-ON$
}

object RedisStandalone {

  def apply(name: String, host: RedisHost, settings: RedisSettings): RedisStandalone =
    create(name, host, settings)

  @inline
  private def create(_name: String, _host: RedisHost, _settings: RedisSettings): RedisStandalone =
    new RedisStandalone with RedisDelegatingSettings with RedisDelegatingHost {
      override val name: String = _name
      override val innerHost: RedisHost = _host
      override val settings: RedisSettings = _settings
    }
}

/**
  * Type of Redis Instance - a sentinel. It encapsulates common settings of
  * the instance, name of the master group, and the list of sentinel nodes.
  */
sealed trait RedisSentinel extends RedisInstance {

  def sentinels: List[RedisHost]
  def masterGroup: String
  def username: Option[String]
  def password: Option[String]
  def database: Option[Int]

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: RedisSentinel => equalsAsInstance(that) && this.sentinels === that.sentinels
    case _                   => false
  }
  /** to string */
  override def toString: String = s"Sentinel[${sentinels mkString ","}]"
}

object RedisSentinel {

  def apply(
    name: String,
    masterGroup: String,
    sentinels: List[RedisHost],
    settings: RedisSettings,
    username: Option[String] = None,
    password: Option[String] = None,
    database: Option[Int] = None
  ): RedisSentinel =
    create(name, masterGroup, username, password, database, sentinels, settings)

  @inline
  private def create(_name: String, _masterGroup: String, _username: Option[String], _password: Option[String], _database: Option[Int],
      _sentinels: List[RedisHost], _settings: RedisSettings): RedisSentinel =
    new RedisSentinel with RedisDelegatingSettings {
      override val name: String = _name
      override val masterGroup: String = _masterGroup
      override val username: Option[String] = _username
      override val password: Option[String] = _password
      override val database: Option[Int] = _database
      override val sentinels: List[RedisHost] = _sentinels
      override val settings: RedisSettings = _settings
    }

}
