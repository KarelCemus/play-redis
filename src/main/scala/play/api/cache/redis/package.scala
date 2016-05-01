package play.api.cache

/**
  * @author Karel Cemus
  */
package object redis extends AnyRef with Expiration {

  type SynchronousResult[ A ] = A
  type AsynchronousResult[ A ] = scala.concurrent.Future[ A ]

}
