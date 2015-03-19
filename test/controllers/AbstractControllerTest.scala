package controllers

import scala.concurrent.Future
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.mockito.Mockito.when
import play.api.libs.Crypto
import java.util.UUID
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.mvc.Cookie
import play.api.mvc.Result
import services.UserSession
import security.InternalUser
import models.ApplicationRoleMembership

trait AbstractControllerTest extends SecurityCookieTokens with Mockito { self: Specification =>

  def createJsonRequest(jsonRequest: JsValue) = {
    val token = UUID.randomUUID.toString
    val encryptedToken = Crypto.encryptAES(token)
    val request = FakeRequest().withBody(jsonRequest).
                         withCookies(Cookie(AUTH_TOKEN_COOKIE_KEY, encryptedToken, None, httpOnly = false)).
                         withHeaders((AUTH_TOKEN_HEADER,encryptedToken))
    (token,request)                     
  }
  
  def createNoBodyRequest() = {
    val token = UUID.randomUUID.toString
    val encryptedToken = Crypto.encryptAES(token)
    val request = FakeRequest().withBody((): Unit).withCookies(Cookie(AUTH_TOKEN_COOKIE_KEY, encryptedToken, None, httpOnly = false)).
                         withHeaders((AUTH_TOKEN_HEADER,encryptedToken))
    (token,request)                     
  }
  
  def createUserSession(token: String) = {
    val userSession = mock[UserSession]
    val user = new InternalUser("person@email.com",1l)
    when(userSession.lookup(token)).thenReturn(Some(user))
    userSession
  }
  
  def createAdminUserSession(token: String) = {
    val userSession = mock[UserSession]
    val allGrps: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"),ApplicationRoleMembership(1l,"resource_manager"))
    val user = new InternalUser("person@email.com",1l,Some(allGrps))
    when(userSession.lookup(token)).thenReturn(Some(user))
    userSession
  }
  
  def statusMustbe(result: Future[Result], expectedStatus: Int) = {
    val status = result.value map { value =>            
         value.get.header.status
    }
    status.get must equalTo(expectedStatus)
  } 
  
}