package play.api.cache.redis.connector

import org.apache.pekko.actor.ActorSystem
import play.api.cache.redis._
import play.api.cache.redis.test._

import java.util.Date
import scala.reflect.ClassTag
import scala.util.Random

class SerializerSpec extends AsyncUnitSpec {

  import SerializerSpec._

  "encode" when {

    test("byte") { implicit serializer =>
      0xab.toByte.encoded mustEqual "-85"
      JavaTypes.byteValue.encoded mustEqual "5"
    }

    test("byte[]") { implicit serializer =>
      JavaTypes.bytesValue.encoded mustEqual "AQID"
    }

    test("char") { implicit serializer =>
      'a'.encoded mustEqual "a"
      'b'.encoded mustEqual "b"
      'š'.encoded mustEqual "š"
    }

    test("boolean") { implicit serializer =>
      true.encoded mustEqual "true"
    }

    test("short") { implicit serializer =>
      12.toShort.toByte.encoded mustEqual "12"
    }

    test("int") { implicit serializer =>
      15.encoded mustEqual "15"
    }

    test("long") { implicit serializer =>
      144L.encoded mustEqual "144"
    }

    test("float") { implicit serializer =>
      1.23f.encoded mustEqual "1.23"
    }

    test("double") { implicit serializer =>
      3.14.encoded mustEqual "3.14"
    }

    test("string") { implicit serializer =>
      "some string".encoded mustEqual "some string"
    }

    test("date") { implicit serializer =>
      new Date(123).encoded mustEqual "rO0ABXNyAA5qYXZhLnV0aWwuRGF0ZWhqgQFLWXQZAwAAeHB3CAAAAAAAAAB7eA=="
    }

    test("null") { implicit serializer =>
      assertThrows[UnsupportedOperationException] {
        new ValueEncoder(null).encoded
      }
    }

    test("custom classes") { implicit serializer =>
      SimpleObject("B", 3).encoded mustEqual
        """
          |rO0ABXNyADpwbGF5LmFwaS5jYWNoZS5yZWRpcy5jb25uZWN0b3IuU2VyaWFsaXplclNwZWMkU2ltc
          |GxlT2JqZWN0LqzdylZUVb0CAAJJAAV2YWx1ZUwAA2tleXQAEkxqYXZhL2xhbmcvU3RyaW5nO3hwAA
          |AAA3QAAUI=
          """.stripMargin.withoutWhitespaces
    }

    test("list") { implicit serializer =>
      List("A", "B", "C").encoded mustEqual
        """
          |rO0ABXNyADJzY2FsYS5jb2xsZWN0aW9uLmdlbmVyaWMuRGVmYXVsdFNlcmlhbGl6YXRpb25Qcm94e
          |QAAAAAAAAADAwABTAAHZmFjdG9yeXQAGkxzY2FsYS9jb2xsZWN0aW9uL0ZhY3Rvcnk7eHBzcgAqc2
          |NhbGEuY29sbGVjdGlvbi5JdGVyYWJsZUZhY3RvcnkkVG9GYWN0b3J5AAAAAAAAAAMCAAFMAAdmYWN
          |0b3J5dAAiTHNjYWxhL2NvbGxlY3Rpb24vSXRlcmFibGVGYWN0b3J5O3hwc3IAJnNjYWxhLnJ1bnRp
          |bWUuTW9kdWxlU2VyaWFsaXphdGlvblByb3h5AAAAAAAAAAECAAFMAAttb2R1bGVDbGFzc3QAEUxqY
          |XZhL2xhbmcvQ2xhc3M7eHB2cgAgc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUuTGlzdCQAAAAAAA
          |AAAwIAAHhwdwT/////dAABQXQAAUJ0AAFDc3EAfgAGdnIAJnNjYWxhLmNvbGxlY3Rpb24uZ2VuZXJ
          |pYy5TZXJpYWxpemVFbmQkAAAAAAAAAAMCAAB4cHg=
            """.stripMargin.withoutWhitespaces
    }
  }

  "decode" when {

    test("byte") { implicit serializer =>
      "-85".decoded[Byte] mustEqual 0xab.toByte
    }

    test("byte[]") { implicit serializer =>
      "YWJj".decoded[Array[Byte]] mustEqual Array("a".head.toByte, "b".head.toByte, "c".head.toByte)
    }

    test("char") { implicit serializer =>
      "a".decoded[Char] mustEqual 'a'
      "b".decoded[Char] mustEqual 'b'
      "š".decoded[Char] mustEqual 'š'
    }

    test("boolean") { implicit serializer =>
      "true".decoded[Boolean] mustEqual true
    }

    test("short") { implicit serializer =>
      "12".decoded[Short] mustEqual 12.toShort.toByte
    }

    test("int") { implicit serializer =>
      "15".decoded[Int] mustEqual 15
    }

    test("long") { implicit serializer =>
      "144".decoded[Long] mustEqual 144L
    }

    test("float") { implicit serializer =>
      "1.23".decoded[Float] mustEqual 1.23f
    }

    test("double") { implicit serializer =>
      "3.14".decoded[Double] mustEqual 3.14
    }

    test("string") { implicit serializer =>
      "some string".decoded[String] mustEqual "some string"
    }

    test("null") { implicit serializer =>
      "".decoded[String] mustEqual null
    }

    test("date") { implicit serializer =>
      "rO0ABXNyAA5qYXZhLnV0aWwuRGF0ZWhqgQFLWXQZAwAAeHB3CAAAAAAAAAB7eA==".decoded[Date] mustEqual new Date(123)
    }

    test("invalid type") { implicit serializer =>
      assertThrows[IllegalArgumentException] {
        "something".decoded[Date]
      }
    }

    test("custom classes") { implicit serializer =>
      """
        |rO0ABXNyADpwbGF5LmFwaS5jYWNoZS5yZWRpcy5jb25uZWN0b3IuU2VyaWFsaXplclNwZWMkU2ltc
        |GxlT2JqZWN0LqzdylZUVb0CAAJJAAV2YWx1ZUwAA2tleXQAEkxqYXZhL2xhbmcvU3RyaW5nO3hwAA
        |AAA3QAAUI=
          """.stripMargin.withoutWhitespaces.decoded[SimpleObject] mustEqual SimpleObject("B", 3)
    }

    test("list") { implicit serializer =>
      """
        |rO0ABXNyADJzY2FsYS5jb2xsZWN0aW9uLmdlbmVyaWMuRGVmYXVsdFNlcmlhbGl6YXRpb25Qcm94e
        |QAAAAAAAAADAwABTAAHZmFjdG9yeXQAGkxzY2FsYS9jb2xsZWN0aW9uL0ZhY3Rvcnk7eHBzcgAqc2
        |NhbGEuY29sbGVjdGlvbi5JdGVyYWJsZUZhY3RvcnkkVG9GYWN0b3J5AAAAAAAAAAMCAAFMAAdmYWN
        |0b3J5dAAiTHNjYWxhL2NvbGxlY3Rpb24vSXRlcmFibGVGYWN0b3J5O3hwc3IAJnNjYWxhLnJ1bnRp
        |bWUuTW9kdWxlU2VyaWFsaXphdGlvblByb3h5AAAAAAAAAAECAAFMAAttb2R1bGVDbGFzc3QAEUxqY
        |XZhL2xhbmcvQ2xhc3M7eHB2cgAgc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUuTGlzdCQAAAAAAA
        |AAAwIAAHhwdwT/////dAABQXQAAUJ0AAFDc3EAfgAGdnIAJnNjYWxhLmNvbGxlY3Rpb24uZ2VuZXJ
        |pYy5TZXJpYWxpemVFbmQkAAAAAAAAAAMCAAB4cHg=
          """.stripMargin.withoutWhitespaces.decoded[List[String]] mustEqual List("A", "B", "C")
    }
  }

  private def test(name: String)(f: PekkoSerializer => Unit): Unit =
    name in {
      val system = ActorSystem.apply(s"test-${Random.nextInt()}", classLoader = Some(getClass.getClassLoader))
      val serializer: PekkoSerializer = new PekkoSerializerImpl(system)
      f(serializer)
      system.terminate().map(_ => Passed)
    }

}

object SerializerSpec {

  implicit private class ValueEncoder(private val any: Any) extends AnyVal {
    def encoded(implicit s: PekkoSerializer): String = s.encode(any).get
  }

  implicit private class StringDecoder(private val string: String) extends AnyVal {
    def decoded[T: ClassTag](implicit s: PekkoSerializer): T = s.decode[T](string).get
  }

  implicit private class StringOps(private val string: String) extends AnyVal {
    def withoutWhitespaces: String = string.replaceAll("\\s", "")
  }

  /** Plain test object to be cached */
  @SerialVersionUID(3363306882840417725L)
  final private case class SimpleObject(key: String, value: Int)

}
