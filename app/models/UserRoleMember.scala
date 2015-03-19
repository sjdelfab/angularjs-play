package models

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ApplicationRoleMembership(userId: Long, roleType: String)

object ApplicationRoleMembership extends ((Long, String) => ApplicationRoleMembership) {
  
  implicit val appGroupMembershipToJson: Writes[ApplicationRoleMembership] = (
     (__ \ "userId").write[String] ~
     (__ \ "roleType").write[String] ~
     (__ \ "name").write[String] ~
     (__ \ "description").write[String]
  )((roleMember:ApplicationRoleMembership) => (
        controllers.IndirectReferenceMapper.convertInternalIdToExternalised(roleMember.userId),
        roleMember.roleType,
        getRoleName(roleMember.roleType),
        getRoleDescription(roleMember.roleType)))
  
  private def getRoleName(roleType: String) = {
    roleType match {
      case "admin" => "Administrator"
      case "resource_manager" => "Resource Manager"
      case _ => throw new RuntimeException("Unknown role type")  
    }
  }   
     
  private def getRoleDescription(roleType: String) = {
    roleType match {
      case "admin" => "Manage users and roles"
      case "resource_manager" => "Resource manager role"
      case _ => throw new RuntimeException("Unknown role type")  
    }
  }   
     
}

case class UserRoleMember(userId: Long, userName: String, userEmail: String, roleType: String)

object UserRoleMember extends((Long,String,String,String) => UserRoleMember) {
    
  implicit val userRoleMemberToJson: Writes[UserRoleMember] = (
    (__ \ "userId").write[String] ~
    (__ \ "name").write[String] ~
    (__ \ "email").write[String] ~
    (__ \ "roleType").write[String]
  )((userRole: UserRoleMember) => (
    controllers.IndirectReferenceMapper.convertInternalIdToExternalised(userRole.userId),
    userRole.userName,
    userRole.userEmail,
    userRole.roleType
  ))

  val userIdValidator = Reads.StringReads.filter(ValidationError("User id is not valid."))(controllers.IndirectReferenceMapper.getExternalisedId(_).isDefined)
    
  implicit val jsonToUserRoleMember: Reads[UserRoleMember] = (
      (__ \ "userId").read[String](userIdValidator) and
      (__ \ "name").read[String] and
      (__ \ "email").read[String] and
      (__ \ "roleType").read[String]
  )((userId: String, userName: String, userEmail: String, roleType: String) => 
     UserRoleMember(controllers.IndirectReferenceMapper.getExternalisedId(userId).get,userName,userEmail,roleType))
}