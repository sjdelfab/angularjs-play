package controllers

import scala.collection.mutable.ArraySeq
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import models.ApplicationRoleMembership
import models.User
import models.UserRoleMember
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Result
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.running
import services.SuccessInsert
import services.SuccessUpdate
import services.UniqueConstraintViolation
import services.UserService
import scala.concurrent.ExecutionContext.Implicits.global
import services.ForeignKeyConstraintViolation
import services.SuccessfulLogin
import services.AccountLocked
import utils.PasswordCrypt
import services.InvalidLoginAttempt

@RunWith(classOf[JUnitRunner])
class UsersTest extends Specification with AbstractControllerTest  {
  
  "Create User Test" should {
    
      "Bad JSON. Return BadRequest" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))             
         ) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
              usersController.createUser.apply(request) 
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
         }
      }
      
      "Forbidden" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           val result: Future[Result] = executeUserJsonOperation(jsonRequest) { usersController => request => 
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
      }
      
      "No password supplied" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
         }
      }
      
      "Password not strong enough" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PASSWORD_POLICY_MESSAGE -> "Password not strong enough"))
         ) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true, "password" -> "password123")
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> "Password not strong enough").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
      
      "Unique constraints violation" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true, "password" -> "PassWord504")
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             when(usersController.getUserService().createUser(any[User],any[String])).thenReturn(Future{UniqueConstraintViolation()})
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
      
      "Success" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true, "password" -> "PassWord504")
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             when(usersController.getUserService().createUser(any[User],any[String])).thenReturn(Future{SuccessInsert(1l)})
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("id" -> IndirectReferenceMapper.convertInternalIdToExternalised(1L)).toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
  }
  
  "Update User Test" should {
    
      "Bad JSON. Return BadRequest" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1L)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
         }
      }

     "Forbidden" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           val result: Future[Result] = executeUserJsonOperation(jsonRequest) { usersController => request => 
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
      }
      
      "Invalid externalised ID. Return BadRequest" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = "RUBBISH"
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request => 
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
         }
      }
      
      "Unique constraints violation" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             when(usersController.getUserService.updateUser(any[User])).thenReturn(Future{UniqueConstraintViolation()})
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
      
      "Success" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             when(usersController.getUserService.updateUser(any[User])).thenReturn(Future{SuccessUpdate(1)})
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "OK").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
  }
  
  "Get User Test" should {
    
      "User with 1 role" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val roles: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"))
           
           val result: Future[Result] = executeAdminUserOperation { usersController => request =>
             when(usersController.getUserService.findOneById(1)).thenReturn(Future{user})
             when(usersController.getUserService.getRoles(1)).thenReturn(Future{roles})
             usersController.getUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val toReturn = Json.obj("user" -> Json.toJson(user), "roles" -> Json.toJson(roles))
           contentAsString(result) must equalTo(toReturn.toString)
           
         }
      }
  }
  
  "Get Users Test" should {
    
      "Just return 1 user" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PAGE_SIZE -> 50))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val users = (ArraySeq(User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)),1)
           val result: Future[Result] = executeAdminUserOperation { usersController => request =>
             when(usersController.getUserService.getUsers(1,50)).thenReturn(Future{users._1})
             when(usersController.getUserService.getTotalUserCount()).thenReturn(Future{users._2})
             when(usersController.getUserService.getRoles(1)).thenReturn(Future{ArraySeq[ApplicationRoleMembership]()})
             usersController.getUsers(1).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedString = "{\"users\":[{\"id\":\"" + externalisedUserId +
                               "\",\"email\":\"simon@email.com\",\"name\":\"Simon\",\"accountLocked\":false,\"enabled\":true}],\"total\":1,\"numberOfPages\":1,\"pageSize\":50}"
           contentAsString(result) must equalTo(expectedString)
           
         }
      }
  }

  "Delete User" should {
    
     "Forbidden" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PAGE_SIZE -> 50))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation { usersController => request =>
               usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
     }
     
     "Invalid externalised ID. Return BadRequest" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = "RUBBISH"
           val result: Future[Result] = executeAdminUserOperation { usersController => request => 
             usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
         }
      }
      
      "Foreign Key constraints violation" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeAdminUserOperation { usersController => request => 
             when(usersController.getUserService.deleteUser(any[Long])).thenReturn(Future{ForeignKeyConstraintViolation()})
             when(usersController.getUserService.findOneById(1)).thenReturn(Future{user})
             usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "FK_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
      
      "Success" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeAdminUserOperation { usersController => request => 
             when(usersController.getUserService.deleteUser(any[Long])).thenReturn(Future{SuccessUpdate(1)})
             when(usersController.getUserService.findOneById(1)).thenReturn(Future{user})
             usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "OK").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
  }
  
  private def executeUserOperation(op:  Users => FakeRequest[Unit] => Future[Result]): Future[Result] = {
      val tokenAndRequest = createNoBodyRequest()
           
      val userSession = createUserSession(tokenAndRequest._1)
      val userService = mock[UserService]
      val usersController = new Users(userSession,userService)
                      
      val request = tokenAndRequest._2
      op(usersController)(request)
  }
  
  private def executeUserJsonOperation(jsonRequest: JsValue)(op:  Users => FakeRequest[JsValue] => Future[Result]): Future[Result] = {
      val tokenAndRequest = createJsonRequest(jsonRequest)
           
      val userSession = createUserSession(tokenAndRequest._1)
      val userService = mock[UserService]
      val usersController = new Users(userSession,userService)
      val request = tokenAndRequest._2
      op(usersController)(request)
  }
  
  private def executeAdminUserOperation(op:  Users => FakeRequest[Unit] => Future[Result]): Future[Result] = {
      val tokenAndRequest = createNoBodyRequest()
           
      val userSession = createAdminUserSession(tokenAndRequest._1)
      val userService = mock[UserService]
      val usersController = new Users(userSession,userService)
      val request = tokenAndRequest._2
      op(usersController)(request)
  }
  
  private def executeAdminUserJsonOperation(jsonRequest: JsValue)(op:  Users => FakeRequest[JsValue] => Future[Result]): Future[Result] = {
      val tokenAndRequest = createJsonRequest(jsonRequest)
           
      val userSession = createAdminUserSession(tokenAndRequest._1)
      val userService = mock[UserService]
      val usersController = new Users(userSession,userService)
      val request = tokenAndRequest._2
      op(usersController)(request)
  }
  
  "Enable User" should {
    
     "Forbidden" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PAGE_SIZE -> 50))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           
           val result: Future[Result] = executeUserOperation { usersController => request => 
             usersController.enableUser(externalisedUserId,true).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
     }
  }
  
  "Unlock User" should {
    
     "Forbidden" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PAGE_SIZE -> 50))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
          
           val result: Future[Result] = executeUserOperation { usersController => request => 
             usersController.unlockUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
     }
  }
  
  "Change user password" should {
    
     "Forbidden" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           
           val result: Future[Result] = executeUserOperation { usersController => request =>
             usersController.changeUserPassword(externalisedUserId,"NewPassword").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
     }
     
     "Invalid externalised user id" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PASSWORD_POLICY_MESSAGE -> "Password not strong enough"))
         ) {
           val externalisedUserId = "RUBBISH"
           
           val result: Future[Result] = executeAdminUserOperation { usersController => request =>
             usersController.changeUserPassword(externalisedUserId,"pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,400)
         }
     }
     
     "Password Not Strong Enough" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PASSWORD_POLICY_MESSAGE -> "Password not strong enough"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           
           val result: Future[Result] = executeAdminUserOperation { usersController => request =>
             usersController.changeUserPassword(externalisedUserId,"pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> "Password not strong enough").toString
           contentAsString(result) must equalTo(expectedResult)
         }
     }
  }
  
  "Change my password" should {
    
    
     "Password Not Strong Enough" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PASSWORD_POLICY_MESSAGE -> "Password not strong enough"))
         ) {
           val encryptedPassword = PasswordCrypt.encrypt("mypassword").get
           val user = User.currentUser(Some(1l), "person@email.com", "Person", true)
           val result: Future[Result] = executeUserOperation { usersController => request =>
             when(usersController.getUserService.authenticate("person@email.com",encryptedPassword)).thenReturn(Future{SuccessfulLogin(user)})
             usersController.changeMyPassword("mypassword","pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> "Password not strong enough").toString
           contentAsString(result) must equalTo(expectedResult)
         }
     }
     
     "Incorrect current password - InvalidLoginAttempt " in {
        running(FakeApplication()) {
           val encryptedPassword = PasswordCrypt.encrypt("mypassword").get
           val result: Future[Result] = executeUserOperation { usersController => request =>
             when(usersController.getUserService.authenticate("person@email.com",encryptedPassword)).thenReturn(Future{InvalidLoginAttempt()})
             usersController.changeMyPassword("mypassword","pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "INVALID_CURRENT_PASSWORD").toString
           contentAsString(result) must equalTo(expectedResult)
         }
     }
     
     "Incorrect current password - AccountLocked " in {
        running(FakeApplication()) {
           val encryptedPassword = PasswordCrypt.encrypt("mypassword").get
           val result: Future[Result] = executeUserOperation { usersController => request =>
             when(usersController.getUserService.authenticate("person@email.com",encryptedPassword)).thenReturn(Future{AccountLocked()})
             usersController.changeMyPassword("mypassword","pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "INVALID_CURRENT_PASSWORD").toString
           contentAsString(result) must equalTo(expectedResult)
         }
     }
  }
  
  "Get User" should {
    
    
     "Forbidden" in {
        running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation { usersController => request =>
             usersController.getUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
     }
    
      "OK" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled", PAGE_SIZE -> 50))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val result: Future[Result] = executeAdminUserOperation { usersController => request =>
             when(usersController.getUserService.findOneById(1)).thenReturn(Future{user})
             when(usersController.getUserService.getRoles(1)).thenReturn(Future{ArraySeq()})
             usersController.getUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedString = "{\"user\":{\"id\":\"" + externalisedUserId + "\",\"email\":\"simon@email.com\",\"name\":\"Simon\",\"accountLocked\":false,\"enabled\":true},\"roles\":[]}"
           contentAsString(result) must equalTo(expectedString)
           
         }
      }
  }
  
   "Delete Role Management" should {
    
      "Forbidden" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation { usersController => request =>
             usersController.deleteRoleMember(externalisedUserId,"resource_manager").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
      }
   }
  
  "Update Role Management" should {
    
      "Forbidden" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> "admin")
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeUserJsonOperation(jsonRequest) { usersController => request =>
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
         }
      }
    
      "Bad JSON. Return BadRequest" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType_rubbish" -> true)
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request => 
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
         }
      }

      "Invalid externalised ID. Return BadRequest" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = "RUBBISH"
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> true)
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request => 
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
         }
      }
      
      "Unique constraints violation" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> "admin")
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             when(usersController.getUserService.addRoleMembers(any[Seq[UserRoleMember]])).thenReturn(Future{UniqueConstraintViolation()})
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
      
      "Success" in {
         running(FakeApplication(
             additionalConfiguration = Map("dbplugin" -> "disabled"))
         ) {
           val externalisedUserId = IndirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> "admin")
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeAdminUserJsonOperation(jsonRequest) { usersController => request =>
             when(usersController.getUserService.addRoleMembers(any[Seq[UserRoleMember]])).thenReturn(Future{SuccessUpdate(1)})  
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "OK").toString
           contentAsString(result) must equalTo(expectedResult)
         }
      }
  }

  
}