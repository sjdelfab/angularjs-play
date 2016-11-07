package security

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import models.User

@RunWith(classOf[JUnitRunner])
class RoleCheckingTest extends Specification {

  val user = User(Some(1),"simon@email.com",None,"Simon",0,true)
  
  "User has no roles" in {     
      val identityUser = new UserIdentity(user)
      identityUser.checkRoles(List(Array("admin"))) must equalTo(false)
  }
  
  "User has wrong roles" in {
      val roles: Seq[SecurityRole] = List(SecurityRole("resource_manager"))
      val identityUser = new UserIdentity(user,Some(roles))
      identityUser.checkRoles(List(Array("admin"))) must equalTo(false)
  }
  
  "User has right roles" in {
      val roles: Seq[SecurityRole] = List(SecurityRole("admin"))
      val identityUser = new UserIdentity(user,Some(roles))
      identityUser.checkRoles(List(Array("admin"))) must equalTo(true)
  }
  
  "User has all roles" in {
      val roles: Seq[SecurityRole] = List(SecurityRole("admin"),SecurityRole("resource_manager"))
      val identityUser = new UserIdentity(user,Some(roles))
      identityUser.checkRoles(List(Array("admin"))) must equalTo(true)
  }
  
  "User has all roles, but can't be resource_manager" in {
      val roles: Seq[SecurityRole] = List(SecurityRole("admin"),SecurityRole("resource_manager"))
      val identityUser = new UserIdentity(user,Some(roles))
      identityUser.checkRoles(List(Array("admin","!resource_manager"))) must equalTo(false)
  }
  
  "User has admin role, but can't be resource_manager" in {
      val roles: Seq[SecurityRole] = List(SecurityRole("admin"))
      val identityUser = new UserIdentity(user,Some(roles))
      identityUser.checkRoles(List(Array("admin","!resource_manager"))) must equalTo(true)
  }
}