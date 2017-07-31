package play.api.cache.redis.util

import com.typesafe.config.Config

object config {

  implicit class ConfigExt(conf: Config) {

    def getStringOpt( path: String ): Option[ String ] = {
      if ( conf.hasPathOrNull( path ) ) {
        if ( conf.getIsNull( path ) ) {
          None
        } else {
          Some( conf.getString( path ) )
        }
      } else {
        None
      }
    }

    def getIntOpt( path: String ): Option[ Int ] = {
      if ( conf.hasPathOrNull( path ) ) {
        if ( conf.getIsNull( path ) ) {
          None
        } else {
          Some( conf.getInt( path ) )
        }
      } else {
        None
      }
    }
  }

}
