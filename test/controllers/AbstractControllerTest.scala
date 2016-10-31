package controllers

import scala.concurrent.Future
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.mockito.Mockito.when
import java.util.UUID
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.mvc.Cookie
import play.api.mvc.Result
import services.UserSession
import security.Crypto
import security.InternalUser
import models.ApplicationRoleMembership
import play.api.Configuration

trait AbstractControllerTest extends SecurityCookieTokens with Mockito with Crypto { self: Specification =>
  
  def transformation: String = "AES"
  def secret: String = "wKZQxIc7:;oyOZ2N=C3C=UqYM1p>_`py:JAdpY;JBri7N?giFgp=Yj<QIlC<UFpv"
  
  val configuration = Configuration.apply(("play.crypto.secret",secret),
                                          (PASSWORD_MINIMUM_PASSWORD_LENGTH,8),
                                          (PASSWORD_MUST_HAVE_1_DIGIT,true),
                                          (PASSWORD_MUST_HAVE_1_NON_ALPHA,false),
                                          (PASSWORD_MUST_HAVE_1_UPPER_CASE,true),
                                          (PASSWORD_MUST_HAVE_1_LOWER_CASE,true),
                                          (PASSWORD_POLICY_MESSAGE,"Password not strong enough")
                                          )
  def indirectReferenceMapper = new CryptoIndirectReferenceMapper(configuration)
  
  def createJsonRequest(jsonRequest: JsValue) = {
    val token = UUID.randomUUID.toString
    val encryptedToken = encryptAES(token)
    val request = FakeRequest().withBody(jsonRequest).
                         withCookies(Cookie(AUTH_TOKEN_COOKIE_KEY, encryptedToken, None, httpOnly = false)).
                         withHeaders((AUTH_TOKEN_HEADER,encryptedToken))
    (token,request)                     
  }
  
  def createNoBodyRequest() = {
    val token = UUID.randomUUID.toString
    val encryptedToken = encryptAES(token)
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
