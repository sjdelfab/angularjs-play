package controllers

import com.mohiva.play.silhouette.api.Silhouette

import java.util.concurrent.TimeoutException

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import javax.inject.Inject
import javax.inject.Singleton
import models.ApplicationRoleMembership
import models.User
import models.UserRoleMember
import play.api.Configuration
import play.api.Logger
import play.api.libs.functional.syntax.toApplicativeOps
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads.BooleanReads
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.applicative
import play.api.libs.json.Reads.email
import play.api.libs.json.Reads.functorReads
import play.api.libs.json.Reads.maxLength
import play.api.libs.json.Reads.minLength
import play.api.libs.json.Reads.traversableReads
import play.api.libs.json.Writes
import play.api.libs.json.__
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.Result
import scalaext.OptionExt.extendOption
import services.ForeignKeyConstraintViolation
import services.SuccessInsert
import services.SuccessUpdate
import services.SuccessfulLogin
import services.UniqueConstraintViolation
import services.UserService
import utils.PasswordCrypt
import play.api.libs.json.Reads
import security.AuthenticationEnv
import com.mohiva.play.silhouette.api.util.PasswordHasher
import security.UserIdentity

@Singleton
class Users @Inject() (userService: UserService, 
                       configuration: Configuration,
                       indirectReferenceMapper: IndirectReferenceMapper,
                       val silhouette: Silhouette[AuthenticationEnv],
                       passwordHasher: PasswordHasher) extends Controller with Security with BaseController {
    
  def getUserService(): UserService = userService
  
  def getConfiguration(): Configuration = configuration
  
  def getIndirectReferenceMapper(): IndirectReferenceMapper = indirectReferenceMapper
  
  def currentLoggedInUser() =  SecuredAction.async { implicit request =>
       Future.successful(OkNoCache(Json.toJson(request.identity.user)))
  }
  
  def createUser() = RestrictAsync(parse.json)(Array("admin")) { implicit request =>
    request.body.validate[User].fold(
        errors => {
          UserBadRequest("Creating User", request.identity, errors)
        },
        user => {
           user.password ifSome { enteredPassword =>
              if (isPasswordStrongEnough(enteredPassword)) {
                  val encryptedPassword = passwordHasher.hash(enteredPassword).password
                  createNewUser(request.identity,user,encryptedPassword)
              } else {
                  PasswordNotStringEnoughResult("Create User",request.identity)
              }
           } otherwise {
             InvalidUserRequest("Create User",request.identity)
           }
        })
  }
  
  private def InvalidPasswordResult(operation: String, identityUser: UserIdentity): Future[Result] = {
    Logger.info(s"User: identityUser.user.email . $operation: Invalid Password")
    Future.successful(OkNoCache(Json.obj("status" -> "INVALID_PASSWORD")))
  }

  private def PasswordNotStringEnoughResult(operation: String, identityUser: UserIdentity): Future[Result] = {
    Logger.info(s"User: ${identityUser.user.email}. $operation: Password not strong enough")
    val msg = configuration.getString(PASSWORD_POLICY_MESSAGE).get
    Future.successful(OkNoCache(Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> msg)))
  }
  
  private def createNewUser(identityUser: UserIdentity, user: User, encryptedPassword: String): Future[Result] = {
    userService.createUser(user,encryptedPassword) map { result =>
      result match {
        case SuccessInsert(newUserId) => {
          Logger.info(s"User: ${identityUser.user.email}. Create User: ${user.email}")
          OkNoCache(Json.obj("id" -> indirectReferenceMapper.convertInternalIdToExternalised(newUserId)))
        }
        case UniqueConstraintViolation() => {
          Logger.info(s"User: ${identityUser.user.email}. Create User: Failed to create due to unique constraints violation - ${user.email}")
          OkNoCache(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
        }
        case _ => {
          Logger.info(s"User: ${identityUser.user.email}. Create User: Internal Server Error")
          InternalServerError("Failed to update user due to server error")
        }
      }
    }
  }
  
  implicit val UserFromJson = (
      (__ \ "id").read[String] ~
      (__ \ "name").read[String](minLength[String](1) keepAnd maxLength[String](254)) ~
      (__ \ "email").read[String](email keepAnd maxLength[String](254)) ~
      (__ \ "enabled").read[Boolean] ~
      (__ \ "password").readNullable[String]
    )((id, name, email, enabled, password) => {
      if (id == "new") {
        User.newUser(email, name, enabled, password)
      } else {
        User.currentUser(indirectReferenceMapper.getExternalisedId(id), email, name, enabled)
      }
    })

  def updateUser() = RestrictAsync(parse.json)(Array("admin")) { implicit request =>
      request.body.validate[User].fold(
        errors => {
          UserBadRequest("Update User", request.identity, errors)
        },
        user => {
          user.id ifSome { updatedUserId =>
              userService.updateUser(user) map { result => result match {
                  case SuccessUpdate(rowsUpdated) => {
                    Logger.info(s"User: ${request.identity.user.email}. Update User: ${user.email}")
                    OkNoCache(Json.obj("status" -> "OK"))
                  }
                  case UniqueConstraintViolation() => {
                    Logger.info(s"User: ${request.identity.user.email}. Create User: Failed to create due to unique constraints violation - ${user.email}")
                    OkNoCache(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
                  }
                  case _ => {
                    Logger.info(s"User: ${request.identity.user.email}. Update User: Internal Server Error")
                    InternalServerError("Failed to update user due to server error")
                  }
                }
              }
          } otherwise {
            InvalidUserRequest("Update User",request.identity)
          }
        })
  }

  def deleteUser(externalisedUserId: String) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId =>
      for {
        user <- userService.findOneById(internalUserId)
        updateResult <- 
            userService.deleteUser(internalUserId) map { result => 
              result match {
                  case SuccessUpdate(rowsUpdated) => {
                      Logger.info(s"User: ${request.identity.user.email}. Delete User: ${user.email}")
                      OkNoCache(Json.obj("status" -> "OK")) 
                  }
                  case ForeignKeyConstraintViolation() => {
                      Logger.info(s"User: ${request.identity.user.email}. Delete User: Failed to delete due to foreign key constraints violation - ${user.email}")
                      OkNoCache(Json.obj("status" -> "FK_CONSTRAINTS_VIOLATION"))
                  }
                  case _ => {
                      Logger.info(s"User: ${request.identity.user.email}. Delete User: Internal Server Error")
                      InternalServerError("Failed to delete user due to server error")
                  }
              }
            }
      } yield updateResult
    } otherwise {
      InvalidUserRequest("Delete User",request.identity)
    }
  }
  
  def searchUser(searchString: String) = SecuredAction.async(parse.empty) { implicit request =>
    userService.searchUsers(searchString) map { foundUsers => 
       OkNoCache(Json.toJson(foundUsers))
    }
  }

  def enableUser(externalisedUserId: String, status: Boolean) = RestrictAsync(parse.empty)(Array("admin")) { request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      for {
        user <- userService.findOneById(internalUserId)
        updateResult <- 
          userService.enableUser(internalUserId,status) map { result =>      
             Logger.info(s"User: ${request.identity.user.email}. Enable/Disable User: ${user.email}")
             OkNoCache
          }
      } yield updateResult
    } otherwise {
       InvalidUserRequest("Enable/Disable User",request.identity)
    }
  }
  
  def unlockUser(externalisedUserId: String) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      for {
        user <- userService.findOneById(internalUserId)
        updateResult <- 
           userService.unlockUser(internalUserId) map { result =>
             Logger.info(s"User: ${request.identity.user.email}. Unlock User: ${user.email}")
             OkNoCache 
           }        
      } yield updateResult
    } otherwise {
      InvalidUserRequest("Unlock User",request.identity)
    }
  }
  
  def changeUserPassword(externalisedUserId: String, newPassword: String) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      if (isPasswordStrongEnough(newPassword)) {
         val passwordInfo = passwordHasher.hash(newPassword)
         for {
            user <- userService.findOneById(internalUserId)
            updateResult <- 
                  userService.changeUserPassword(internalUserId,passwordInfo.password) map { result =>  
                  Logger.info(s"User: ${request.identity.user.email}. Change User Password: ${user.email}")
                  OkNoCache(Json.obj("status" -> "OK")) 
            }
               
         } yield updateResult
      } else {
         PasswordNotStringEnoughResult("Change User Password",request.identity)
      }
    } otherwise {
       InvalidUserRequest("Change User Password",request.identity)
    }
  }
  
  def getUsers(page: Int) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    val pageSize = configuration.getInt(PAGE_SIZE).getOrElse(50)
    for {
      foundUsers <- userService.getUsers(page,pageSize)
      result <- {
        userService.getTotalUserCount() map { total =>
            val numberOfPages: Int = (total / pageSize) + 1
            val data = Json.obj("users" -> Json.toJson(foundUsers), "total" -> total, "numberOfPages" -> numberOfPages, "pageSize" -> pageSize)
            OkNoCache(data)      
        }        
      }
    } yield result
  }
  
  implicit val appGroupMembershipToJson: Writes[ApplicationRoleMembership] = ApplicationRoleMembership.createJsonWrite(id => indirectReferenceMapper.convertInternalIdToExternalised(id))
  
  def getUser(externalisedUserId: String) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      val actions = for {
        user <- userService.findOneById(internalUserId)
        result <- {
          userService.getRoles(internalUserId) map { roles =>
             val toReturn = Json.obj("user" -> Json.toJson(user), "roles" -> Json.toJson(roles))
             OkNoCache(toReturn)   
          }
        }
      } yield result
      actions
    } otherwise {
      InvalidUserRequest("Get User",request.identity)
    }
  }

  def getMyProfile() = SecuredAction.async { implicit request =>
      val actions = for {
        user <- userService.findOneById(request.identity.user.id.get)
        result <- {
          userService.getRoles(request.identity.user.id.get) map { roles =>
             val toReturn = Json.obj("user" -> Json.toJson(user), "roles" -> Json.toJson(roles))
             OkNoCache(toReturn)   
          }
        }
      } yield result
      actions
  }
  
  def updateMyProfile() = SecuredAction.async(parse.json) { implicit request =>
      request.body.validate[User].fold(
        errors => {
          UserBadRequest("Update my profile", request.identity, errors)
        },
        user => {
          user.id ifSome { updatedUserId =>
              val actions = for {
                currentUser <- userService.findOneById(updatedUserId)
                updateResult <- {
                    currentUser.mergeEditableChanges(user)
                    userService.updateUser(currentUser) map { updateDbResult => updateDbResult match {
                          case SuccessUpdate(rowsUpdated) => {
                            Logger.info(s"User: ${request.identity.user.email}. Updated Profile.")
                            OkNoCache(Json.obj("status" -> "OK"))
                          }
                          case _ => {
                            Logger.info(s"User: ${request.identity.user.email}. Update Profile: Internal Server Error")
                            InternalServerError("Failed to update user profile due to server error")
                          }
                       }
                    }
                  }
              } yield updateResult
              actions
          } otherwise {
            InvalidUserRequest("Update User Profile",request.identity)
          }
        })
  }
    
  implicit val userRoleMemberToJson: Writes[UserRoleMember] = UserRoleMember.createJsonWrite(id => indirectReferenceMapper.convertInternalIdToExternalised(id))
  
  def getRoleMembers(roleType: String) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    userService.getRoleMembers(roleType) map { roleMembers =>
        OkNoCache(Json.toJson(roleMembers))
    }
  }
  
  def getRoleNonMembers(roleType: String) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    userService.getRoleNonMembers(roleType) map { nonRoleMembers =>
        OkNoCache(Json.toJson(nonRoleMembers))
    }
  }
  
  def deleteRoleMember(externalisedUserId: String, roleType: String) = RestrictAsync(parse.empty)(Array("admin")) { implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      userService.deleteRoleMember(internalUserId,roleType)
      val userFuture = userService.findOneById(internalUserId)
      userFuture.map { user =>
          Logger.info(s"User: ${request.identity.user.email}. Delete Role: $roleType from ${user.email}")
          OkNoCache
      }
    } otherwise {
       InvalidUserRequest("Delete Role",request.identity)
    }
  }
  
  implicit val jsonToUserRoleMember: Reads[UserRoleMember] = UserRoleMember.createJsonRead(indirectReferenceMapper)
  
  def addUsersToRole = RestrictAsync(parse.json)(Array("admin")) { implicit request =>
      request.body.validate[Seq[UserRoleMember]].fold(
        errors => {
          UserBadRequest("Add Users To Role", request.identity, errors)
        },
        newRoleMembers => {
          userService.addRoleMembers(newRoleMembers) map { dbResult => 
              dbResult match {
                  case SuccessUpdate(rowsUpdated) => Ok(Json.obj("status" -> "OK"))
                  case UniqueConstraintViolation() => {
                      Logger.info(s"User: ${request.identity.user.email}. Add Users To Role: Unique constraints violation")
                      OkNoCache(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
                  }
                  case _ => {
                      Logger.info(s"User: ${request.identity.user.email}. Add Users To Role: Internal Server Error")
                      InternalServerError("Failed to add role members due to server error")
                  }
              }
           }
        }
     )
  }
  
}