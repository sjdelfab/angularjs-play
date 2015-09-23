package modules

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{ Configuration, Environment }
import services.UserSession
import services.UserService
import play.api.Logger
import play.api.Play
import services.PlayCacheUserSession
import services.UserServiceDatabase
import services.DevelopmentUserSession
  
class ApplicationModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  
  def configure() = {
      environment.mode match {
         case play.api.Mode.Dev => {
             Logger.info("Using dev cache service")
             bind(classOf[UserSession]).to(classOf[DevelopmentUserSession])
         }
         case play.api.Mode.Prod => {
             bind(classOf[UserSession]).to(classOf[PlayCacheUserSession])
         }
         case play.api.Mode.Test => {
             Logger.info("Using dev cache service")
             bind(classOf[UserSession]).to(classOf[DevelopmentUserSession])
         }
         case _ => {}
      }
      bind(classOf[UserService]).to(classOf[UserServiceDatabase])
  }
}