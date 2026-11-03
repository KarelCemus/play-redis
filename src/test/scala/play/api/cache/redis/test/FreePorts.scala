package play.api.cache.redis.test

import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import scala.util.Using

/**
  * Allocates host ports that are free now and not handed out earlier by this
  * JVM. Cluster bus ports are port + 10000 and all of them must stay below
  * 32768, where the Linux ephemeral range starts; a node cannot bind a port
  * already taken by an outgoing gossip connection.
  */
object FreePorts {

  private val reserved: java.util.Set[Int] = ConcurrentHashMap.newKeySet[Int]()

  def range(count: Int): Int = synchronized {
    val base = LazyList
      .continually(scala.util.Random.between(20000, 22768 - count))
      .find(base => base.until(base + count).forall(port => !reserved.contains(port) && isFree(port)))
      .getOrElse(throw new IllegalStateException(s"No range of $count free ports"))
    base.until(base + count).foreach(reserved.add)
    base
  }

  def one(): Int = range(1)

  private def isFree(port: Int): Boolean =
    Using(new ServerSocket(port))(_ => true).getOrElse(false)

}
