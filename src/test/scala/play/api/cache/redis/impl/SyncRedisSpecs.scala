package play.api.cache.redis.impl

import scala.concurrent.duration._

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

/**
  * @author Karel Cemus
  */
class SyncRedisSpecs( implicit ee: ExecutionEnv ) extends Specification with ReducedMockito {

  import Implicits._
  import RedisCacheImplicits._

  import org.mockito.ArgumentMatchers._

  "SyncRedis" should {

    "get or else (hit)" in new MockedSyncRedis with OrElse {
      connector.get[ String ]( anyString )( anyClassTag ) returns Some( value )
      cache.getOrElse( key )( doElse( value ) ) must beEqualTo( value )
      orElse mustEqual 0
    }

    "get or else (miss)" in new MockedSyncRedis with OrElse {
      connector.get[ String ]( anyString )( anyClassTag ) returns None
      connector.set( anyString, anyString, any[ Duration ], anyBoolean ) returns true
      cache.getOrElse( key )( doElse( value ) ) must beEqualTo( value )
      orElse mustEqual 1
    }

    "get or else (failure)" in new MockedSyncRedis with OrElse {
      connector.get[ String ]( anyString )( anyClassTag ) returns ex
      connector.set( anyString, anyString, any[ Duration ], anyBoolean ) returns true
      cache.getOrElse( key )( doElse( value ) ) must beEqualTo( value )
      orElse mustEqual 1
    }
  }
}
