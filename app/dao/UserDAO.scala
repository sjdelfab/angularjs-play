package dao

import java.util.Date
import models.ApplicationRoleMembership
import models.User
import models.UserRoleMember
import scala.util.Try
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaext.OptionExt._
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
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
  
    val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
    lazy val usersQuery = TableQuery[UsersTable]
    lazy val rolesQuery = TableQuery[ApplicationRoleMembershipTable]
    
    def allEnabled: List[User] = {
        val query  = usersQuery.filter(_.enabled === true).sortBy(_.name.asc)
        Await.result(db.run(query.result), Duration.Inf).toSet[User].toList
    }
    
    def allPaged(pageNumber: Int, pageSize: Int): (Seq[User],Int) = {
       val offset = (pageNumber -1)*pageSize
       val query = usersQuery.sortBy(_.name.asc).drop(offset).take(pageSize)
       val users = Await.result(db.run(query.result), Duration.Inf).toSet[User].toList
       val totalCount = Await.result(db.run(usersQuery.length.result),Duration.Inf)
       (users,totalCount)
    }
    
    def create(newuser: User):Try[Long] =  {
        val query = usersQuery returning usersQuery.map(_.id) += newuser
        Await.result(db.run(query.asTry.transactionally),Duration.Inf)
    }

    def find(userId: Long): User = { 
        Await.result(db.run(usersQuery.filter(_.id === userId).result.head),Duration.Inf)
    }
    
    
    def findByEmail(email: String): Option[User] = {
        val q = usersQuery.filter(u => u.email.toLowerCase === email.toLowerCase && u.enabled === true)
        Await.result(db.run(q.result.headOption),Duration.Inf)
    }
    
    def update(updateUser: User):Try[Int] = { 
        val q = usersQuery.filter(_.id === updateUser.id).map(u => (u.name,u.email,u.enabled)).update((updateUser.name,updateUser.email,updateUser.enabled))
        Await.result(db.run(q.asTry.transactionally),Duration.Inf)        
    }

    def updatefailedLoginAttempt(userId: Long, failedLoginAttempts: Int) = {
        val q = usersQuery.filter(_.id === userId).map(u => (u.failedLoginAttempt)).update((failedLoginAttempts))
        Await.result(db.run(q.transactionally),Duration.Inf)
    }

    def enableUser(userId: Long, status: Boolean) = {
        val q = usersQuery.filter(_.id === userId).map(u => (u.enabled)).update((status))
        Await.result(db.run(q.transactionally),Duration.Inf)
    }

    def changeUserPassword(userId: Long, newPasswordEncrypted: String) = {
        val q = usersQuery.filter(_.id === userId).map(u => (u.password)).update((newPasswordEncrypted))
        Await.result(db.run(q.transactionally),Duration.Inf)
    }

    def delete(id: Long) = {
        val q = usersQuery.filter(_.id === id).delete
        Await.result(db.run(q.asTry.transactionally),Duration.Inf)
    }

    def searchByNameAndEmail(searchString: String) = {
        val query = usersQuery.filter(u => u.email.toLowerCase like searchString)
        Await.result(db.run(query.result), Duration.Inf).toSet[User].toList
    }

    
    def getRoleMembers(roleType: String) = {
       val innerJoin = for {
         (usr,grp) <- usersQuery join rolesQuery on (_.id === _.userId) if grp.roleType === roleType
       } yield (grp.userId,usr.name,usr.email,grp.roleType)
       Await.result(db.run(innerJoin.result), Duration.Inf).map {
         case((userId:Long,userName:String,userEmail:String,roleType:String)) => UserRoleMember(userId,userName,userEmail,roleType)
       }
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
       Await.result(db.run(query),Duration.Inf)
    }
    
    def deleteRoleMember(id: Long, roleType: String) = {
        val q = rolesQuery.filter(grp => grp.userId === id && grp.roleType === roleType).delete
        Await.result(db.run(q.asTry.transactionally),Duration.Inf)
    }

    def addRoleMembers(newMembers: Seq[UserRoleMember]):Try[Int] = Try {
        val q = rolesQuery ++= newMembers.map { member => ApplicationRoleMembership(member.userId,member.roleType) }
        val rowsInserted = Await.result(db.run(q.asTry.transactionally),Duration.Inf).get
        rowsInserted ifSome { rows =>
          rows
        } otherwise {
          0
        }       
    }

    def getRoles(userId: Long): Seq[ApplicationRoleMembership] = {
        val query = rolesQuery.filter(u => u.userId === userId)
        Await.result(db.run(query.result), Duration.Inf).toSet[ApplicationRoleMembership].toList
    }

}