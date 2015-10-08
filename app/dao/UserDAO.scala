package dao

import java.util.Date
import models.ApplicationRoleMembership
import models.User
import models.UserRoleMember
import scala.util.Try
import slick.driver.PostgresDriver.api._
import scalaext.OptionExt._
import play.api.Play
import scala.concurrent.Future
import slick.jdbc.GetResult


class UsersTable(tag: Tag) extends Table[User](tag, "application_user") {
  // Auto Increment the id primary key column
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  // The name can't be null
  def name = column[String]("name")
  def email = column[String]("email")
  def password = column[String]("password")
  def failedLoginAttempt = column[Int]("failed_login_attempts")
  def enabled = column[Boolean]("enabled")
  
  def * = (id.?,email,password.?,name,failedLoginAttempt,enabled) <> (User.tupled, User.unapply)
  
}

class ApplicationRoleMembershipTable(tag: Tag) extends Table[ApplicationRoleMembership](tag, "application_role_membership") {
  def userId = column[Long]("user_id")
  def roleType = column[String]("role_type")
  
  def * = (userId,roleType) <> (ApplicationRoleMembership.tupled, ApplicationRoleMembership.unapply)
}


object UsersDAO {
  
    lazy val usersQuery = TableQuery[UsersTable]
    lazy val rolesQuery = TableQuery[ApplicationRoleMembershipTable]
    
    def allEnabled = {
        usersQuery.filter(_.enabled === true).sortBy(_.name.asc)
    }
    
    def allPaged(pageNumber: Int, pageSize: Int) = {
       val offset = (pageNumber -1)*pageSize
       val query = usersQuery.sortBy(_.name.asc).drop(offset).take(pageSize)
       query.result
    }
    
    def getTotalUserCount() = {
       usersQuery.length.result
    }
    
    def create(newuser: User) =  {
        usersQuery returning usersQuery.map(_.id) += newuser        
    }

    def find(userId: Long) = { 
        usersQuery.filter(_.id === userId).result.head
    }
    
    
    def findByEmail(email: String) = {
        val q = usersQuery.filter(u => u.email.toLowerCase === email.toLowerCase && u.enabled === true)
        q.result.headOption
    }
    
    def update(updateUser: User) = { 
        usersQuery.filter(_.id === updateUser.id).map(u => (u.name,u.email,u.enabled)).update((updateUser.name,updateUser.email,updateUser.enabled))
    }

    def updatefailedLoginAttempt(userId: Long, failedLoginAttempts: Int) = {
        usersQuery.filter(_.id === userId).map(u => (u.failedLoginAttempt)).update((failedLoginAttempts))
    }

    def enableUser(userId: Long, status: Boolean) = {
        usersQuery.filter(_.id === userId).map(u => (u.enabled)).update((status))
    }

    def changeUserPassword(userId: Long, newPasswordEncrypted: String) = {
        usersQuery.filter(_.id === userId).map(u => (u.password)).update((newPasswordEncrypted))
    }

    def delete(id: Long) = {
        usersQuery.filter(_.id === id).delete
    }

    def searchByNameAndEmail(searchString: String) = {
        usersQuery.filter(u => u.email.toLowerCase like searchString)        
    }
    
    def getRoleMembers(roleType: String) = {
       val innerJoin = for {
         (usr,grp) <- usersQuery join rolesQuery on (_.id === _.userId) if grp.roleType === roleType
       } yield (grp.userId,usr.name,usr.email,grp.roleType)
       innerJoin
    }
    
    /**
       Sometime it is just too hard to get ORMs to work and plain SQL is necessary
       val roleMembers = rolesQuery.filter { role => role.roleType === roleType }  
       val outerJoin = for {
         (usr,grp) <- usersQuery joinLeft roleMembers on (_.id === _.userId) if grp.isEmpty 
       } yield (usr)  
       Await.result(db.run(outerJoin.result), Duration.Inf).map {
         case(user:User) => UserRoleMember(user.id.get,user.name,user.email,roleType)
       } 
      
     */
    
    implicit val getUserRoleMemberResult = GetResult( r => UserRoleMember(r.nextInt, r.nextString, r.nextString, r.nextString))
    
    def getRoleNonMembers(roleType: String) = {
       val query = sql"""select usr.id, usr.name, usr.email, r.role_type 
       from application_user usr
       left outer join application_role_membership r on usr.id = r.user_id and r.role_type=$roleType
       where r.role_type is null""".as[UserRoleMember]
       query
    }
    
    def deleteRoleMember(id: Long, roleType: String) = {
        rolesQuery.filter(grp => grp.userId === id && grp.roleType === roleType).delete        
    }

    def addRoleMembers(newMembers: Seq[UserRoleMember]) = {
        rolesQuery ++= newMembers.map { member => ApplicationRoleMembership(member.userId,member.roleType) }              
    }

    def getRoles(userId: Long) = {
        rolesQuery.filter(u => u.userId === userId)        
    }

}