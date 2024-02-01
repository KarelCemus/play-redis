package play.api.cache.redis.configuration

import java.net.InetAddress

object HostnameResolver {

  implicit class HostNameResolver(private val name: String) extends AnyVal {
    def resolvedIpAddress: String = InetAddress.getByName(name).getHostAddress
  }
}
