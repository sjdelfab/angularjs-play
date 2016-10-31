package services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

import dao.UsersDAO
import javax.inject.Inject
import javax.inject.Singleton
import models.ApplicationRoleMembership
import models.User
import models.UserRoleMember
import play.api.Play
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import scalaext.OptionExt._
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

trait UserService {

  def findOneById(id: Long): Future[User]
  
  def authenticate(email: String, password: String): Future[LoginResult]
  
  def searchUsers(searchString: String): Future[Seq[User]]
  
  def getUsers(page: Int, pageSize: Int): Future[Seq[User]]
  
  def getTotalUserCount(): Future[Int]
  
  def updateUser(user: User): Future[DatabaseResult]
  
  def createUser(user: User, encryptedPassword: String): Future[DatabaseResult]
  
  def enableUser(userId: Long, status: Boolean): Future[Int]
  
  def unlockUser(userId: Long): Future[Int]
  
  def deleteUser(userId: Long): Future[DatabaseResult]
  
  def changeUserPassword(userId: Long, newPasswordEncrypted: String): Future[Int]
  
  def getRoleMembers(roleType: String): Future[Seq[UserRoleMember]]
  
  def getRoleNonMembers(roleType: String): Future[Seq[UserRoleMember]]
  
  def deleteRoleMember(userId: Long, roleType: String): Future[Int]
  
  def addRoleMembers(newMembers: Seq[UserRoleMember]):Future[DatabaseResult]
  
  def getRoles(userId: Long): Future[Seq[ApplicationRoleMembership]]
}

@Singleton
class UserServiceDatabase @Inject()(dbConfigProvider: DatabaseConfigProvider, configuration: Configuration) extends UserService with AbstractService {
  
  val db = dbConfigProvider.get[JdbcProfile].db
  
  override def findOneById(id: Long): Future[User] = {
      db.run(UsersDAO.find(id))
  }
  
  override def authenticate(email: String, password: String): Future[LoginResult] = {
    db.run(UsersDAO.findByEmail(email)) map { userOption =>
        userOption ifSome { user =>
          if (user.isAccountLocked(configuration.getInt(controllers.MAX_FAILED_LOGIN_ATTEMPTS).getOrElse(3))) {
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
  }
  
  override def unlockUser(userId: Long): Future[Int] = {    
    db.run(UsersDAO.updatefailedLoginAttempt(userId,0).transactionally) map { result => result } 
  }
  
  override def deleteUser(userId: Long): Future[DatabaseResult] = {
    db.run(UsersDAO.delete(userId).asTry.transactionally) map { result =>
        translateDatabaseUpdate(result)
    }
  }
  
  override def searchUsers(searchString: String): Future[Seq[User]] = {
    db.run(UsersDAO.searchByNameAndEmail("%" + searchString + "%").result)
  }

  override def getUsers(page: Int, pageSize: Int): Future[Seq[User]] = {
    db.run(UsersDAO.allPaged(page, pageSize))
  }
  
  override def getTotalUserCount(): Future[Int] = {
    db.run(UsersDAO.getTotalUserCount())
  }
  
  override def updateUser(user: User) = {
    val query = UsersDAO.update(user)
    db.run(query.asTry.transactionally) map { result =>
       translateDatabaseUpdate(result)
    } 
  }
  
  override def enableUser(userId: Long, status: Boolean) = {
    db.run(UsersDAO.enableUser(userId,status).transactionally) map { result => result } 
  }
  
  override def changeUserPassword(userId: Long, newPasswordEncrypted: String) = {
    db.run(UsersDAO.changeUserPassword(userId,newPasswordEncrypted).transactionally) map { result => result }
  }
  
  override def createUser(user: User, encryptedPassword: String) = {
    val newUser = User(None,user.email,Some(encryptedPassword),user.name,0,user.enabled)
    val query = UsersDAO.create(newUser)
    db.run(query.asTry.transactionally) map { result =>
       translateDatabaseInsert(result)
    }    
  }
  
  override def getRoleMembers(roleType: String): Future[Seq[UserRoleMember]] = {
    val query = UsersDAO.getRoleMembers(roleType).result
    db.run(query) map { result =>
      result map {
         case((userId:Long,userName:String,userEmail:String,roleType:String)) => UserRoleMember(userId,userName,userEmail,roleType)  
      }      
    }
  }
  
  override def getRoleNonMembers(roleType: String): Future[Seq[UserRoleMember]] = {
    db.run(UsersDAO.getRoleNonMembers(roleType))
  }
  
  override def deleteRoleMember(userId: Long, roleType: String) = {
    val query = UsersDAO.deleteRoleMember(userId, roleType)
    db.run(query.asTry.transactionally) map { result => result.get }
  }
  
  override def addRoleMembers(newMembers: Seq[UserRoleMember]): Future[DatabaseResult] = {
    val query = UsersDAO.addRoleMembers(newMembers)
    db.run(query.asTry.transactionally) map { rowsInserted =>
        translateDatabaseBatchInsertOption(rowsInserted,newMembers.length)
    }      
  }
  
  override def getRoles(userId: Long): Future[Seq[ApplicationRoleMembership]] = {
    db.run(UsersDAO.getRoles(userId).result) map { result =>
        result.toSet[ApplicationRoleMembership].toList
    }
  }
}

abstract class LoginResult
case class SuccessfulLogin(user: User) extends LoginResult
case class InvalidLoginAttempt() extends LoginResult
case class AccountLocked() extends LoginResult