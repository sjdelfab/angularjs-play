package security

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import models.ApplicationRoleMembership

@RunWith(classOf[JUnitRunner])
class RoleCheckingTest extends Specification {

  "User has no roles" in {
      val internalUser = new InternalUser("test@email.com", 1l)
      internalUser.checkRoles(List(Array("admin"))) must equalTo(false)
  }
  
  "User has wrong roles" in {
      val grps: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"resource_manager"))
      val internalUser = new InternalUser("test@email.com", 1l,Some(grps))
      internalUser.checkRoles(List(Array("admin"))) must equalTo(false)
  }
  
  "User has right roles" in {
      val grps: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"))
      val internalUser = new InternalUser("test@email.com", 1l,Some(grps))
      internalUser.checkRoles(List(Array("admin"))) must equalTo(true)
  }
  
  "User has all roles" in {
      val grps: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"),ApplicationRoleMembership(1l,"resource_manager"))
      val internalUser = new InternalUser("test@email.com", 1l,Some(grps))
      internalUser.checkRoles(List(Array("admin"))) must equalTo(true)
  }
  
  "User has all roles, but can't be resource_manager" in {
      val grps: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"),ApplicationRoleMembership(1l,"resource_manager"))
      val internalUser = new InternalUser("test@email.com", 1l,Some(grps))
      internalUser.checkRoles(List(Array("admin","!resource_manager"))) must equalTo(false)
  }
  
  "User has admin role, but can't be resource_manager" in {
      val grps: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"))
      val internalUser = new InternalUser("test@email.com", 1l,Some(grps))
      internalUser.checkRoles(List(Array("admin","!resource_manager"))) must equalTo(true)
  }
}