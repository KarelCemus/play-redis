package play.api.cache.redis.configuration

import play.api.ConfigLoader

import com.typesafe.config.Config

/**
  * Configures a single node either a standalone or within a cluster.
  */
trait RedisHost {
  /** host with redis server */
  def host: String
  /** port redis listens on */
  def port: Int
  /** redis database identifier to work with */
  def database: Option[Int]
  /** when enabled security, this returns username for the AUTH command */
  def username: Option[String]
  /** when enabled security, this returns password for the AUTH command */
  def password: Option[String]
  // $COVERAGE-OFF$
  /** trait-specific equals */
  override def equals(obj: scala.Any) = equalsAsHost(obj)
  /** trait-specific equals, invokable from children */
  protected def equalsAsHost(obj: scala.Any) = obj match {
    case that: RedisHost => Equals.check(this, that)(_.host, _.port, _.username, _.database, _.password)
    case _               => false
  }
  /** to string */
  override def toString: String = (password, database) match {
    case (Some(password), Some(database)) => s"redis://${username.getOrElse("redis")}:$password@$host:$port?db=$database"
    case (Some(password), None)           => s"redis://${username.getOrElse("redis")}:$password@$host:$port"
    case (None, Some(database))           => s"redis://$host:$port?db=$database"
    case (None, None)                     => s"redis://$host:$port"
  }
  // $COVERAGE-ON$
}

object RedisHost extends ConfigLoader[RedisHost] {
  import RedisConfigLoader._

  /** expected format of the environment variable */
  private val ConnectionString = "redis://((?<username>[^:]+):(?<password>[^@]+)@)?(?<host>[^:]+):(?<port>[0-9]+)".r("auth", "username", "password", "host", "port")

  def load(config: Config, path: String): RedisHost = apply(
    host = config.getString(path / "host"),
    port = config.getInt(path / "port"),
    database = config.getOption(path / "database", _.getInt),
    username = config.getOption(path / "username", _.getString),
    password = config.getOption(path / "password", _.getString)
  )

  /** read environment url or throw an exception */
  def fromConnectionString(connectionString: String): RedisHost = ConnectionString findFirstMatchIn connectionString match {
    // read the environment variable and fill missing information from the local configuration file
    case Some(matcher) => new RedisHost {
      override val host: String = matcher.group("host")
      override val port: Int = matcher.group("port").toInt
      override val database: Option[Nothing] = None
      override val username: Option[String] = Option(matcher.group("username"))
      override val password: Option[String] = Option(matcher.group("password"))
    }
    // unexpected format
    case None => throw new IllegalArgumentException(s"Unexpected format of the connection string: '$connectionString'. Expected format is 'redis://[user:password@]host:port'.")
  }

  def apply(host: String, port: Int, database: Option[Int] = None, username: Option[String] = None, password: Option[String] = None): RedisHost =
    create(host, port, database, username, password)

  /** hackish method to preserve nice names of parameters in apply */
  @inline private def create(_host: String, _port: Int, _database: Option[Int], _username: Option[String], _password: Option[String]) = new RedisHost {
    override val host: String = _host
    override val port: Int = _port
    override val database: Option[Int] = _database
    override val username: Option[String] = _username
    override val password: Option[String] = _password
  }

  // $COVERAGE-OFF$
  def unapply(host: RedisHost): Option[(String, Int, Option[Int],Option[String], Option[String])] = {
    Some((host.host, host.port, host.database, host.username, host.password))
  }
  // $COVERAGE-ON$
}

/**
  *
  * A helper trait delegating properties into the inner settings object
  */
trait RedisDelegatingHost extends RedisHost {
  def innerHost: RedisHost
  override def host: String = innerHost.host
  override def port: Int = innerHost.port
  override def database: Option[Int] = innerHost.database
  override def username: Option[String] = innerHost.username
  override def password: Option[String] = innerHost.password
}
