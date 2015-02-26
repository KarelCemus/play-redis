package play.cache.api

import play.api.Plugin

trait CachePlugin extends Plugin {

  /** Implementation of the the Cache plugin provided by this plugin. */
  def api: CacheAPI
}
