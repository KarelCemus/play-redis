package play.api.cache.redis.configuration

import java.net.InetAddress

/**
  * @author Karel Cemus
  */
object HostnameResolver {

  implicit class HostNameResolver( val name: String ) extends AnyVal {
    def resolvedIpAddress = InetAddress.getByName( name ).getHostAddress
  }
}
