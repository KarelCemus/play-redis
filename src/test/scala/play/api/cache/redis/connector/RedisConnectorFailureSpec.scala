package play.api.cache.redis.connector

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.Failure

import play.api.cache.redis._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import redis._

/**
  * @author Karel Cemus
  */
class RedisConnectorFailureSpec( implicit ee: ExecutionEnv ) extends Specification {
  import Implicits._

  val key = "key"
  val value = "value"

  val simulatedEx = new RuntimeException( "Simulated failure." )
  val simulatedFailure = Failure( simulatedEx )

  val someValue = Some( value )

  val disconnected = Future.failed( new IllegalStateException( "Simulated redis status: disconnected." ) )

  def anySerializer = org.mockito.ArgumentMatchers.any[ ByteStringSerializer[ String ] ]
  def anyDeserializer = org.mockito.ArgumentMatchers.any[ ByteStringDeserializer[ String ] ]

  "Serializer failure" should {
    import MockitoImplicits._

    "fail when serialization fails" in new MockedConnector {
      serializer.encode( any[ Any ] ) returns simulatedFailure
      // run the test
      connector.set( key, value ) must throwA[ SerializationException ].await
    }

    "fail when decoder fails" in new MockedConnector {
      serializer.decode( anyString )( any[ ClassTag[ _ ] ] ) returns simulatedFailure
      commands.get[ String ]( key ) returns someValue
      // run the test
      connector.get[ String ]( key ) must throwA[ SerializationException ].await
    }
  }

  "Redis returning error code" should {

    "SET returning false" in new MockedConnector {
      import org.mockito.ArgumentMatchers._
      import org.mockito.Mockito._

      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.set[ String ]( anyString, anyString, any[ Some[ Long ] ], any[ Some[ Long ] ], anyBoolean, anyBoolean )( anySerializer ) ) thenReturn false
      // run the test
      connector.set( key, value ) must not( throwA[ Throwable ] ).await
    }

    "EXPIRE returning false" in new MockedConnector {
      import MockitoImplicits._

      commands.expire( anyString, any[ Long ] ) returns false
      // run the test
      connector.expire( key, 1.minute ) must not( throwA[ Throwable ] ).await
    }
  }

  "Connector failure" should {
    import org.mockito.ArgumentMatchers._
    import org.mockito.Mockito._

    "failed SETEX" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.setex( anyString, anyLong, anyString )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.set( key, value, 1.minute ) must throwA[ ExecutionFailedException ].await
    }

    "failed SETNX" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.setnx( anyString, anyString )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.setIfNotExists( key, value ) must throwA[ ExecutionFailedException ].await
    }

    "failed SET" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.set( anyString, anyString, any, any, anyBoolean, anyBoolean )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.set( key, value ) must throwA[ ExecutionFailedException ].await
    }

    "failed MSET" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.mset[ String ]( any[ Map[ String, String ] ] )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.mSet( key -> value ) must throwA[ ExecutionFailedException ].await
    }

    "failed MSETNX" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.msetnx[ String ]( any[ Map[ String, String ] ] )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.mSetIfNotExist( key -> value ) must throwA[ ExecutionFailedException ].await
    }

    "failed EXPIRE" in new MockedConnector {
      when( commands.expire( anyString, anyLong ) ) thenReturn disconnected
      // run the test
      connector.expire( key, 1.minute ) must throwA[ ExecutionFailedException ].await
    }

    "failed INCRBY" in new MockedConnector {
      when( commands.incrby( anyString, anyLong ) ) thenReturn disconnected
      // run the test
      connector.increment( key, 1L ) must throwA[ ExecutionFailedException ].await
    }

    "failed LRANGE" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.lrange[ String ]( anyString, anyLong, anyLong )( anyDeserializer ) ) thenReturn disconnected
      // run the test
      connector.listSlice[ String ]( key, 0, -1 ) must throwA[ ExecutionFailedException ].await
    }

    "failed LREM" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.lrem( anyString, anyLong, anyString )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.listRemove( key, value, 2 ) must throwA[ ExecutionFailedException ].await
    }

    "failed LTRIM" in new MockedConnector {
      when( commands.ltrim( anyString, anyLong, anyLong ) ) thenReturn disconnected
      // run the test
      connector.listTrim( key, 1, 5 ) must throwA[ ExecutionFailedException ].await
    }

    "failed LINSERT" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.linsert[ String ]( anyString, any[ api.ListPivot ], anyString, anyString )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.listInsert( key, "pivot", value ) must throwA[ ExecutionFailedException ].await
    }

    "failed HINCRBY" in new MockedConnector {
      when( commands.hincrby( anyString, anyString, anyLong ) ) thenReturn disconnected
      // run the test
      connector.hashIncrement( key, "field", 1 ) must throwA[ ExecutionFailedException ].await
    }

    "failed HSET" in new MockedConnector {
      when( serializer.encode( anyString ) ) thenReturn "encoded"
      when( commands.hset[ String ]( anyString, anyString, anyString )( anySerializer ) ) thenReturn disconnected
      // run the test
      connector.hashSet( key, "field", value ) must throwA[ ExecutionFailedException ].await
    }
  }
}
