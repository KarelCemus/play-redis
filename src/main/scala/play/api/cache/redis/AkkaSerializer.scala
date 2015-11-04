package play.api.cache.redis

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util._

import play.api.libs.concurrent.Akka
import play.api.{Application, Logger}

import akka.serialization._

/**
 * Provides a encode and decode methods to serialize objects into strings.
 *
 * @author Karel Cemus
 */
trait AkkaSerializer {

  /** logger handler */
  protected def log: Logger

  /** current application */
  protected implicit def application: Application

  /** in production mode serializer is just one, in development mode it is reloaded */
  private val serializer: Serialization = SerializationExtension( Akka.system )

  /** encode given object into string */
  private def encode[ T ]( value: T ): Try[ String ] = value match {
    // null is special case
    case null => throw new UnsupportedOperationException( "Null is not supported by the redis cache connector." )
    // AnyVal is not supported by default, have to be implemented manually; also basic types are processed as primitives
    case v if v.getClass.isPrimitive || Primitives.primitives.contains( v.getClass ) => Success( v.toString )
    // AnyRef is supported by Akka serializers, but it does not consider classTag, thus it is done manually
    case anyRef: AnyRef =>
      // serialize the object with the respect to the current class
      Try( serializer.findSerializerFor( anyRef ).toBinary( anyRef ) ).map( toBase64 )
    // if none of the cases above matches, throw an exception
    case _ => throw new UnsupportedOperationException( s"Type ${ value.getClass } is not supported by redis cache connector." )
  }

  /** encode given value and handle error if occurred */
  protected def encode[ T ]( key: String, value: T ): Try[ String ] = encode( value ) recoverWith {
    case ex => log.error( s"Serialization for key '$key' failed. Cause: '$ex'." ); Failure( ex )
  }

  /** produces BASE64 encoded string from an array of bytes */
  private def toBase64( bytes: Array[ Byte ] ): String = new sun.misc.BASE64Encoder( ).encode( bytes )

  /** decode given value from string to object */
  private def decode[ T ]( value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] = ( value match {
    // AnyVal is not supported by default, have to be implemented manually
    case "" => Success( null )
    case string if classTag == ClassTag( classOf[ String ] ) => Try( string )
    case boolean if classTag == ClassTag.Boolean => Try( boolean.toBoolean )
    case byte if classTag == ClassTag.Byte => Try( byte.toByte )
    case char if classTag == ClassTag.Char => Try( char.charAt( 0 ) )
    case short if classTag == ClassTag.Short => Try( short.toShort )
    case int if classTag == ClassTag.Int => Try( int.toInt )
    case long if classTag == ClassTag.Long => Try( long.toLong )
    case float if classTag == ClassTag.Float => Try( float.toFloat )
    case double if classTag == ClassTag.Double => Try( double.toDouble )
    // AnyRef is supported by Akka serializers
    case anyRef => serializer.deserialize( toBinary( anyRef ), classTag.runtimeClass )
  } ).asInstanceOf[ Try[ T ] ]

  /** decode given value and handle error if occurred */
  protected def decode[ T ]( key: String, value: String )( implicit classTag: ClassTag[ T ] ): Try[ T ] = decode( value ) recoverWith {
    case ex => log.error( s"Deserialization for key '$key' failed. Cause: '$ex'." ); Failure( ex )
  }

  /** consumes BASE64 string and returns array of bytes */
  private def toBinary( base64: String ): Array[ Byte ] = new sun.misc.BASE64Decoder( ).decodeBuffer( base64 )
}


private[ redis ] object Primitives {

  /** primitive types with simplified encoding */
  val primitives = Seq(
    classOf[ Boolean ], classOf[ java.lang.Boolean ],
    classOf[ Byte ], classOf[ java.lang.Byte ],
    classOf[ Char ], classOf[ java.lang.Character ],
    classOf[ Short ], classOf[ java.lang.Short ],
    classOf[ Int ], classOf[ java.lang.Integer ],
    classOf[ Long ], classOf[ java.lang.Long ],
    classOf[ Float ], classOf[ java.lang.Float ],
    classOf[ Double ], classOf[ java.lang.Double ],
    classOf[ String ]
  )
}
