package controllers

import scala.concurrent.Future

import org.specs2.mock.Mockito

import com.mohiva.play.silhouette.password.BCryptPasswordHasher

import models.User
import play.api.Configuration
import play.api.mvc.Result
import play.api.test.PlaySpecification
import security.SecurityRole

trait AbstractControllerTest extends Mockito { self: PlaySpecification =>
  
  def transformation: String = "AES"
  def secret: String = "wKZQxIc7:;oyOZ2N=C3C=UqYM1p>_`py:JAdpY;JBri7N?giFgp=Yj<QIlC<UFpv"
  
  val configuration = Configuration.apply(("play.crypto.secret",secret))
  
  def indirectReferenceMapper = new CryptoIndirectReferenceMapper(configuration)
  
  val hasher = new BCryptPasswordHasher()
  val user = User(Some(1l),"simon@acme.com",Some(hasher.hash("mypassword").password),"Simon",0,true)
  val adminRoles = Seq(SecurityRole("admin"))
    
  def statusMustbe(result: Future[Result], expectedStatus: Int) = {
    val status = result.value map { value =>            
         value.get.header.status
    }
    status.get must equalTo(expectedStatus)
  }
  
  
  
}
