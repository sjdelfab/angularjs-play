package integration

import integration.dbunit.AbstractIntegrationTest
import org.scalatest.BeforeAndAfter
import play.api.test.FakeApplication
import org.scalatestplus.play.PlaySpec
import services.UserServiceDatabase
import org.scalatestplus.play.OneAppPerSuite
import services.SuccessfulLogin
import services.AccountLocked
import services.InvalidLoginAttempt
import models.User
import services.UniqueConstraintViolation
import models.ApplicationRoleMembership
import models.UserRoleMember

class UserServiceTest extends PlaySpec with AbstractIntegrationTest with BeforeAndAfter with OneAppPerSuite {

  implicit override lazy val app: FakeApplication =
    FakeApplication(
      additionalConfiguration = Map("db.default.url" -> "jdbc:postgresql://localhost:5432/myapp_test?stringtype=unspecified",
                                    controllers.MAX_FAILED_LOGIN_ATTEMPTS -> 3)
    )
  
  var hasInitialisedDatabase = false
   
  before {
     if (!hasInitialisedDatabase) {
       setUpBeforeClass("test/integration/usersDataset.json")
       hasInitialisedDatabase = true
     }
  }
  
  val userService = new UserServiceDatabase()
  
 "User Service - authenticate" should {
    "Succesful login" in {
     userService.authenticate("simon@email.com", "papAq5PwY/QQM") match {
        case SuccessfulLogin(user) => {
           // OK  
        }
        case _ => {
          fail("Must be successful login")
        }
     }
    }
    
    "Acccount Locked" in {
     userService.authenticate("locked@email.com", "papAq5PwY/QQM") match {        
        case AccountLocked() => {
          // OK
        }
        case _ => {
           fail("Must be account locked")   
        }
     }
    }
    
    "Wrong password" in {
     userService.authenticate("simon@email.com", "Wrong password") match {        
        case InvalidLoginAttempt() => {
          // OK
        }
        case _ => {
           fail("Must be invalid password")   
        }
     }
    }
  }
 
 "User Service - createUser" should {
    "Unique constraint failure" in {
       val user = User.newUser("simon@email.com", "Simon", true, Some("newPassword"))
       userService.createUser(user, "papAq5PwY/QQM") match {
          case UniqueConstraintViolation() => {
             // OK  
          }
          case _ => {
            fail("Duplicate on email so should be UniqueConstraintViolation")
          }
       }
    }
  }
 
 "User Service - updateUser" should {
    "Unique constraint failure" in {
       val user = User.currentUser(Some(2),"simon@email.com", "Simon", true)
       userService.updateUser(user) match {
          case UniqueConstraintViolation() => {
             // OK  
          }
          case _ => {
            fail("Duplicate on email so should be UniqueConstraintViolation")
          }
       }
    }
  }
 
  "Role members management" should {
      "Have a user" in {         
         val members = userService.getRoleMembers("admin")
         members.size mustBe 1
      }
      "Not have a user" in {
         val members = userService.getRoleMembers("resource_manager")
         members.size mustBe 0
      }
      "Get Non members" in {
         val members = userService.getRoleNonMembers("admin")
         members.size mustBe 2
      }
      "Add member already in role" in {
         val members = userService.getRoleMembers("admin")
         members.size mustBe 1
         userService.addRoleMembers(members) match {
           case UniqueConstraintViolation() => {
             // OK  
          }
          case _ => {
            fail("Duplicate on user role member so should be UniqueConstraintViolation")
          } 
         }
      }
      "Add member" in {
         val newMembers: Seq[UserRoleMember] = List(UserRoleMember(1l,"Simon","simon@email.com","resource_manager"))
         userService.addRoleMembers(newMembers)
         val members = userService.getRoleNonMembers("admin")
         members.size mustBe 2
      }
  }
}
