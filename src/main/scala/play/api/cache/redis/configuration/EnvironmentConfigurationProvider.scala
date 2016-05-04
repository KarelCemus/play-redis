package play.api.cache.redis.configuration

import javax.inject.Provider

/**
 * Reads environment variables for the connection string and returns EnvironmentConfiguration instance.
 * This configuration instance is designed to work in PaaS environments such as Heroku.
 *
 * @param variable name of the variable with the connection string in the environment
 */
class EnvironmentConfigurationProvider( variable: String ) extends Provider[ EnvironmentConfiguration ] {

  /** expected format of the environment variable */
  private val REDIS_URL = "redis://((?<user>[^:]+):(?<password>[^@]+)@)?(?<host>[^:]+):(?<port>[0-9]+)".r( "auth", "user", "password", "host", "port" )

  /** read environment url or throw an exception */
  override def get( ): EnvironmentConfiguration = url.map( REDIS_URL.findFirstMatchIn ) match {
    // read the environment variable and fill missing information from the local configuration file
    case Some( Some( matcher ) ) => new EnvironmentConfiguration( matcher.group( "host" ), matcher.group( "port" ).toInt, Option( matcher.group( "password" ) ) )
    // value is defined but not in the expected format
    case Some( None ) => throw new IllegalArgumentException( s"Unexpected value in the environment variable '$variable'. Expected format is 'redis://[user:password@]host:port'." )
    // variable is missing
    case None => throw new IllegalArgumentException( s"Expected environment variable '$variable' is missing. Expected value is 'redis://[user:password@]host:port'." )
  }

  /** returns the connection url to redis server */
  protected def url = sys.env.get( variable )
}
