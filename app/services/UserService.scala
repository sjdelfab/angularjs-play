package services

import dao.UsersDAO
import javax.inject.Singleton
import models.User
import models.UserRoleMember
import models.ApplicationRoleMembership
import org.postgresql.util.PSQLException
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scalaext.OptionExt._
import play.api.Play
import dao.UsersDAO

trait UserService {

  def findOneById(id: Long): User
  
  def authenticate(email: String, password: String): LoginResult
  
  def searchUsers(searchString: String): Seq[User]
  
  def getUsers(page: Int, pageSize: Int): (Seq[User],Int)
  
  def updateUser(user: User): DatabaseResult
  
  def createUser(user: User, encryptedPassword: String): DatabaseResult
  
  def enableUser(userId: Long, status: Boolean): Int
  
  def unlockUser(userId: Long): Int
  
  def deleteUser(userId: Long): DatabaseResult
  
  def changeUserPassword(userId: Long, newPasswordEncrypted: String): Int
  
  def getRoleMembers(roleType: String): Seq[UserRoleMember]
  
  def getRoleNonMembers(roleType: String): Seq[UserRoleMember]
  
  def deleteRoleMember(userId: Long, roleType: String): Int
  
  def addRoleMembers(newMembers: Seq[UserRoleMember]):DatabaseResult
  
  def getRoles(userId: Long): Seq[ApplicationRoleMembership]
}

@Singleton
class UserServiceDatabase extends UserService with AbstractService {
  
  override def findOneById(id: Long): User = {
      UsersDAO.find(id)
  }
  
  override def authenticate(email: String, password: String): LoginResult = {
    UsersDAO.findByEmail(email) ifSome { user =>
      if (user.isAccountLocked(Play.current.configuration.getInt(controllers.MAX_FAILED_LOGIN_ATTEMPTS).getOrElse(3))) {
         AccountLocked()   
      } else if (user.password.get == password) {
         SuccessfulLogin(user)
      } else {
         UsersDAO.updatefailedLoginAttempt(user.id.get,user.failedLoginAttempts + 1)
         InvalidLoginAttempt()
      }
    } otherwise {
      InvalidLoginAttempt()
    }    
  }
  
  override def unlockUser(userId: Long): Int = {
    UsersDAO.updatefailedLoginAttempt(userId,0)
  }
  
  override def deleteUser(userId: Long): DatabaseResult = {
    translateDatabaseUpdate(UsersDAO.delete(userId))
  }
  
  override def searchUsers(searchString: String): Seq[User] = {
    UsersDAO.searchByNameAndEmail("%" + searchString + "%")
  }

  override def getUsers(page: Int, pageSize: Int): (Seq[User],Int) = {
    UsersDAO.allPaged(page, pageSize)
  }
  
  override def updateUser(user: User) = {    
    translateDatabaseUpdate(UsersDAO.update(user))
  }
  
  override def enableUser(userId: Long, status: Boolean) = {
    UsersDAO.enableUser(userId,status)
  }
  
  override def changeUserPassword(userId: Long, newPasswordEncrypted: String) = {
    UsersDAO.changeUserPassword(userId,newPasswordEncrypted)
  }
  
  override def createUser(user: User, encryptedPassword: String) = {
    val newUser = User(None,user.email,Some(encryptedPassword),user.name,0,user.enabled)
    translateDatabaseInsert(UsersDAO.create(newUser))
  }
  
  override def getRoleMembers(roleType: String): Seq[UserRoleMember] = {
    UsersDAO.getRoleMembers(roleType)
  }
  
  override def getRoleNonMembers(roleType: String): Seq[UserRoleMember] = {
    UsersDAO.getRoleNonMembers(roleType)
  }
  
  override def deleteRoleMember(userId: Long, roleType: String) = {
    UsersDAO.deleteRoleMember(userId, roleType).get
  }
  
  override def addRoleMembers(newMembers: Seq[UserRoleMember]): DatabaseResult = {
    translateDatabaseBatchInsert(UsersDAO.addRoleMembers(newMembers),newMembers.length)
  }
  
  override def getRoles(userId: Long): Seq[ApplicationRoleMembership] = {
    UsersDAO.getRoles(userId)
  }
}

abstract class LoginResult
case class SuccessfulLogin(user: User) extends LoginResult
case class InvalidLoginAttempt() extends LoginResult
case class AccountLocked() extends LoginResult