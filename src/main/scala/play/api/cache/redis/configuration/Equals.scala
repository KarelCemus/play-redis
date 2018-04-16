package play.api.cache.redis.configuration

/**
  * @author Karel Cemus
  */
private[ configuration ] object Equals {

  // $COVERAGE-OFF$
  @inline
  def check[ T ]( a: T, b: T )( property: ( T => Any )* ): Boolean = {
    property.forall( property => property( a ) == property( b ) )
  }
  // $COVERAGE-ON$
}
