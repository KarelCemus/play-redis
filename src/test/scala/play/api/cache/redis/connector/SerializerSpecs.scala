package play.api.cache.redis.connector

import java.util.Date

import scala.reflect.ClassTag

import play.api.cache.redis.{Redis, SimpleObject}

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
      new Date( 123 ).encoded mustEqual "rO0ABXNyAA5qYXZhLnV0aWwuRGF0ZWhqgQFLWXQZAwAAeHB3CAAAAAAAAAB7eA=="
    }

    "datetime" in {
      new DateTime( 123456L, DateTimeZone.forID( "UTC" ) ).encoded mustEqual """
          |rO0ABXNyABZvcmcuam9kYS50aW1lLkRhdGVUaW1luDx4ZGpb3fkCAAB4cgAfb3JnLmpvZGEudGlt
          |ZS5iYXNlLkJhc2VEYXRlVGltZf//+eFPXS6jAgACSgAHaU1pbGxpc0wAC2lDaHJvbm9sb2d5dAAa
          |TG9yZy9qb2RhL3RpbWUvQ2hyb25vbG9neTt4cAAAAAAAAeJAc3IAJ29yZy5qb2RhLnRpbWUuY2hy
          |b25vLklTT0Nocm9ub2xvZ3kkU3R1YqnIEWZxN1AnAwAAeHBzcgAfb3JnLmpvZGEudGltZS5EYXRl
          |VGltZVpvbmUkU3R1YqYvAZp8MhrjAwAAeHB3BQADVVRDeHg=
        """.stripMargin.trim
    }

    "custom classes" in {
      SimpleObject( "B", 3 ).encoded mustEqual  """
        |rO0ABXNyACFwbGF5LmFwaS5jYWNoZS5yZWRpcy5TaW1wbGVPYmplY3Te6Peta/KhNAIAAkkABXZh
        |bHVlTAADa2V5dAASTGphdmEvbGFuZy9TdHJpbmc7eHAAAAADdAABQg==
      """.stripMargin.trim
    }

    "null" in {
      new ValueEncoder( null ).encoded must throwA[ UnsupportedOperationException ]
    }

    "list" in {
      List( "A", "B", "C" ).encoded mustEqual """
        |rO0ABXNyADJzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS5MaXN0JFNlcmlhbGl6YXRpb25Qcm94
        |eQAAAAAAAAABAwAAeHB0AAFBdAABQnQAAUNzcgAsc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUu
        |TGlzdFNlcmlhbGl6ZUVuZCSKXGNb91MLbQIAAHhweA==
      """.stripMargin.trim
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
      "rO0ABXNyAA5qYXZhLnV0aWwuRGF0ZWhqgQFLWXQZAwAAeHB3CAAAAAAAAAB7eA==".decoded[ Date ] mustEqual new Date( 123 )
    }

    "datetime" in {
      """
        |rO0ABXNyABZvcmcuam9kYS50aW1lLkRhdGVUaW1luDx4ZGpb3fkCAAB4cgAfb3JnLmpvZGEudGlt
        |ZS5iYXNlLkJhc2VEYXRlVGltZf//+eFPXS6jAgACSgAHaU1pbGxpc0wAC2lDaHJvbm9sb2d5dAAa
        |TG9yZy9qb2RhL3RpbWUvQ2hyb25vbG9neTt4cAAAAAAAAeJAc3IAJ29yZy5qb2RhLnRpbWUuY2hy
        |b25vLklTT0Nocm9ub2xvZ3kkU3R1YqnIEWZxN1AnAwAAeHBzcgAfb3JnLmpvZGEudGltZS5EYXRl
        |VGltZVpvbmUkU3R1YqYvAZp8MhrjAwAAeHB3BQADVVRDeHg=
      """.stripMargin.trim.decoded[ DateTime ] mustEqual new DateTime( 123456L, DateTimeZone.forID( "UTC" ) )
    }

    "custom classes" in {
      """
        |rO0ABXNyACFwbGF5LmFwaS5jYWNoZS5yZWRpcy5TaW1wbGVPYmplY3Te6Peta/KhNAIAAkkABXZh
        |bHVlTAADa2V5dAASTGphdmEvbGFuZy9TdHJpbmc7eHAAAAADdAABQg==
      """.stripMargin.trim.decoded[ SimpleObject ] mustEqual SimpleObject( "B", 3 )
    }

    "list" in {
      """
        |rO0ABXNyADJzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS5MaXN0JFNlcmlhbGl6YXRpb25Qcm94
        |eQAAAAAAAAABAwAAeHB0AAFBdAABQnQAAUNzcgAsc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUu
        |TGlzdFNlcmlhbGl6ZUVuZCSKXGNb91MLbQIAAHhweA==
      """.stripMargin.trim.decoded[ List[ String ] ] mustEqual List( "A", "B", "C" )
    }

  }

}
