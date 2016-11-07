package security

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.User

case class UserIdentity(user: User, val roles: Option[Seq[SecurityRole]] = None) extends Identity {
 
  def loginInfo: LoginInfo = LoginInfo(CredentialsProvider.ID,user.email)
  
  def isValidUser = user.enabled && user.password.isDefined
  
  def checkRoles(roleGroups: List[Array[String]]): Boolean = {
    check(roleGroups.head, roleGroups.tail)
  }

  private def check(current: Array[String], remaining: List[Array[String]]): Boolean = {
    if (checkRole(current)) true
    else if (remaining.isEmpty) false
    else check(remaining.head, remaining.tail)
  }
  
  private def checkRole(roleNames: Seq[String]): Boolean = {
     hasAllRoles(roleNames)
  }

  /**
   * Gets the role name of each role held.
   */
  private def getRoleNames(): Seq[String] = {
     roles.fold(Seq.empty[String]){ r => r.map { _.getName }}
  }

  /**
   * Check if the subject has the given role.
   */
  private def hasRole(roleName: String) = {
      getRoleNames().contains(roleName)
  }

  private def hasAllRoles(roleNames: Seq[String]): Boolean = {
    val heldRoles = getRoleNames()
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