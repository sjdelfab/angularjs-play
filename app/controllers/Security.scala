package controllers

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest

import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.mvc.Results
import security.AuthenticationEnv

/**
 * Security actions that should be used by all controllers that need to protect their actions.
 * Can be composed to fine-tune access control.
 */
trait Security { self: Controller =>  
      
  def silhouette: Silhouette[AuthenticationEnv]
  
  def SecuredAction = silhouette.SecuredAction
  def UnsecuredAction = silhouette.UnsecuredAction
  def UserAwareAction = silhouette.UserAwareAction
  
  def RestrictAsync[A](parser: BodyParser[A] = parse.anyContent)(roleNames: Array[String])(action: SecuredRequest[AuthenticationEnv, A] => Future[Result]): Action[A] = {
    RestrictionAsync[A](parser)(List(roleNames))(action)
  }
  
  def RestrictionAsync[A](parser: BodyParser[A] = parse.anyContent)(roleGroups: List[Array[String]])(action: SecuredRequest[AuthenticationEnv, A] => Future[Result]): Action[A] = {
    SecuredAction.async(parser) { implicit request =>
      val user = request.identity
      if (user.checkRoles(roleGroups)) {
           action(request)
      } else {
           Future.successful(Results.Forbidden)
      } 
    }
  }
  
}