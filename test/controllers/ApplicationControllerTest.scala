package controllers

import org.specs2.runner.JUnitRunner
import org.mockito.Mockito.when

import org.junit.runner.RunWith
import play.api.test.PlaySpecification
import scala.concurrent.duration.`package`.DurationLong
import services.SuccessfulLogin
import services.AccountLocked
import security.UserIdentity
import utils.PasswordCrypt
import services.InvalidLoginAttempt
import scala.concurrent.Await
import models.User
import scala.concurrent.Future
import play.api.test.WithApplication
import play.api.libs.json.Json
import play.api.mvc.Result
import models.ApplicationRoleMembership

@RunWith(classOf[JUnitRunner])
class ApplicationControllerTest extends PlaySpecification with AbstractControllerTest {

  "Change my password" should {
        
     "Password Not Strong Enough" in new TestScope {
       override def identity = UserIdentity(user)
       override def overrideConfig: Map[String, Any] = Map(PASSWORD_POLICY_MESSAGE -> "Password not strong enough")
       new WithApplication(application) {
           val result: Future[Result] = executeApplicationOperation(app) { appController => request =>
             when(userService.findByEmail("simon@acme.com")).thenReturn(Future.successful(Some(user)))
             when(userService.getRoles(1l)).thenReturn(Future.successful(List[ApplicationRoleMembership]()))
             appController.changeMyPassword("mypassword","pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> "Password not strong enough").toString
           contentAsString(result) must equalTo(expectedResult)
       }
     }
     
     "Incorrect current password - InvalidLoginAttempt " in new TestScope {
       override def identity = UserIdentity(user)
       new WithApplication(application) {
           val result: Future[Result] = executeApplicationOperation(app) { appController => request =>
             when(userService.findByEmail("simon@acme.com")).thenReturn(Future.successful(Some(user)))
             when(userService.getRoles(1l)).thenReturn(Future.successful(List[ApplicationRoleMembership]()))
             appController.changeMyPassword("wrongpassword","Flouts01").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "INVALID_CURRENT_PASSWORD").toString
           contentAsString(result) must equalTo(expectedResult)
       }
     }
     
     "Incorrect current password - AccountLocked " in new TestScope {
       override def identity = UserIdentity(user)
       new WithApplication(application) {
         val userAccountLocked = User(Some(1l),"simon@acme.com",Some(hasher.hash("mypassword").password),"Simon",10,true)
           val result: Future[Result] = executeApplicationOperation(app) { appController => request =>
             when(userService.findByEmail("simon@acme.com")).thenReturn(Future.successful(Some(userAccountLocked)))
             when(userService.getRoles(1l)).thenReturn(Future.successful(List[ApplicationRoleMembership]()))
             appController.changeMyPassword("mypassword","Flouts01").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "ACCOUNT_LOCKED").toString
           contentAsString(result) must equalTo(expectedResult)
       }
     }
  }

  
}