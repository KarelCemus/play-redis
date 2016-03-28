package play.api.cache.redis

import java.util.Date

import scala.reflect.ClassTag

import org.joda.time.DateTime
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
      new DateTime( 123456 ).encoded mustEqual """
          |AQBvcmcuam9kYS50aW1lLkRhdGVUaW3lAQEBb3JnLmpvZGEudGltZS5jaHJvbm8uSVNPQ2hyb25v
          |bG9n+QEBAm9yZy5qb2RhLnRpbWUuY2hyb25vLlpvbmVkQ2hyb25vbG9n+QEBAQEBA29yZy5qb2Rh
          |LnRpbWUuY2hyb25vLkdyZWdvcmlhbkNocm9ub2xvZ/kBAAgAAAEEb3JnLmpvZGEudGltZS50ei5D
          |YWNoZWREYXRlVGltZVpvbuUBAUV1cm9wZS9QcmFndeUBBW9yZy5qb2RhLnRpbWUudHouRGF0ZVRp
          |bWVab25lQnVpbGRlciRQcmVjYWxjdWxhdGVkWm9u5QEIAT4BTE3UAVBN1AFDRdQBQ0VT1A0ODQ4N
          |Dg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQE+gO2m
          |A4DtpgOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcD
          |gLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOA
          |urcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6
          |twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3
          |A4C6twOAurcDgLq3AwEBAUNF1AEAAQ6AurcDAHUUAAiAurcDAQFDRVPUAQABDoC6twMAdQaAurcD
          |AT7/////////////nM+JttwB/5yRvd6PAf/DpYzLYv+ZhsroYf/18NrpYP+V1smGYP+1mrL/Xv/V
          |/6CcXv+V47HTNv+llojzMf/V37eUMf+l0OKaMP+Fpc6lL//19I26Lv+lnbe3Lf+lmZ6nLP+1g7m6
          |K//Fm/rXKv+FqOTZKf+FxdHtKP/F0bvvJ//F7qiDJ/+FkqaKJv+FmICZJYCq1PP9EIDK/4fzEYD6
          |jt3sEoCK1rDdE4CqgcXSFIDKrNnHFYDq1+28FoCKg4KyF4CqrpanGIDK2aqcGYDqhL+RGoCalJSL
          |G4C6v6iAHIDa6rz1HID6ldHqHYCaweXfHoC67PnUH4Dal47KIID6wqK/IYCa7ra0IoC6mcupI4Da
          |xN+eJID67/OTJYCq/8iNJoDKqt2CJ4Dq1fH3J4CKgYbtKICqrJriKYDK167XKoDqgsPMK4CKrtfB
          |LICq2eu2LYDKhICsLoDqr5ShL4Cav+maMID6+oCiMQE+gO2mA4DtpgOAurcDgPTuBoC6twOA9O4G
          |gLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaA
          |urcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6
          |twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3
          |A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3AwCAiQ8=
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
        |bG9n+QEBAm9yZy5qb2RhLnRpbWUuY2hyb25vLlpvbmVkQ2hyb25vbG9n+QEBAQEBA29yZy5qb2Rh
        |LnRpbWUuY2hyb25vLkdyZWdvcmlhbkNocm9ub2xvZ/kBAAgAAAEEb3JnLmpvZGEudGltZS50ei5D
        |YWNoZWREYXRlVGltZVpvbuUBAUV1cm9wZS9QcmFndeUBBW9yZy5qb2RhLnRpbWUudHouRGF0ZVRp
        |bWVab25lQnVpbGRlciRQcmVjYWxjdWxhdGVkWm9u5QEIAT4BTE3UAVBN1AFDRdQBQ0VT1A0ODQ4N
        |Dg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQ4NDg0ODQE+gO2m
        |A4DtpgOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcD
        |gLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOA
        |urcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6
        |twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3A4C6twOAurcDgLq3
        |A4C6twOAurcDgLq3AwEBAUNF1AEAAQ6AurcDAHUUAAiAurcDAQFDRVPUAQABDoC6twMAdQaAurcD
        |AT7/////////////nM+JttwB/5yRvd6PAf/DpYzLYv+ZhsroYf/18NrpYP+V1smGYP+1mrL/Xv/V
        |/6CcXv+V47HTNv+llojzMf/V37eUMf+l0OKaMP+Fpc6lL//19I26Lv+lnbe3Lf+lmZ6nLP+1g7m6
        |K//Fm/rXKv+FqOTZKf+FxdHtKP/F0bvvJ//F7qiDJ/+FkqaKJv+FmICZJYCq1PP9EIDK/4fzEYD6
        |jt3sEoCK1rDdE4CqgcXSFIDKrNnHFYDq1+28FoCKg4KyF4CqrpanGIDK2aqcGYDqhL+RGoCalJSL
        |G4C6v6iAHIDa6rz1HID6ldHqHYCaweXfHoC67PnUH4Dal47KIID6wqK/IYCa7ra0IoC6mcupI4Da
        |xN+eJID67/OTJYCq/8iNJoDKqt2CJ4Dq1fH3J4CKgYbtKICqrJriKYDK167XKoDqgsPMK4CKrtfB
        |LICq2eu2LYDKhICsLoDqr5ShL4Cav+maMID6+oCiMQE+gO2mA4DtpgOAurcDgPTuBoC6twOA9O4G
        |gLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaA
        |urcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6
        |twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3
        |A4D07gaAurcDgPTuBoC6twOA9O4GgLq3A4D07gaAurcDgPTuBoC6twOA9O4GgLq3AwCAiQ8=
      """.stripMargin.trim.decoded[ DateTime ] mustEqual new DateTime( 123456 )
    }

    "custom classes" in {
      "AQBwbGF5LmFwaS5jYWNoZS5yZWRpcy5TaW1wbGVPYmplY/QBAYJCBg==".decoded[ SimpleObject ] mustEqual SimpleObject( "B", 3 )
    }

    "list" in {
      "AQBzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS4kY29sb24kY29sb+4BAwMBgkEDAYJCAwGCQw==".decoded[ List[ String ] ] mustEqual List( "A", "B", "C" )
    }

  }

}
