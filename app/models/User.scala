package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class User(
  id: Option[Long] = None,
  email: String,
  password: Option[String],
  var name: String,
  failedLoginAttempts: Int = 0,
  enabled: Boolean
) extends Identity {
  
  def isAccountLocked(maxAttempts: Int) = {
    failedLoginAttempts >= maxAttempts
  }
  
  def mergeEditableChanges(user: User) {
    name = user.name
  }
  
  def loginInfo: LoginInfo = LoginInfo(CredentialsProvider.ID,email)
}

object User extends ((Option[Long],String,Option[String],String,Int,Boolean) => User) {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def newUser(email: String, name: String, enabled: Boolean, password: Option[String]):User = {
    User(None,email,password,name,0,enabled)
  }
  
  def currentUser(id: Option[Long], email: String, name: String, enabled: Boolean):User = {
    User(id,email,None,name,0,enabled)
  }
  
  def createJsonWrite(maxLoginAttempts: Int, idExternaliser: (User) => String): Writes[User] = {
     val userWrites: Writes[User] = (
      (__ \ "id").write[String] ~
      (__ \ "email").write[String] ~
      (__ \ "name").write[String] ~
      (__ \ "accountLocked").write[Boolean] ~
      (__ \ "enabled").write[Boolean]
     )((user: User) => (
      idExternaliser(user),
      user.email,
      user.name,
      user.isAccountLocked(maxLoginAttempts),
      user.enabled 
    ))
    userWrites
  }




}


