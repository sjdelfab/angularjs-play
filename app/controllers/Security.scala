package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.cache._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.Crypto
import services.UserSession
import scala.concurrent.Future
import security.InternalUser


/**
 * Security actions that should be used by all controllers that need to protect their actions.
 * Can be composed to fine-tune access control.
 */
trait Security extends SecurityCookieTokens { self: Controller { def getUserSession(): UserSession } =>

  implicit val app: play.api.Application = play.api.Play.current
  
  /**
    * Checks that the token is:
    * - present in the cookie header of the request,
    * - either in the header or in the query string,
    * - matches a token already stored in the play cache
    */
  def ValidUserAction[A](parser: BodyParser[A] = parse.anyContent)(action: String => InternalUser => Request[A] => Result): Action[A] =
    Action.async(parser) { implicit request =>
      scala.concurrent.Future {
          request.cookies.get(AUTH_TOKEN_COOKIE_KEY).fold {
            Unauthorized(Json.obj("message" -> "Invalid XSRF Token cookie"))
          } { xsrfTokenCookie =>
            val maybeToken = request.headers.get(AUTH_TOKEN_HEADER).orElse(request.getQueryString(AUTH_TOKEN_URL_KEY))
            maybeToken flatMap { token =>
              val unencryptedToken = Crypto.decryptAES(token)
              getUserSession().lookup(unencryptedToken) map { sessionUser =>
                if (xsrfTokenCookie.value.equals(token)) {
                  action(token)(sessionUser)(request)
                } else {
                  Unauthorized(Json.obj("message" -> "Invalid Token"))
                }
              }
            } getOrElse Unauthorized(Json.obj("message" -> "No Token"))
          }
      }
    }
  
  def ValidUserAsyncAction[A](parser: BodyParser[A] = parse.anyContent)(action: String => InternalUser => Request[A] => Future[Result]): Action[A] =
    Action.async(parser) { implicit request =>
          request.cookies.get(AUTH_TOKEN_COOKIE_KEY).fold {
              scala.concurrent.Future { Unauthorized(Json.obj("message" -> "Invalid XSRF Token cookie")) }
          } { xsrfTokenCookie =>
            val maybeToken = request.headers.get(AUTH_TOKEN_HEADER).orElse(request.getQueryString(AUTH_TOKEN_URL_KEY))
            maybeToken flatMap { token =>
              val unencryptedToken = Crypto.decryptAES(token)
              getUserSession().lookup(unencryptedToken) map { sessionUser =>
                if (xsrfTokenCookie.value.equals(token)) {
                  action(token)(sessionUser)(request)
                } else {
                  scala.concurrent.Future { Unauthorized(Json.obj("message" -> "Invalid Token")) }
                }
              }
            } getOrElse {
              scala.concurrent.Future {
                  Unauthorized(Json.obj("message" -> "No Token"))
              }
            }
          }
    }  

  def Restrict[A](parser: BodyParser[A] = parse.anyContent)(roleNames: Array[String])(action: String => InternalUser => Request[A] => Result): Action[A] = {
    Restriction[A](parser)(List(roleNames))(action)
  }

  
  def Restriction[A](parser: BodyParser[A] = parse.anyContent)(roleGroups: List[Array[String]])(action: String => InternalUser => Request[A] => Result): Action[A] = {
    ValidUserAction[A](parser) { token => sessionUser => implicit request =>
      if (sessionUser.checkRoles(roleGroups)) {
           action(token)(sessionUser)(request)
      } else {
           Results.Forbidden
      } 
    }
  }
  
  def RestrictAsync[A](parser: BodyParser[A] = parse.anyContent)(roleNames: Array[String])(action: String => InternalUser => Request[A] => Future[Result]): Action[A] = {
    RestrictionAsync[A](parser)(List(roleNames))(action)
  }
  
  def RestrictionAsync[A](parser: BodyParser[A] = parse.anyContent)(roleGroups: List[Array[String]])(action: String => InternalUser => Request[A] => Future[Result]): Action[A] = {
    ValidUserAsyncAction[A](parser) { token => sessionUser => implicit request =>
      if (sessionUser.checkRoles(roleGroups)) {
           action(token)(sessionUser)(request)
      } else {
        scala.concurrent.Future {
           Results.Forbidden
        }
      } 
    }
  }
  
}

trait SecurityCookieTokens {
  
  final val AUTH_TOKEN_HEADER = "X-XSRF-TOKEN"
  final val AUTH_TOKEN_COOKIE_KEY = "XSRF-TOKEN"
  final val AUTH_TOKEN_URL_KEY = "URL-XSRF-TOKEN"

}