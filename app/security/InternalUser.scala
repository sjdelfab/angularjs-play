package security

import play.libs.Scala
import scalaext.OptionExt._
import models.ApplicationRoleMembership

class InternalUser(val userEmail: String, val userId: Long, val roles: Option[Seq[ApplicationRoleMembership]] = None) extends Subject
{
  
  var securityRoles = Seq[SecurityRole]()
  if (roles.isDefined) {
    securityRoles = roles.get.map{role => new SecurityRole(role.roleType)}
  }
  
  def getRoles: Seq[SecurityRole] = {
      securityRoles
  }
 
  def getIdentifier: String = userEmail
  
  def checkRoles(roleGroups: List[Array[String]]): Boolean = {
    check(roleGroups.head, roleGroups.tail)
  }

  private def check(current: Array[String], remaining: List[Array[String]]): Boolean = {
    if (checkRole(this, current)) true
    else if (remaining.isEmpty) false
    else check(remaining.head, remaining.tail)
  }
  
  private def checkRole(subject: Subject, roleNames: Seq[String]): Boolean = {
     hasAllRoles(subject, roleNames)
  }

  /**
   * Gets the role name of each role held.
   */
  private def getRoleNames(subject: Subject): Seq[String] = {
     if (subject != null) {
          subject.getRoles.map { _.getName }
     } else {
       Array[String]()
     }
  }

  /**
   * Check if the subject has the given role.
   */
  private def hasRole(subject: Subject, roleName: String) = {
      getRoleNames(subject).contains(roleName)
  }

  /**
   * Check if the {@link Subject} has all the roles given in the roleNames array.  Note that while a Subject must
   * have all the roles, it may also have other roles.     
   */
  private def hasAllRoles(subject: Subject, roleNames: Seq[String]): Boolean = {
    val heldRoles = getRoleNames(subject)
    var roleCheckResult = roleNames.length > 0
    for (roleRule <- roleNames if roleCheckResult) {
      var roleName = roleRule
      var invert = false;
      if (roleName.startsWith("!")) {
        invert = true;
        roleName = roleName.substring(1);
      }
      roleCheckResult = heldRoles.contains(roleName);
      if (invert) {
        roleCheckResult = !roleCheckResult;
      }
    }
    roleCheckResult;
  }
}