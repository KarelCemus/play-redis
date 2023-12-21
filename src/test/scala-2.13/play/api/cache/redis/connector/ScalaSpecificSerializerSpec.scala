package play.api.cache.redis.connector

import play.api.inject.guice.GuiceApplicationBuilder

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ScalaSpecificSerializerSpec extends Specification with Mockito {
  import SerializerImplicits._

  private val system = GuiceApplicationBuilder().build().actorSystem

  private implicit val serializer: PekkoSerializer = new AkkaSerializerImpl(system)

  "AkkaEncoder" should "encode" >> {

    "custom classes" in {
      SimpleObject("B", 3).encoded mustEqual
        """
          |rO0ABXNyAD9wbGF5LmFwaS5jYWNoZS5yZWRpcy5jb25uZWN0b3IuU2VyaWFsaXplckltcGxpY2l0
          |cyRTaW1wbGVPYmplY3TyYCEG2fNkUQIAAkkABXZhbHVlTAADa2V5dAASTGphdmEvbGFuZy9TdHJp
          |bmc7eHAAAAADdAABQg==
        """.stripMargin.removeAllWhitespaces
    }

    "list" in {
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
        """.stripMargin.removeAllWhitespaces
    }
  }

  "AkkaDecoder" should "decode" >> {

    "custom classes" in {
      """
        |rO0ABXNyAD9wbGF5LmFwaS5jYWNoZS5yZWRpcy5jb25uZWN0b3IuU2VyaWFsaXplckltcGxpY2l0
        |cyRTaW1wbGVPYmplY3TyYCEG2fNkUQIAAkkABXZhbHVlTAADa2V5dAASTGphdmEvbGFuZy9TdHJp
        |bmc7eHAAAAADdAABQg==
      """.stripMargin.removeAllWhitespaces.decoded[SimpleObject] mustEqual SimpleObject("B", 3)
    }

    "list" in {
      """
        |rO0ABXNyADJzY2FsYS5jb2xsZWN0aW9uLmdlbmVyaWMuRGVmYXVsdFNlcmlhbGl6YXRpb25Qcm94e
        |QAAAAAAAAADAwABTAAHZmFjdG9yeXQAGkxzY2FsYS9jb2xsZWN0aW9uL0ZhY3Rvcnk7eHBzcgAqc2
        |NhbGEuY29sbGVjdGlvbi5JdGVyYWJsZUZhY3RvcnkkVG9GYWN0b3J5AAAAAAAAAAMCAAFMAAdmYWN
        |0b3J5dAAiTHNjYWxhL2NvbGxlY3Rpb24vSXRlcmFibGVGYWN0b3J5O3hwc3IAJnNjYWxhLnJ1bnRp
        |bWUuTW9kdWxlU2VyaWFsaXphdGlvblByb3h5AAAAAAAAAAECAAFMAAttb2R1bGVDbGFzc3QAEUxqY
        |XZhL2xhbmcvQ2xhc3M7eHB2cgAgc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUuTGlzdCQAAAAAAA
        |AAAwIAAHhwdwT/////dAABQXQAAUJ0AAFDc3EAfgAGdnIAJnNjYWxhLmNvbGxlY3Rpb24uZ2VuZXJ
        |pYy5TZXJpYWxpemVFbmQkAAAAAAAAAAMCAAB4cHg=
      """.stripMargin.removeAllWhitespaces.decoded[List[String]] mustEqual List("A", "B", "C")
    }
  }
}
