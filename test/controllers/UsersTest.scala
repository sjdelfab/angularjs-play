package controllers

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.specs2.runner.JUnitRunner

import javax.inject.Singleton
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Result
import play.api.test.PlaySpecification
import play.api.test.WithApplication
import security.UserIdentity
import services.UniqueConstraintViolation
import services.SuccessInsert
import models.User
import services.SuccessUpdate
import play.api.libs.json.Writes
import models.ApplicationRoleMembership
import scala.collection.mutable.ArraySeq
import services.ForeignKeyConstraintViolation
import utils.PasswordCrypt
import services.SuccessfulLogin
import models.UserRoleMember

@RunWith(classOf[JUnitRunner])
class UsersTest extends PlaySpecification with AbstractControllerTest  {
  
  "Create User Test" should {
        
      "Bad JSON. Return BadRequest" in new TestScope {
         override def identity = UserIdentity(user,Some(adminRoles))
        
         new WithApplication(application) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           
           val usersController = app.injector.instanceOf[Users]
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
          
           contentAsString(result) must be equalTo("Invalid request")
           statusMustbe(result,400)
         }  
      }
      
      
      "Forbidden" in new TestScope {
        override def identity = UserIdentity(user)
        new WithApplication(application) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
       }
      }
      
      "No password supplied" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
        }
      } 
      
      "Password not strong enough" in new TestScope  {
        override def identity = UserIdentity(user,Some(adminRoles))
        override def overrideConfig: Map[String, Any] = Map(PASSWORD_POLICY_MESSAGE -> "Password not strong enough")
        new WithApplication(application) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true, "password" -> "password123")
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> "Password not strong enough").toString
           contentAsString(result) must equalTo(expectedResult)
        }
      }
      
      "Unique constraints violation" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true, "password" -> "PassWord504")
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             when(usersController.getUserService().createUser(any[User],any[String])).thenReturn(Future.successful(UniqueConstraintViolation()))
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
        }
      }
      
      "Success" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val jsonRequest: JsValue = Json.obj("id" -> "new","name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true, "password" -> "PassWord504")
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             when(usersController.getUserService().createUser(any[User],any[String])).thenReturn(Future.successful(SuccessInsert(1l)))
             usersController.createUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("id" -> indirectReferenceMapper.convertInternalIdToExternalised(1L)).toString
           contentAsString(result) must equalTo(expectedResult)
        }
      }
  }
  
  "Update User Test" should {
    
      "Bad JSON. Return BadRequest" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1L)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
        }
      }

     "Forbidden" in new TestScope {
       override def identity = UserIdentity(user)
       new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled_rubbish" -> true)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request => 
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
       }
      }
      
      "Invalid externalised ID. Return BadRequest" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = "RUBBISH"
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request => 
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
        }
      }
      
      "Unique constraints violation" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             when(usersController.getUserService.updateUser(any[User])).thenReturn(Future.successful(UniqueConstraintViolation()))
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
        }
      }
      
      "Success" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val jsonRequest: JsValue = Json.obj("id" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "enabled" -> true)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             when(usersController.getUserService.updateUser(any[User])).thenReturn(Future.successful(SuccessUpdate(1)))
             usersController.updateUser.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "OK").toString
           contentAsString(result) must equalTo(expectedResult)
        }
      }
  }
  
  implicit val userToJsonWrites: Writes[User] = User.createJsonWrite(3, user => indirectReferenceMapper.convertInternalIdToExternalised(user.id.get))
  implicit val appGroupMembershipToJson: Writes[ApplicationRoleMembership] = ApplicationRoleMembership.createJsonWrite(id => indirectReferenceMapper.convertInternalIdToExternalised(id))
  
  "Get User Test" should {
    
      "User with 1 role" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val roles: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"))
           
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             when(usersController.getUserService.findOneById(1)).thenReturn(Future.successful(user))
             when(usersController.getUserService.getRoles(1)).thenReturn(Future.successful(roles))
             usersController.getUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val toReturn = Json.obj("user" -> Json.toJson(user), "roles" -> Json.toJson(roles))
           contentAsString(result) must equalTo(toReturn.toString)           
        }
      }
  }
  
  "Get Users Test" should {
    
      "Just return 1 user" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val users = (ArraySeq(User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)),1)
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             when(usersController.getUserService.getUsers(1,50)).thenReturn(Future.successful(users._1))
             when(usersController.getUserService.getTotalUserCount()).thenReturn(Future.successful(users._2))
             when(usersController.getUserService.getRoles(1)).thenReturn(Future.successful(ArraySeq[ApplicationRoleMembership]()))
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
    
     "Forbidden" in new TestScope {
       override def identity = UserIdentity(user)
       new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
               usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
      }
     }
     
     "Invalid externalised ID. Return BadRequest" in new TestScope {
       override def identity = UserIdentity(user,Some(adminRoles))
       new WithApplication(application) {
           val externalisedUserId = "RUBBISH"
           val result: Future[Result] = executeUserOperation(app) { usersController => request => 
             usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
      }
     }
      
     "Foreign Key constraints violation" in new TestScope {
       override def identity = UserIdentity(user,Some(adminRoles))
       new WithApplication(application) {
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation(app) { usersController => request => 
             when(usersController.getUserService.deleteUser(any[Long])).thenReturn(Future.successful(ForeignKeyConstraintViolation()))
             when(usersController.getUserService.findOneById(1)).thenReturn(Future.successful(user))
             usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "FK_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
       }
     }
      
     "Success" in new TestScope {
       override def identity = UserIdentity(user,Some(adminRoles))
       new WithApplication(application) {
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation(app) { usersController => request => 
             when(usersController.getUserService.deleteUser(any[Long])).thenReturn(Future.successful(SuccessUpdate(1)))
             when(usersController.getUserService.findOneById(1)).thenReturn(Future.successful(user))
             usersController.deleteUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "OK").toString
           contentAsString(result) must equalTo(expectedResult)
      }
     }
  }
   
  "Enable User" should {
    
     "Forbidden" in new TestScope {
       override def identity = UserIdentity(user)
       new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           
           val result: Future[Result] = executeUserOperation(app) { usersController => request => 
             usersController.enableUser(externalisedUserId,true).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
       }
     }
  }
  
  "Unlock User" should {
    
     "Forbidden" in new TestScope {
       override def identity = UserIdentity(user)
       new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
          
           val result: Future[Result] = executeUserOperation(app) { usersController => request => 
             usersController.unlockUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
      }
     }
  }
  
  "Change user password" should {
    
     "Forbidden" in new TestScope {
       override def identity = UserIdentity(user)
       new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             usersController.changeUserPassword(externalisedUserId,"NewPassword").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
       }
     }
     
     "Invalid externalised user id" in new TestScope {
       override def identity = UserIdentity(user,Some(adminRoles))
       new WithApplication(application) {
           val externalisedUserId = "RUBBISH"
           
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             usersController.changeUserPassword(externalisedUserId,"pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,400)
       }
     }
     
     "Password Not Strong Enough" in new TestScope {
       override def identity = UserIdentity(user,Some(adminRoles))
       override def overrideConfig: Map[String, Any] = Map(PASSWORD_POLICY_MESSAGE -> "Password not strong enough")
       new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             usersController.changeUserPassword(externalisedUserId,"pass1234").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> "Password not strong enough").toString
           contentAsString(result) must equalTo(expectedResult)
       }
     }
  }
    
  "Get User" should {
    
     "Forbidden" in new TestScope { 
       override def identity = UserIdentity(user)
       new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             usersController.getUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
       }
     }
    
      "OK" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             when(usersController.getUserService.findOneById(1)).thenReturn(Future.successful(user))
             when(usersController.getUserService.getRoles(1)).thenReturn(Future.successful(ArraySeq()))
             usersController.getUser(externalisedUserId).apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedString = "{\"user\":{\"id\":\"" + externalisedUserId + "\",\"email\":\"simon@email.com\",\"name\":\"Simon\",\"accountLocked\":false,\"enabled\":true},\"roles\":[]}"
           contentAsString(result) must equalTo(expectedString)
        }
      }
  }
  
   "Delete Role Management" should {
    
      "Forbidden" in new TestScope {
        override def identity = UserIdentity(user)
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val result: Future[Result] = executeUserOperation(app) { usersController => request =>
             usersController.deleteRoleMember(externalisedUserId,"resource_manager").apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
        }
      }
   }
  
  "Update Role Management" should {
    
      "Forbidden" in new TestScope { 
        override def identity = UserIdentity(user)
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> "admin")
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           statusMustbe(result,403)
        }
      }
    
      "Bad JSON. Return BadRequest" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType_rubbish" -> true)
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request => 
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
       }
      }

      "Invalid externalised ID. Return BadRequest" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = "RUBBISH"
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> true)
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request => 
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           contentAsString(result) must equalTo("Invalid request")
           statusMustbe(result,400)
        }
      }
      "Unique constraints violation" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> "admin")
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             when(usersController.getUserService.addRoleMembers(any[Seq[UserRoleMember]])).thenReturn(Future.successful(UniqueConstraintViolation()))
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION").toString
           contentAsString(result) must equalTo(expectedResult)
        }
      }
      
      "Success" in new TestScope {
        override def identity = UserIdentity(user,Some(adminRoles))
        new WithApplication(application) {
           val externalisedUserId = indirectReferenceMapper.convertInternalIdToExternalised(1)
           val newUserRole: JsValue = Json.obj("userId" -> externalisedUserId, "name" -> "foobar", "email" -> "foo@email.com", "roleType" -> "admin")
           val jsonRequest = Json.arr(newUserRole)
           val result: Future[Result] = executeUserJsonOperation(app,jsonRequest) { usersController => request =>
             when(usersController.getUserService.addRoleMembers(any[Seq[UserRoleMember]])).thenReturn(Future.successful(SuccessUpdate(1)))  
             usersController.addUsersToRole.apply(request)
           }
           Await.result(result, DurationLong(5l) seconds)
           val expectedResult = Json.obj("status" -> "OK").toString
           contentAsString(result) must equalTo(expectedResult)
        }
      }
  }

}