package models

import java.util.Date
import scala.util.Try
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.ExtensionMethods
import play.api.Play.current
import play.api.Play

case class User(
  id: Option[Long] = None,
  email: String,
  password: Option[String],
  var name: String,
  failedLoginAttempts: Int = 0,
  enabled: Boolean
) {
  
  def isAccountLocked(maxAttempts: Int) = {
    failedLoginAttempts >= maxAttempts
  }
  
  def mergeEditableChanges(user: User) {
    name = user.name
  }
}

object User extends ((Option[Long],String,Option[String],String,Int,Boolean) => User) {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  def newUser(email: String, name: String, enabled: Boolean, password: Option[String]):User = {
    User(None,email,password,name,0,enabled)
  }
  
  def currentUser(id: Option[Long], email: String, name: String, enabled: Boolean):User = {
    User(id,email,None,name,0,enabled)
  }
  
  implicit val UserToJson: Writes[User] = (
    (__ \ "id").write[String] ~
    (__ \ "email").write[String] ~
    (__ \ "name").write[String] ~
    (__ \ "accountLocked").write[Boolean] ~
    (__ \ "enabled").write[Boolean]
  )((user: User) => (
    controllers.IndirectReferenceMapper.convertInternalIdToExternalised(user.id.get),
    user.email,
    user.name,
    user.isAccountLocked(Play.current.configuration.getInt(controllers.MAX_FAILED_LOGIN_ATTEMPTS).getOrElse(3)),
    user.enabled 
  ))

}


