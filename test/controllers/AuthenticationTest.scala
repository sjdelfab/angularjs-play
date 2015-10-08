package controllers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Result
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.running
import services.AccountLocked
import services.InvalidLoginAttempt
import services.UserService
import services.UserSession
import utils.PasswordCrypt

@RunWith(classOf[JUnitRunner])
class AuthenticationTest extends Specification with Mockito {


  "Authentication" should {
    
      "Return BadRequest" in {
         running(FakeApplication(additionalConfiguration = Map("dbplugin" -> "disabled"))) {
	         val userSession = mock[UserSession]
		       val userService = mock[UserService]
		       val application = new Application(userSession,userService)
	         
	         val result: Future[Result] = application.login.apply(FakeRequest())
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
	         contentAsString(result) must equalTo("Expecting Json data")
	         status.get must equalTo(400)
         }
      }
    
      "Invalid JSON request returns BadRequest" in {
        running(FakeApplication(additionalConfiguration = Map("dbplugin" -> "disabled"))) {
	         val userSession = mock[UserSession]
	         val userService = mock[UserService]
	         val application = new Application(userSession,userService)
	         
	         val jsonRequest = Json.obj("crap" -> "")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = application.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
	         contentAsString(result) must equalTo("Invalid request")
	         status.get must equalTo(400)
         }
      }
      
      "Empty email" in {
        running(FakeApplication(additionalConfiguration = Map("dbplugin" -> "disabled"))) {
             val userSession = mock[UserSession]
	         val userService = mock[UserService]
	         val application = new Application(userSession,userService)
	         
	         val jsonRequest = Json.obj("email" -> "","password" -> "mypassword")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = application.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
             contentAsString(result) must equalTo("Invalid request")
	         status.get must equalTo(400)
        }
      }
      
      "Empty password" in {
        running(FakeApplication(additionalConfiguration = Map("dbplugin" -> "disabled"))) {
             val userSession = mock[UserSession]
	         val userService = mock[UserService]
	         val application = new Application(userSession,userService)
	         
	         val jsonRequest = Json.obj("email" -> "simon@acme.com","password" -> "")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = application.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
             contentAsString(result) must equalTo("Invalid request")
	         status.get must equalTo(400)
        }
      }
      
      "Invalid user name" in {
        running(FakeApplication(additionalConfiguration = Map("dbplugin" -> "disabled"))) {
           val userSession = mock[UserSession]
	         val userService = mock[UserService]
	         val application = new Application(userSession,userService)
           val encryptedPassword = PasswordCrypt.encrypt("mypassword").get
	         when(userService.authenticate("simon@acme.com",encryptedPassword)).thenReturn(Future{InvalidLoginAttempt()})
	         val jsonRequest = Json.obj("email" -> "simon@acme.com","password" -> "mypassword")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = application.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
             contentAsString(result) must equalTo("{\"login_error_status\":\"INVALID_USERNAME_PASSWORD\"}")
	         status.get must equalTo(400)
        }
      }
      
      "Account locked" in {
        running(FakeApplication(additionalConfiguration = Map("dbplugin" -> "disabled"))) {
             val userSession = mock[UserSession]
             val userService = mock[UserService]
             val application = new Application(userSession,userService)
             val encryptedPassword = PasswordCrypt.encrypt("mypassword").get
             when(userService.authenticate("simon@acme.com",encryptedPassword)).thenReturn(Future{AccountLocked()})
             val jsonRequest = Json.obj("email" -> "simon@acme.com","password" -> "mypassword")
             val request = FakeRequest().withJsonBody(jsonRequest)
             
             val result: Future[Result] = application.login().apply(request)
             Await.result(result, DurationLong(5l) seconds)
             val status = result.value map { value =>            
                value.get.header.status
             }
             contentAsString(result) must equalTo("{\"login_error_status\":\"ACCOUNT_LOCKED\"}")
             status.get must equalTo(400)
        }
      }
  }
}