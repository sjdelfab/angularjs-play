import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector

import play.api.GlobalSettings
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter
import play.api.Logger

import services.UserSession
import services.PlayCacheUserSession
import services.DevelopmentUserSession
import services.UserService
import services.UserServiceDatabase

object Global extends WithFilters(new GzipFilter(shouldGzip =
  (request, response) => {
    val contentType = response.headers.get("Content-Type")
    contentType.exists(_.startsWith("text/html")) || request.path.endsWith("jsroutes.js")
  }
)) with GlobalSettings {
  
  
  override def onStart(app: play.api.Application) {
      super.onStart(app)
      Logger.info("Starting application")     
  }
  
}