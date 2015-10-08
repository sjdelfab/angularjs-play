package e2e

import scala.collection.mutable.ArraySeq
import org.mockito.Matchers.anyString
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.tags.ChromeBrowser
import org.scalatestplus.play.ChromeFactory
import org.scalatestplus.play.OneBrowserPerSuite
import org.scalatestplus.play.OneServerPerSuite
import org.scalatestplus.play.PlaySpec
import controllers.SecurityCookieTokens
import models.ApplicationRoleMembership
import models.User
import play.api.GlobalSettings
import play.api.test.FakeApplication
import scalaext.OptionExt.extendOption
import security.InternalUser
import services.InvalidLoginAttempt
import services.SuccessfulLogin
import services.UserService
import services.UserSession
import utils.PasswordCrypt
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@ChromeBrowser
class UsersManagementSpec extends PlaySpec with OneServerPerSuite with OneBrowserPerSuite with ChromeFactory  with SecurityCookieTokens with MockitoSugar with BeforeAndAfterAll {
  
  // Override app if you need a FakeApplication with other than non-default parameters.
  implicit override lazy val app: FakeApplication =
    FakeApplication(
      additionalConfiguration = Map("dbplugin" -> "disabled", controllers.PASSWORD_POLICY_MESSAGE -> "Password not strong enough"),
      withGlobal = Some(new GlobalSettings() {
        val userService = mock[UserService]
        val user = User(Some(1),"simon@email.com",Some("password"),"Simon",0,true)
        val users = (ArraySeq(user),1)
        when(userService.getUsers(1,50)).thenReturn(Future{users._1})        
        when(userService.findOneById(1)).thenReturn(Future{user})
        val allRoles: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"),ApplicationRoleMembership(1l,"resource_manager"))
        when(userService.getRoles(1l)).thenReturn(Future{allRoles})
        val encryptedPassword = PasswordCrypt.encrypt("password").get
        
        when(userService.authenticate("simon@email.com",encryptedPassword)).thenReturn(Future{SuccessfulLogin(user)})
        when(userService.authenticate("wrong@email.com",encryptedPassword)).thenReturn(Future{InvalidLoginAttempt()})
        
        val userSession = createAdminUserSession();
              
      })
    )
  
  override def afterAll() {
    Await.result(app.stop(),Duration.Inf)
  }  
    
  def createAdminUserSession() = {
    val userSession = mock[UserSession]
    val allRoles: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"),ApplicationRoleMembership(1l,"resource_manager"))
    val user = new InternalUser("person@email.com",1l,Some(allRoles))
    when(userSession.lookup(anyString())).thenReturn(Some(user))
    userSession
  }  
    
  val baseUrl = "http://localhost:" + port  
    
  "Login" must {
    
    "Unsuccessful Login" in {
        go to (baseUrl + "/#/login")
        pageTitle mustBe "MyApp"
        eventually {
          click on name("email")
          enter("wrong@email.com")
          click on name("password")
          enter("password")
          click on className("btn")
        }
        eventually {
        find(id("loginError")) ifNone {
          fail("Must have login error message")
        } otherwise { alert =>
          alert.isDisplayed mustBe true
          alert.text mustBe "Invalid username or password"
        }        
      }
    }
    
    "Successful Login" in {
        go to (baseUrl + "/#/login")
        pageTitle mustBe "MyApp"
        eventually {
          click on name("email")
          enter("simon@email.com")
          click on name("password")
          enter("password")
          click on className("btn")
        }
        eventually {
          val userMenuButton = find(id("userMenuItem"))
          userMenuButton ifNone { 
            fail("Must have user menu item button")
          } otherwise { button =>
            button.isDisplayed mustBe true
          }
      }
    }    
  }  
    
  "Change password" must {
    
    "Passwords don't match" in {
      eventually {
        click on id("adminMenu")
      }
      eventually {
        click on id("usersMenu")
      }
      eventually {
        val h3 = find(tagName("h3")).get.text
        h3 mustBe "Manage Users"
      }
      eventually {
        click on id("action_1")
      }
      eventually {
        click on id("chgpwd_1")
      }
      var okButton: Option[Element] = None
      eventually {
        okButton = find(id("ok"))
        okButton ifNone { 
          fail("Must have disabled OK button")
        } otherwise { button =>
          button.isEnabled mustBe false
        }
      }
      pwdField("password").value = "test1"
      pwdField("retypePassword").value = "test2"
      click on okButton.get
      eventually {
        find(id("passwordError")) ifNone {
          fail("Must have error message")
        } otherwise { alert =>
          alert.isDisplayed mustBe true
          alert.text mustBe "Passwords do not match"
        }        
      }
      eventually {
        val closeButton = find(id("close"))
        click on closeButton.get
      }      
    }
  }
  
  "New user" must {
      var saveUserButton: Option[Element] = None
    
     "Passwords do not match" in {
        eventually {
          click on id("newUser")
        }
        
        eventually {
          saveUserButton = find(id("saveUserButton"))
          saveUserButton ifNone { 
            fail("Must have disabled Save button")
          } otherwise { button =>
            button.isEnabled mustBe true
          }
        }
        textField("userName").value = "John Smith"
        emailField("userEmail").value = "john@email.com"
        pwdField("password").value = "password1"
        pwdField("retypePassword").value = "password2"
        click on saveUserButton.get
        var passwordField = pwdField("password")
        passwordField.attribute("popover") ifSome { popoverAttributeValue =>
          popoverAttributeValue mustBe "Passwords do not match"
        } otherwise {
          fail("Must have popover attribute")
        }
      }
     
     "Password not strong enough" in {       
        pwdField("password").value = "password"
        pwdField("retypePassword").value = "password"
        click on saveUserButton.get
        eventually {
          find(id("userErrorMessage")) ifNone {
            fail("Must have error message")
          } otherwise { alert =>
            alert.isDisplayed mustBe true
            alert.text mustBe "Password not strong enough"
          }        
        }
      }
  }

}
