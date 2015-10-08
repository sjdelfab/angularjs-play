package controllers

import models._
import java.util.UUID
import javax.inject.{Singleton, Inject}
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.cache._
import play.api.libs.Crypto
import scalaext.OptionExt._
import services.UserSession
import services.UserService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.InvalidLoginAttempt
import services.AccountLocked
import services.SuccessfulLogin
import utils.PasswordCrypt
import security.InternalUser

@Singleton
class Application @Inject() (userSession: UserSession, userService: UserService) extends Controller with Security {

  def getUserSession(): UserSession = {
    userSession
  }
  
  def index = Action {
    Ok(views.html.index())
  }

  /**
   * Retrieves all routes via reflection.
   * http://stackoverflow.com/questions/12012703/less-verbose-way-of-generating-play-2s-javascript-router
   * TODO If you have controllers in multiple packages, you need to add each package here.
   */

  val routeCache = {
    val jsRoutesClass = classOf[routes.javascript]
    val controllers = jsRoutesClass.getFields.map(_.get(null))
    controllers.flatMap { controller =>
      controller.getClass.getDeclaredMethods.filter(_.invoke(controller).isInstanceOf[play.api.routing.JavaScriptReverseRoute]).map { action =>
           action.invoke(controller).asInstanceOf[play.api.routing.JavaScriptReverseRoute]
      }
    }
  }

  /**
   * Returns the JavaScript router that the client can use for "type-safe" routes.
   * Uses browser caching; set duration (in seconds) according to your release cycle.
   * @param varName The name of the global variable, defaults to `jsRoutes`
   */
  def jsRoutes(varName: String = "jsRoutes") = Cached(_ => "jsRoutes", duration = 86400) {
    Action { implicit request =>
      Ok(play.api.routing.JavaScriptReverseRouter(varName)(routeCache: _*)).as(JAVASCRIPT)
    }
  }

  /** Used for obtaining the email and password from the HTTP login request */
  case class LoginCredentials(email: String, password: String)

  /** JSON reader for [[LoginCredentials]]. */
  implicit val LoginCredentialsFromJson = (
      (__ \ "email").read[String](minLength[String](5)) ~
      (__ \ "password").read[String](minLength[String](2))
    )((email, password) => LoginCredentials(email, password))

  /**
   * Log-in a user. Expects the credentials in the body in JSON format.
   *
   * Set the cookie [[AUTH_TOKEN_COOKIE_KEY]] to have AngularJS set the X-XSRF-TOKEN in the HTTP
   * header.
   *
   * @return The token needed for subsequent requests
   */
  def login() = Action.async { request =>           
	    request.body.asJson.map { json =>
	      json.validate[LoginCredentials].fold(
		      errors => {
		        scala.concurrent.Future {
  		        Logger.info("Invalid JSON request: " + JsError.toJson(errors))
  		        BadRequest("Invalid request")
		        }
		      },
		      credentials => {
		        PasswordCrypt.encrypt(credentials.password) ifSome { encryptedPassword =>
		            performLogin(credentials.email,encryptedPassword)
		        } otherwise {
		           scala.concurrent.Future {
                 Logger.info("Attempting login for user '" + credentials.email + "'. Invalid username/password")
  		           BadRequest(Json.obj("login_error_status" -> "INVALID_USERNAME_PASSWORD"))
		           }
		        }
		      }
	    )
	    }.getOrElse {
	       scala.concurrent.Future {
	          BadRequest("Expecting Json data")
	       }
	    }
     
  }

  private def performLogin(email: String, encryptedPassword: String) = {
    val actions = for {
        loginResult <- userService.authenticate(email, encryptedPassword) 
        result <- {
            loginResult match {
              case SuccessfulLogin(user) => {
                val token = UUID.randomUUID.toString
                val encryptedToken = Crypto.encryptAES(token)
                userService.getRoles(user.id.get) map { userRoles => 
                  userSession.register(token, new InternalUser(user.email,user.id.get,Some(userRoles)))
                  Ok(Json.obj("token" -> encryptedToken, "user" -> Json.toJson(user)))
                    .withCookies(Cookie(AUTH_TOKEN_COOKIE_KEY, encryptedToken, None, httpOnly = false))
                    .withNewSession
                }
              }
              case InvalidLoginAttempt() => scala.concurrent.Future{BadRequest(Json.obj("login_error_status" -> "INVALID_USERNAME_PASSWORD"))}
              case AccountLocked() => scala.concurrent.Future{BadRequest(Json.obj("login_error_status" -> "ACCOUNT_LOCKED"))}
            }
        }  
    } yield result
    actions
  }
  
  /**
   * Log-out a user. Invalidates the authentication token.
   *
   * Discard the cookie [[AUTH_TOKEN_COOKIE_KEY]] to have AngularJS no longer set the
   * X-XSRF-TOKEN in HTTP header.
   */
  def logout() = ValidUserAction(parse.empty) { token => userId => implicit request =>
    userSession.deregister(token)
    Ok.discardingCookies(DiscardingCookie(name = AUTH_TOKEN_COOKIE_KEY))
  }
}