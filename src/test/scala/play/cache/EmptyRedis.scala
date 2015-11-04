package play.cache

import play.api.Application

import org.specs2.specification.BeforeAll

/**
 * Set up Redis server, empty its testing database to avoid any inference with previous tests
 *
 * @author Karel Cemus
 */
trait EmptyRedis extends BeforeAll { self: Redis =>

  override def beforeAll( ): Unit = EmptyRedis.empty
}

object EmptyRedis extends RedisInstance {

  /** already executed */
  private var executed = false

  /** empty redis database at the beginning of the test suite */
  def empty( implicit application: Application ): Unit = synchronized {
    if ( !executed ) { // execute only once
      println("----------------------------")
      executed = true
      redis execute "FLUSHDB"
    }
  }
}
