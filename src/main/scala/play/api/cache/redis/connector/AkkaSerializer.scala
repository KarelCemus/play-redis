package play.api.cache.redis.connector

import java.util.Base64

import javax.inject._
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util._

import play.api.cache.redis._
import akka.actor.ActorSystem
import akka.serialization._

/**
  * Provides a encode and decode methods to serialize objects into strings
  * and vise versa.
  */
trait AkkaSerializer {

  /**
    * Method accepts a value to be serialized into the string.
    * Based on the implementation, it returns a string representing the value or
    * provides an exception, if the computation fails.
    *
    * @param value value to be serialized
    * @return serialized string or exception
    */
  def encode(value: Any): Try[String]

  /**
    * Method accepts a valid serialized string and based on the accepted class it deserializes it.
    * If the expected class does not match expectations, deserialization fails with an exception.
    * Also, if the string is not valid representation, it also fails.
    *
    * @param value valid serialized entity
    * @tparam T expected class
    * @return deserialized object or exception
    */
  def decode[T: ClassTag](value: String): Try[T]
}

/**
  * Akka encoder provides implementation of serialization using Akka serializer.
  * The implementation considers all primitives, nulls, and refs. This enables
  * us to use Akka settings to modify serializer mapping and use different serializers
  * for different objects.
  */
private[connector] class AkkaEncoder(serializer: Serialization) {

  /** Unsafe method encoding the given value into a string */
  def encode(value: Any): String = value match {
    // null is special case
    case null                                => unsupported("Null is not supported by the redis cache connector.")
    // AnyVal is not supported by default, have to be implemented manually; also basic types are processed as primitives
    case primitive if isPrimitive(primitive) => primitive.toString
    // AnyRef is supported by Akka serializers, but it does not consider classTag, thus it is done manually
    case anyRef: AnyRef                      => anyRefToString(anyRef)
    // $COVERAGE-OFF$
    // if no of the cases above matches, throw an exception
    case _                                   => unsupported(s"Type ${value.getClass} is not supported by redis cache connector.")
    // $COVERAGE-ON$
  }

  /** determines whether the given value is a primitive */
  protected def isPrimitive(candidate: Any): Boolean =
    candidate.getClass.isPrimitive || Primitives.primitives.contains(candidate.getClass)

  /** unsafe method converting AnyRef into bytes */
  protected def anyRefToBinary(anyRef: AnyRef): Array[Byte] =
    serializer.findSerializerFor(anyRef).toBinary(anyRef)

  /** Produces BASE64 encoded string from an array of bytes */
  protected def binaryToString(bytes: Array[Byte]): String =
    Base64.getEncoder.encodeToString(bytes)

  /** unsafe method converting AnyRef into BASE64 string */
  protected def anyRefToString(value: AnyRef): String =
    (anyRefToBinary _ andThen binaryToString)(value)
}

/**
  * Akka decoder provides implementation of deserialization using Akka serializer.
  * The implementation considers all primitives, nulls, and refs. This enables
  * us to use Akka settings to modify serializer mapping and use different serializers
  * for different objects.
  */
private[connector] class AkkaDecoder(serializer: Serialization) {

  import scala.reflect.{ClassTag => Scala}

  import play.api.cache.redis.connector.{JavaClassTag => Java}

  private val Nothing = ClassTag(classOf[Nothing])

  /** unsafe method decoding a string into an object. It directly throws exceptions */
  def decode[T](value: String)(implicit classTag: ClassTag[T]): T =
    untypedDecode[T](value).asInstanceOf[T]

  /** unsafe method decoding a string into an object. It directly throws exceptions. It does not perform type cast */
  protected def untypedDecode[T](value: String)(implicit tag: ClassTag[T]): Any = value match {
    // AnyVal is not supported by default, have to be implemented manually
    case "" => null
    case _ if tag == Nothing => throw new IllegalArgumentException("Type Nothing is not supported. You have probably forgot to specify expected data type.")
    case string if tag == Java.String => string
    case boolean if tag == Java.Boolean || tag == Scala.Boolean => boolean.toBoolean
    case byte if tag == Java.Byte || tag == Scala.Byte => byte.toByte
    case char if tag == Java.Char || tag == Scala.Char => char.charAt(0)
    case short if tag == Java.Short || tag == Scala.Short => short.toShort
    case int if tag == Java.Int || tag == Scala.Int => int.toInt
    case long if tag == Java.Long || tag == Scala.Long => long.toLong
    case float if tag == Java.Float || tag == Scala.Float => float.toFloat
    case double if tag == Java.Double || tag == Scala.Double => double.toDouble
    case anyRef => stringToAnyRef[T](anyRef)
  }

  /** consumes BASE64 string and returns array of bytes */
  protected def stringToBinary(base64: String): Array[Byte] =
    Base64.getDecoder.decode(base64)

  /** deserializes the binary stream into the object */
  protected def binaryToAnyRef[T](binary: Array[Byte])(implicit classTag: ClassTag[T]): AnyRef =
    serializer.deserialize(binary, classTag.runtimeClass.asInstanceOf[Class[_ <: AnyRef]]).get

  /** converts BASE64 string directly into the object */
  protected def stringToAnyRef[T: ClassTag](base64: String): AnyRef =
    (stringToBinary _ andThen binaryToAnyRef[T])(base64)
}

@Singleton
private[connector] class AkkaSerializerImpl @Inject() (system: ActorSystem) extends AkkaSerializer {

  /**
    * serializer dispatcher used to serialize the objects into bytes;
    * the instance is retrieved from Akka based on its configuration
    */
  protected val serializer: Serialization = SerializationExtension(system)

  /** value serializer based on Akka serialization */
  private val encoder = new AkkaEncoder(serializer)

  /** value decoder based on Akka serialization */
  private val decoder = new AkkaDecoder(serializer)

  /**
    * Method accepts a value to be serialized into the string.
    * Based on the implementation, it returns a string representing the value or
    * provides an exception, if the computation fails.
    *
    * @param value value to be serialized
    * @return serialized string or exception
    */
  override def encode(value: Any): Try[String] =
    Try(encoder.encode(value))

  /**
    * Method accepts a valid serialized string and based on the accepted class it deserializes it.
    * If the expected class does not match expectations, deserialization fails with an exception.
    * Also, if the string is not valid representation, it also fails.
    *
    * @param value valid serialized entity
    * @tparam T expected class
    * @return deserialized object or exception
    */
  override def decode[T: ClassTag](value: String): Try[T] =
    Try(decoder.decode[T](value))
}

/**
  * Registry of known Scala and Java primitives
  */
private[connector] object Primitives {

  /** primitive types with simplified encoding */
  val primitives = Seq(
    classOf[Boolean], classOf[java.lang.Boolean],
    classOf[Byte], classOf[java.lang.Byte],
    classOf[Char], classOf[java.lang.Character],
    classOf[Short], classOf[java.lang.Short],
    classOf[Int], classOf[java.lang.Integer],
    classOf[Long], classOf[java.lang.Long],
    classOf[Float], classOf[java.lang.Float],
    classOf[Double], classOf[java.lang.Double],
    classOf[String]
  )
}

/**
  * Registry of class tags for Java primitives
  */
private[connector] object JavaClassTag {

  val Byte = ClassTag(classOf[java.lang.Byte])
  val Short = ClassTag(classOf[java.lang.Short])
  val Char = ClassTag(classOf[java.lang.Character])
  val Int = ClassTag(classOf[java.lang.Integer])
  val Long = ClassTag(classOf[java.lang.Long])
  val Float = ClassTag(classOf[java.lang.Float])
  val Double = ClassTag(classOf[java.lang.Double])
  val Boolean = ClassTag(classOf[java.lang.Boolean])
  val String = ClassTag(classOf[String])
}

class AkkaSerializerProvider @Inject() (implicit system: ActorSystem) extends Provider[AkkaSerializer] {
  lazy val get = new AkkaSerializerImpl(system)
}
