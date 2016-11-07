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
import play.api._
import play.api.inject.bind
import play.api.cache._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Result
import play.api.test._
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.running
import services.AccountLocked
import services.InvalidLoginAttempt
import services.UserService
import utils.PasswordCrypt
import modules.ApplicationModule
import play.api.inject.guice.GuiceApplicationBuilder
import models.User
import com.mohiva.play.silhouette.api.util.CacheLayer
import models.ApplicationRoleMembership

@RunWith(classOf[JUnitRunner])
class AuthenticationTest extends PlaySpecification with Mockito with AbstractControllerTest {

  "Authentication" should {
    
       val userService = mock[UserService]
       val cache = mock[CacheLayer]
       val application = new GuiceApplicationBuilder()
           .overrides(bind[UserService].toInstance(userService))
           .overrides(bind[CacheLayer].toInstance(cache))
           .build
    
      "Return BadRequest" in new WithApplication(application) {
           val appController = app.injector.instanceOf[Application]

           val result: Future[Result] = appController.login.apply(FakeRequest())
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
	         contentAsString(result) must equalTo("Expecting Json data")
	         status.get must equalTo(400)
      }
    
      "Invalid JSON request returns BadRequest" in new WithApplication(application) {
           val appController = app.injector.instanceOf[Application]
	         val jsonRequest = Json.obj("crap" -> "")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = appController.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
	         contentAsString(result) must equalTo("Invalid request")
	         status.get must equalTo(400)
      }
      
      "Empty email" in new WithApplication(application) {
           val appController = app.injector.instanceOf[Application]
	         val jsonRequest = Json.obj("email" -> "","password" -> "mypassword")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = appController.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
           contentAsString(result) must equalTo("Invalid request")
	         status.get must equalTo(400)
      }
      
      "Empty password" in new WithApplication(application) {
           val appController = app.injector.instanceOf[Application]
	         
	         val jsonRequest = Json.obj("email" -> "simon@acme.com","password" -> "")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = appController.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
             contentAsString(result) must equalTo("Invalid request")
	         status.get must equalTo(400)
      }
      
      "Invalid user name" in new WithApplication(application) {
	         val appController = app.injector.instanceOf[Application]
           
	         when(userService.findByEmail("simon@acme.com")).thenReturn(Future{None})
	         val jsonRequest = Json.obj("email" -> "simon@acme.com","password" -> "mypassword")
	         val request = FakeRequest().withJsonBody(jsonRequest)
	         
	         val result: Future[Result] = appController.login().apply(request)
	         Await.result(result, DurationLong(5l) seconds)
	         val status = result.value map { value =>            
	            value.get.header.status
	         }
             contentAsString(result) must equalTo("{\"login_error_status\":\"INVALID_USERNAME_PASSWORD\"}")
	         status.get must equalTo(400)
      }
      
      "Account locked" in new WithApplication(application) {
           val appController = app.injector.instanceOf[Application]  
           
           val accountLockedUser = User(Some(1l),"simon@acme.com",Some(hasher.hash("mypassword").password),"Simon",10,true)
           when(userService.findByEmail("simon@acme.com")).thenReturn(Future{Some(accountLockedUser)})
           when(userService.getRoles(1l)).thenReturn(Future{List[ApplicationRoleMembership]()})
           val jsonRequest = Json.obj("email" -> "simon@acme.com","password" -> "mypassword")
           val request = FakeRequest().withJsonBody(jsonRequest)
             
           val result: Future[Result] = appController.login().apply(request)
           Await.result(result, DurationLong(5l) seconds)
           val status = result.value map { value =>            
               value.get.header.status
           }
           contentAsString(result) must equalTo("{\"login_error_status\":\"ACCOUNT_LOCKED\"}")
           status.get must equalTo(400)
        }
  }
}