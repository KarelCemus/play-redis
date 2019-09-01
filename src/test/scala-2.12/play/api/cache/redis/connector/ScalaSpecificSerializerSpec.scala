package play.api.cache.redis.connector

import java.util.Date

import scala.reflect.ClassTag

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.cache.redis._

import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ScalaSpecificSerializerSpec extends Specification with Mockito {
  import SerializerImplicits._

  private val system = GuiceApplicationBuilder().build().actorSystem

  private implicit val serializer: AkkaSerializer = new AkkaSerializerImpl(system)

  "AkkaEncoder" should "encode" >> {

    "custom classes" in {
      SimpleObject("B", 3).encoded mustEqual """
          |rO0ABXNyAD9wbGF5LmFwaS5jYWNoZS5yZWRpcy5jb25uZWN0b3IuU2VyaWFsaXplckltcGxpY2l0c
          |yRTaW1wbGVPYmplY3TbTGkeqUDNdwIAAkkABXZhbHVlTAADa2V5dAASTGphdmEvbGFuZy9TdHJpbm
          |c7eHAAAAADdAABQg==
        """.stripMargin.removeAllWhitespaces
    }

    "list" in {
      List("A", "B", "C").encoded mustEqual """
          |rO0ABXNyADJzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS5MaXN0JFNlcmlhbGl6YXRpb25Qcm94
          |eQAAAAAAAAABAwAAeHB0AAFBdAABQnQAAUNzcgAsc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUu
          |TGlzdFNlcmlhbGl6ZUVuZCSKXGNb91MLbQIAAHhweA==
        """.stripMargin.removeAllWhitespaces
    }
  }

  "AkkaDecoder" should "decode" >> {

    "custom classes" in {
      """
        |rO0ABXNyAD9wbGF5LmFwaS5jYWNoZS5yZWRpcy5jb25uZWN0b3IuU2VyaWFsaXplckltcGxpY2l0c
        |yRTaW1wbGVPYmplY3TbTGkeqUDNdwIAAkkABXZhbHVlTAADa2V5dAASTGphdmEvbGFuZy9TdHJpbm
        |c7eHAAAAADdAABQg==
      """.stripMargin.removeAllWhitespaces.decoded[SimpleObject] mustEqual SimpleObject("B", 3)
    }

    "list" in {
      """
        |rO0ABXNyADJzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS5MaXN0JFNlcmlhbGl6YXRpb25Qcm94
        |eQAAAAAAAAABAwAAeHB0AAFBdAABQnQAAUNzcgAsc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUu
        |TGlzdFNlcmlhbGl6ZUVuZCSKXGNb91MLbQIAAHhweA==
      """.stripMargin.removeAllWhitespaces.decoded[List[String]] mustEqual List("A", "B", "C")
    }
  }
}
