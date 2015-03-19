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
  
  var injector: Injector = null
  
  override def onStart(app: play.api.Application) {
      super.onStart(app)
      Logger.info("Starting Syrup")
      injector = Guice.createInjector(new AbstractModule {
         protected def configure() {
            import play.api.Play
            Play.current.mode match {
              case play.api.Mode.Dev => {
                Logger.info("Using dev cache service")
                bind(classOf[UserSession]).to(classOf[DevelopmentUserSession])
              }
              case play.api.Mode.Prod => {
                bind(classOf[UserSession]).to(classOf[PlayCacheUserSession])
              }
              case _ => {}
            }
            bind(classOf[UserService]).to(classOf[UserServiceDatabase])
         }
      })
  }
  
  /**
   * Controllers must be resolved through the application context. There is a special method of GlobalSettings
   * that we can override to resolve a given controller. This resolution is required by the Play router.
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
}