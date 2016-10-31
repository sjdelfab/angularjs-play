package models

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import controllers.IndirectReferenceMapper

case class ApplicationRoleMembership(userId: Long, roleType: String)

object ApplicationRoleMembership extends ((Long, String) => ApplicationRoleMembership) {
    
  def createJsonWrite(idExternaliser: (Long) => String): Writes[ApplicationRoleMembership] = {      
        val appGroupMembershipToJson: Writes[ApplicationRoleMembership] = (
           (__ \ "userId").write[String] ~
           (__ \ "roleType").write[String] ~
           (__ \ "name").write[String] ~
           (__ \ "description").write[String]
        )((roleMember:ApplicationRoleMembership) => (
        idExternaliser(roleMember.userId),
        roleMember.roleType,
        getRoleName(roleMember.roleType),
        getRoleDescription(roleMember.roleType)))
        
        appGroupMembershipToJson
  }
  
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
  
  def createJsonWrite(idExternaliser: (Long) => String): Writes[UserRoleMember] = {
    val userRoleMemberToJson: Writes[UserRoleMember] = (
        (__ \ "userId").write[String] ~
        (__ \ "name").write[String] ~
        (__ \ "email").write[String] ~
        (__ \ "roleType").write[String]
      )((userRole: UserRoleMember) => (
        idExternaliser(userRole.userId),
        userRole.userName,
        userRole.userEmail,
        userRole.roleType
      ))
      
    userRoleMemberToJson  
  }
  
  def createJsonRead(indirectReferenceMapper: IndirectReferenceMapper): Reads[UserRoleMember] = {
    val userIdValidator = Reads.StringReads.filter(ValidationError("User id is not valid."))(indirectReferenceMapper.getExternalisedId(_).isDefined)
    val jsonToUserRoleMember: Reads[UserRoleMember] = (
      (__ \ "userId").read[String](userIdValidator) and
      (__ \ "name").read[String] and
      (__ \ "email").read[String] and
      (__ \ "roleType").read[String]
     )((userId: String, userName: String, userEmail: String, roleType: String) => 
        UserRoleMember(indirectReferenceMapper.getExternalisedId(userId).get,userName,userEmail,roleType)
     )
     jsonToUserRoleMember
  }

}