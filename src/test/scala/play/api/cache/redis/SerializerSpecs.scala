package play.api.cache.redis

import java.util.Date

import scala.reflect.ClassTag

import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mutable.Specification

/**
  * @author Karel Cemus
  */
class SerializerSpecs extends Specification {

  private val serializer: AkkaSerializer = Redis.injector.instanceOf[ AkkaSerializer ]

  private implicit class ValueEncoder( any: Any ) {
    def encoded: String = serializer.encode( any ).get
  }

  private implicit class StringDecoder( string: String ) {
    def decoded[ T: ClassTag ]: T = serializer.decode[ T ]( string ).get
  }

  "AkkaEncoder" should "encode" >> {

    "byte" in {
      0xAB.toByte.encoded mustEqual "-85"
    }

    "char" in {
      'a'.encoded mustEqual "a"
      'b'.encoded mustEqual "b"
      'š'.encoded mustEqual "š"
    }

    "short" in {
      12.toShort.toByte.encoded mustEqual "12"
    }

    "int" in {
      15.encoded mustEqual "15"
    }

    "long" in {
      144L.encoded mustEqual "144"
    }

    "float" in {
      1.23f.encoded mustEqual "1.23"
    }

    "double" in {
      3.14.encoded mustEqual "3.14"
    }

    "string" in {
      "some string".encoded mustEqual "some string"
    }

    "date" in {
      new Date( 123 ).encoded mustEqual "AQBqYXZhLnV0aWwuRGF05QF7"
    }

    "datetime" in {
      new DateTime( 123456L, DateTimeZone.forID( "UTC" ) ).encoded mustEqual """
          |AQBvcmcuam9kYS50aW1lLkRhdGVUaW3lAQEBb3JnLmpvZGEudGltZS5jaHJvbm8uSVNPQ2hyb25v
          |bG9n+QEBAm9yZy5qb2RhLnRpbWUuY2hyb25vLkdyZWdvcmlhbkNocm9ub2xvZ/kBAAgAAICJDw==
        """.stripMargin.trim
    }

    "custom classes" in {
      SimpleObject( "B", 3 ).encoded mustEqual "AQBwbGF5LmFwaS5jYWNoZS5yZWRpcy5TaW1wbGVPYmplY/QBAYJCBg=="
    }

    "null" in {
      new ValueEncoder( null ).encoded must throwA[ UnsupportedOperationException ]
    }

    "list" in {
      List( "A", "B", "C" ).encoded mustEqual "AQBzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS4kY29sb24kY29sb+4BAwMBgkEDAYJCAwGCQw=="
    }

  }

  "AkkaDecoder" should "decode" >> {

    "byte" in {
      "-85".decoded[ Byte ] mustEqual 0xAB.toByte
    }

    "char" in {
      "a".decoded[ Char ] mustEqual 'a'
      "b".decoded[ Char ] mustEqual 'b'
      "š".decoded[ Char ] mustEqual 'š'
    }

    "short" in {
      "12".decoded[ Short ] mustEqual 12.toShort.toByte
    }

    "int" in {
      "15".decoded[ Int ] mustEqual 15
    }

    "long" in {
      "144".decoded[ Long ] mustEqual 144L
    }

    "float" in {
      "1.23".decoded[ Float ] mustEqual 1.23f
    }

    "double" in {
      "3.14".decoded[ Double ] mustEqual 3.14
    }

    "string" in {
      "some string".decoded[ String ] mustEqual "some string"
    }

    "date" in {
      "AQBqYXZhLnV0aWwuRGF05QF7".decoded[ Date ] mustEqual new Date( 123 )
    }

    "datetime" in {
      """
        |AQBvcmcuam9kYS50aW1lLkRhdGVUaW3lAQEBb3JnLmpvZGEudGltZS5jaHJvbm8uSVNPQ2hyb25v
        |bG9n+QEBAm9yZy5qb2RhLnRpbWUuY2hyb25vLkdyZWdvcmlhbkNocm9ub2xvZ/kBAAgAAICJDw==
      """.stripMargin.trim.decoded[ DateTime ] mustEqual new DateTime( 123456L, DateTimeZone.forID( "UTC" ) )
    }

    "custom classes" in {
      "AQBwbGF5LmFwaS5jYWNoZS5yZWRpcy5TaW1wbGVPYmplY/QBAYJCBg==".decoded[ SimpleObject ] mustEqual SimpleObject( "B", 3 )
    }

    "list" in {
      "AQBzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS4kY29sb24kY29sb+4BAwMBgkEDAYJCAwGCQw==".decoded[ List[ String ] ] mustEqual List( "A", "B", "C" )
    }

  }

}
