package e2e

import scala.collection.mutable.ArraySeq
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.mockito.Matchers.anyString
import org.mockito.Mockito.when
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mock.MockitoSugar
import org.scalatest.tags.ChromeBrowser
import org.scalatestplus.play.BrowserFactory
import org.scalatestplus.play.BrowserFactory.UnavailableDriver
import org.scalatestplus.play.OneBrowserPerSuite
import org.scalatestplus.play.OneServerPerSuite
import org.scalatestplus.play.PlaySpec

import models.ApplicationRoleMembership
import models.User
import play.api.test._
import scalaext.OptionExt.extendOption
import services.InvalidLoginAttempt
import services.SuccessfulLogin
import services.UserService
import utils.PasswordCrypt
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import com.mohiva.play.silhouette.password.BCryptPasswordHasher


@ChromeBrowser
class UsersManagementSpec extends PlaySpec with OneServerPerSuite with OneBrowserPerSuite with DevChromeFactory with MockitoSugar with BeforeAndAfterAll {
  
  val userService = mock[UserService]
  
  val hasher = new BCryptPasswordHasher()
  val encryptedPassword = hasher.hash("password").password
  
  val user = User(Some(1),"simon@email.com",Some(encryptedPassword),"Simon",0,true)
  val users = (ArraySeq(user),1)
  when(userService.getUsers(1,50)).thenReturn(Future{users._1})        
  when(userService.findOneById(1)).thenReturn(Future{user})
  when(userService.getTotalUserCount()).thenReturn(Future{1})
  val allRoles: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"),ApplicationRoleMembership(1l,"resource_manager"))
  when(userService.getRoles(1l)).thenReturn(Future{allRoles})
  
  when(userService.findByEmail("simon@email.com")).thenReturn(Future.successful(Some(user)))
  when(userService.findByEmail("wrong@email.com")).thenReturn(Future.successful(None))
    
  import controllers.`package`.PASSWORD_POLICY_MESSAGE
  
  implicit override lazy val app = new GuiceApplicationBuilder()
                       .configure(PASSWORD_POLICY_MESSAGE -> "Password not strong enough")
                       .overrides(bind(classOf[UserService]).toInstance(userService))
                       .build
  
  override def afterAll() = {
    val stopFuture = Await.result(app.stop(),Duration.Inf)
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
        passwordField.attribute("uib-popover") ifSome { popoverAttributeValue =>
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

trait DevChromeFactory extends BrowserFactory {

  def createWebDriver(): WebDriver =
    try {
      val options = new ChromeOptions()
      options.addArguments("--disable-extensions")
      new ChromeDriver(options)
    }
    catch {
      case ex: Throwable => UnavailableDriver(Some(ex), "Can't Create ChromeDriver")
    }
}
