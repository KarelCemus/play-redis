package play.api.cache.redis.test

object Helpers {

  object configuration {
    import com.typesafe.config.ConfigFactory
    import play.api.Configuration

    def default: Configuration = {
      Configuration(ConfigFactory.load())
    }

    def fromHocon(hocon: String): Configuration = {
      val reference = ConfigFactory.load()
      val local = ConfigFactory.parseString(hocon.stripMargin)
      Configuration(local.withFallback(reference))
    }
  }

  object probe {

    val orElse: OrElseProbe.type = OrElseProbe
  }
}
