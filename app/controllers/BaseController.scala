package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future
import services.UserSession
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data.validation.ValidationError
import security.InternalUser

trait BaseController { self: Controller { def getUserSession(): UserSession } =>
  
  protected def UserBadRequest(operation: String, sessionUser: InternalUser, errors: Seq[(JsPath, Seq[ValidationError])]): Future[Result] = {    
      Future{
        Logger.info(s"User: ${sessionUser.userEmail}. $operation: Invalid JSON request: " + JsError.toJson(errors))
        BadRequest("Invalid request")
      }
  }
  
  protected def InvalidUserRequest(operation: String, sessionUser: InternalUser): Future[Result] = {    
      Future{
        Logger.info(s"User: ${sessionUser.userEmail}. $operation: Invalid request.")
        BadRequest("Invalid request")
      }
  }
  
}