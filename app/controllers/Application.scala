package controllers

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.LoginEvent
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.Logger
import play.api.cache.Cached
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.applicative
import play.api.libs.json.Reads.functorReads
import play.api.libs.json.Reads.minLength
import play.api.libs.json.__
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.Cookie
import play.api.mvc.DiscardingCookie
import play.api.mvc.Request
import play.api.mvc.Result
import scalaz._
import security.AuthenticationEnv
import security.UserIdentity
import security.UserIdentityService
import services.AccountLocked
import services.SuccessfulLogin
import services.UserService
import com.mohiva.play.silhouette.api.LogoutEvent
import com.mohiva.play.silhouette.api.util.PasswordHasher

@Singleton
class Application @Inject() (userService: UserService, 
                             configuration: Configuration,
                             cached: Cached,
                             indirectReferenceMapper: IndirectReferenceMapper,
                             val silhouette: Silhouette[AuthenticationEnv],
                             userIdentityService: UserIdentityService,
                             authInfoRepository: AuthInfoRepository,
                             credentialsProvider: CredentialsProvider,
                             passwordHasher: PasswordHasher) extends Controller with Security with BaseController {
  
  def getUserService(): UserService = userService
  
  def getConfiguration(): Configuration = configuration
  
  def getIndirectReferenceMapper(): IndirectReferenceMapper = indirectReferenceMapper 
  
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
  def jsRoutes(varName: String = "jsRoutes") = cached(_ => "jsRoutes", duration = 86400) {
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
  
  def login() = UnsecuredAction.async { request =>           
	    request.body.asJson.map { json =>
	      json.validate[LoginCredentials].fold(
		      errors => {
		        Future {
  		        Logger.info("Invalid JSON request: " + JsError.toJson(errors))
  		        BadRequest("Invalid request")
		        }
		      },
		      credentials => {
		        attemptAuthentication(credentials,request)
		      }
	    )
	    }.getOrElse {
	       Future { BadRequest("Expecting Json data") }
	    }
     
  }

  private def LoginErrorStatusResult(email: String, status: String): Future[Result] = {
     Future {
         Logger.info(s"Attempting login for user ' $email ': $status")
  		   BadRequest(Json.obj("login_error_status" -> status))
		 }
  }

  private def attemptAuthentication(credentials: LoginCredentials, request: Request[AnyContent]): Future[Result] = {
      for {
        authAttempt <- tryAuthenticate(credentials)
        result <- {
          authAttempt match {
		         case \/-(loginInfo: LoginInfo) => handleLogin(loginInfo,request)
		         case -\/(failed: FailedLogin) => LoginErrorStatusResult(credentials.email,"INVALID_USERNAME_PASSWORD")
		      }
        }
      } yield result
  }
  
  case class FailedLogin()
  
  private def tryAuthenticate(credentials: LoginCredentials): Future[FailedLogin \/ LoginInfo] = {
     credentialsProvider.authenticate(Credentials(credentials.email, credentials.password)).map { loginInfo =>
       \/-(loginInfo)
     }.recover {
       case e: ProviderException => {
         // TODO pass parameter with more contextual info
         -\/(FailedLogin())
       }
     }
  }
  
  private def handleLogin(loginInfo: LoginInfo, request: Request[AnyContent]): Future[Result] = {
     for {
       user <- userIdentityService.retrieve(loginInfo)
		   authenticator <- silhouette.env.authenticatorService.create(loginInfo)(request)
		   result <- user.fold(LoginErrorStatusResult(loginInfo.providerKey,"INVALID_USERNAME_PASSWORD")) { identity =>
		       validateIdentity(identity) match {
		         case SuccessfulLogin(user) => {
		            silhouette.env.eventBus.publish(LoginEvent(identity, request))
                silhouette.env.authenticatorService.init(authenticator)(request).map { token =>
                  Ok(Json.obj("token" -> token, "user" -> Json.toJson(user)))
                    .withNewSession 
                }
		         }
		         case AccountLocked() => LoginErrorStatusResult(identity.user.email,"ACCOUNT_LOCKED")
		       }
		     }
     } yield result
  }
  
  private def validateIdentity(identity: UserIdentity) = {
    if (identity.user.isAccountLocked(configuration.getInt(controllers.MAX_FAILED_LOGIN_ATTEMPTS).getOrElse(3))) {
       AccountLocked()   
    } else {
       SuccessfulLogin(identity.user)
    }
  }
  
  def logout() = SecuredAction.async { implicit request =>
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, Ok)
  }
  
  def changeMyPassword(currentPassword: String, newPassword: String) = SecuredAction.async(parse.empty) { implicit request =>
    val credentials = LoginCredentials(request.identity.user.email,currentPassword)
    for {
        authAttempt <- tryAuthenticate(credentials)
        result <- {
          authAttempt match {
		         case \/-(loginInfo: LoginInfo) => handlePasswordChange(loginInfo,newPassword)
		         case -\/(failed: FailedLogin) => {
		           Future.successful({
  		           Logger.info(s"User: ${request.identity.user.email}. Change Own Password: Invalid current password")
                 OkNoCache(Json.obj("status" -> "INVALID_CURRENT_PASSWORD"))
		           })
		         }
		      }
        }
      } yield result
  }

  private def handlePasswordChange(loginInfo: LoginInfo, newPassword: String): Future[Result] = {
     for {
       user <- userIdentityService.retrieve(loginInfo)
		   result <- user.fold(LoginErrorStatusResult(loginInfo.providerKey,"INVALID_USERNAME_PASSWORD")) { identity =>
		       validateIdentity(identity) match {
		         case SuccessfulLogin(user) => {
		           Future { changePassword(identity,newPassword) }
		         }
		         case AccountLocked() => Future {
		           Logger.info(s"User: ${identity.user.email}. Change Own Password: Account locked")
		           OkNoCache(Json.obj("status" -> "ACCOUNT_LOCKED"))
		         }
		       }
		     }
     } yield result
  }
  
  private def changePassword(userIdentity: UserIdentity, newPassword: String): Result = {
    if (isPasswordStrongEnough(newPassword)) {
        val passwordInfo = passwordHasher.hash(newPassword)
        userService.changeUserPassword(userIdentity.user.id.get, passwordInfo.password)
        Logger.info(s"User: ${userIdentity.user.email}. Change Own Password.")
        OkNoCache(Json.obj("status" -> "OK"))
    } else {
      Logger.info(s"User: ${userIdentity.user.email}. Change Own Password: Password not strong enough")
      val msg = configuration.getString(PASSWORD_POLICY_MESSAGE).get
      OkNoCache(Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> msg))
    }
  }
  
}