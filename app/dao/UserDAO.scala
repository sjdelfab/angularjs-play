package dao

import java.util.Date
import models.ApplicationRoleMembership
import models.User
import models.UserRoleMember
import scala.util.Try
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.ExtensionMethods
import scalaext.OptionExt._
import play.api.Play.current
import play.api.Play


class UsersTable(tag: Tag) extends Table[User](tag, "application_user") {
  // Auto Increment the id primary key column
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  // The name can't be null
  def name = column[String]("name", O.NotNull)
  def email = column[String]("email", O.NotNull)
  def password = column[String]("password", O.NotNull)
  def failedLoginAttempt = column[Int]("failed_login_attempts", O.NotNull)
  def enabled = column[Boolean]("enabled", O.NotNull)
  
  def * = (id.?,email,password.?,name,failedLoginAttempt,enabled) <> (User.tupled, User.unapply)
  
}

class ApplicationRoleMembershipTable(tag: Tag) extends Table[ApplicationRoleMembership](tag, "application_role_membership") {
  def userId = column[Long]("user_id")
  def roleType = column[String]("role_type", O.NotNull)
  
  def * = (userId,roleType) <> (ApplicationRoleMembership.tupled, ApplicationRoleMembership.unapply)
}


object UsersDAO {
  
    val db = play.api.db.slick.DB
    lazy val usersQuery = TableQuery[UsersTable]
    lazy val rolesQuery = TableQuery[ApplicationRoleMembershipTable]
    
    def allEnabled: List[User] = db.withSession { implicit session =>
        usersQuery.filter(_.enabled === true).sortBy(_.name.asc).list
    }
    
    def allPaged(pageNumber: Int, pageSize: Int): (Seq[User],Int) = db.withSession { implicit session =>
       val offset = (pageNumber -1)*pageSize
       val users = usersQuery.sortBy(_.name.asc).drop(offset).take(pageSize).list
       val totalCount = Query(usersQuery.length).first
       (users,totalCount)
    }
    
    def create(newuser: User):Try[Long] = Try { 
      db.withTransaction{ implicit session =>
        (usersQuery returning usersQuery.map(_.id)) += newuser
      }
    }
    
    def find(userId: Long): User = db.withSession{ implicit session =>
        usersQuery.filter(_.id === userId).first
    }
    
    def findByEmail(email: String): Option[User] = db.withSession{ implicit session =>
        usersQuery.filter(u => u.email.toLowerCase === email.toLowerCase && u.enabled === true).firstOption
    }
    
    def update(updateUser: User):Try[Int] = Try {
        db.withTransaction{ implicit session =>
            usersQuery.filter(_.id === updateUser.id).map(u => (u.name,u.email,u.enabled)).update((updateUser.name,updateUser.email,updateUser.enabled))
        }
    }
    
    def updatefailedLoginAttempt(userId: Long, failedLoginAttempts: Int) = db.withTransaction{ implicit session =>
        usersQuery.filter(_.id === userId).map(u => (u.failedLoginAttempt)).update((failedLoginAttempts))
    }
    
    def enableUser(userId: Long, status: Boolean) = db.withTransaction{ implicit session =>
        usersQuery.filter(_.id === userId).map(u => (u.enabled)).update((status))
    }
    
    def changeUserPassword(userId: Long, newPasswordEncrypted: String) = db.withTransaction{ implicit session =>
        usersQuery.filter(_.id === userId).map(u => (u.password)).update((newPasswordEncrypted))
    }
    
    def delete(id: Long) = Try {db.withTransaction{ implicit session =>
          usersQuery.filter(_.id === id).delete
      }
    }
    
    def searchByNameAndEmail(searchString: String) = db.withSession{ implicit session =>
        usersQuery.filter(u => u.email.toLowerCase like searchString).run
    }
    
    def getRoleMembers(roleType: String) = db.withSession{ implicit session =>
       val innerJoin = for {
         (usr,grp) <- usersQuery innerJoin rolesQuery on (_.id === _.userId) if grp.roleType === roleType
       } yield (grp.userId,usr.name,usr.email,grp.roleType)
       innerJoin.list.map {
         case((userId:Long,userName:String,userEmail:String,roleType:String)) => UserRoleMember(userId,userName,userEmail,roleType)
       }
    }
    
    def getRoleNonMembers(roleType: String) = db.withSession{ implicit session =>
       val roleMembers = rolesQuery.filter { role => role.roleType === roleType }
       val outerJoin = for {
         (usr,grp) <- usersQuery leftJoin roleMembers on (_.id === _.userId) if grp.roleType.?.isEmpty 
       } yield (usr)
       outerJoin.list.map {
         case(user:User) => UserRoleMember(user.id.get,user.name,user.email,roleType)
       }
    }
    
    def deleteRoleMember(id: Long, roleType: String) = db.withTransaction{ implicit session =>
        rolesQuery.filter(grp => grp.userId === id && grp.roleType === roleType).delete
    }
    
    def addRoleMembers(newMembers: Seq[UserRoleMember]):Try[Int] = 
       Try { db.withTransaction{ implicit session =>
        val rowsInserted = rolesQuery ++= newMembers.map { member => ApplicationRoleMembership(member.userId,member.roleType) }
        rowsInserted ifSome { rows =>
          rows
        } otherwise {
          0
        }
       }
    }
    
    def getRoles(userId: Long): Seq[ApplicationRoleMembership] = db.withSession{ implicit session =>
        rolesQuery.filter(u => u.userId === userId).list
    }
}