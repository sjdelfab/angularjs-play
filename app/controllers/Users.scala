package controllers

import java.util.concurrent.TimeoutException

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import edu.vt.middleware.password.AlphabeticalSequenceRule
import edu.vt.middleware.password.CharacterCharacteristicsRule
import edu.vt.middleware.password.DigitCharacterRule
import edu.vt.middleware.password.LengthRule
import edu.vt.middleware.password.LowercaseCharacterRule
import edu.vt.middleware.password.NonAlphanumericCharacterRule
import edu.vt.middleware.password.NumericalSequenceRule
import edu.vt.middleware.password.Password
import edu.vt.middleware.password.PasswordData
import edu.vt.middleware.password.PasswordValidator
import edu.vt.middleware.password.QwertySequenceRule
import edu.vt.middleware.password.RepeatCharacterRegexRule
import edu.vt.middleware.password.Rule
import edu.vt.middleware.password.UppercaseCharacterRule
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
import security.InternalUser
import services.ForeignKeyConstraintViolation
import services.SuccessInsert
import services.SuccessUpdate
import services.SuccessfulLogin
import services.UniqueConstraintViolation
import services.UserService
import services.UserSession
import utils.PasswordCrypt
import play.api.libs.json.Reads

@Singleton
class Users @Inject() (userSession: UserSession, 
                       userService: UserService, 
                       configuration: Configuration,
                       indirectReferenceMapper: IndirectReferenceMapper) extends Controller with Security with BaseController {
  
  def getUserSession(): UserSession = userSession
  
  def getUserService(): UserService = userService
  
  def getConfiguration(): Configuration = configuration
  
  def getIndirectReferenceMapper(): IndirectReferenceMapper = indirectReferenceMapper
  
  /** Retrieves a logged in user if the authentication token is valid.
    *
    * @return The user in JSON format.
    */
  def currentLoggedInUser() =  ValidUserAsyncAction(parse.empty) { token => sessionUser => implicit request =>
    userService.findOneById(sessionUser.userId).map { user =>
       OkNoCache(Json.toJson(user))
    } .recover {
      case ex: TimeoutException =>
        Logger.error("Problem found in employee delete process")
        InternalServerError(ex.getMessage)
     }
  }
  
  def createUser() = RestrictAsync(parse.json)(Array("admin")) { token => sessionUser => implicit request =>
    request.body.validate[User].fold(
        errors => {
          UserBadRequest("Creating User", sessionUser, errors)
        },
        user => {
           user.password ifSome { enteredPassword =>
              if (isPasswordStrongEnough(enteredPassword)) {
                PasswordCrypt.encrypt(enteredPassword) ifSome { encryptedPassword =>
                  createNewUser(sessionUser,user,encryptedPassword)
                } otherwise {
                  InvalidPasswordResult("Create User",sessionUser)
                }
              } else {
                  PasswordNotStringEnoughResult("Create User",sessionUser)
              }
           } otherwise {
             InvalidUserRequest("Create User",sessionUser)
           }
        })
  }
  
  private def InvalidPasswordResult(operation: String, sessionUser: InternalUser): Future[Result] = {
    Logger.info(s"User: $sessionUser.userEmail . $operation: Invalid Password")
    Future{OkNoCache(Json.obj("status" -> "INVALID_PASSWORD"))}
  }

  private def PasswordNotStringEnoughResult(operation: String, sessionUser: InternalUser): Future[Result] = {
    Logger.info(s"User: ${sessionUser.userEmail}. $operation: Password not strong enough")
    val msg = configuration.getString(PASSWORD_POLICY_MESSAGE).get
    Future{OkNoCache(Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> msg))}
  }
  
  private def createNewUser(sessionUser: InternalUser, user: User, encryptedPassword: String):Future[Result] = {
    userService.createUser(user,encryptedPassword) map { result =>
      result match {
        case SuccessInsert(newUserId) => {
          Logger.info(s"User: ${sessionUser.userEmail}. Create User: ${user.email}")
          OkNoCache(Json.obj("id" -> indirectReferenceMapper.convertInternalIdToExternalised(newUserId)))
        }
        case UniqueConstraintViolation() => {
          Logger.info(s"User: ${sessionUser.userEmail}. Create User: Failed to create due to unique constraints violation - ${user.email}")
          OkNoCache(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
        }
        case _ => {
          Logger.info(s"User: ${sessionUser.userEmail}. Create User: Internal Server Error")
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

  def updateUser() = RestrictAsync(parse.json)(Array("admin")) { token => sessionUser => implicit request =>
      request.body.validate[User].fold(
        errors => {
          UserBadRequest("Update User", sessionUser, errors)
        },
        user => {
          user.id ifSome { updatedUserId =>
              userService.updateUser(user) map { result => result match {
                  case SuccessUpdate(rowsUpdated) => {
                    Logger.info(s"User: ${sessionUser.userEmail}. Update User: ${user.email}")
                    OkNoCache(Json.obj("status" -> "OK"))
                  }
                  case UniqueConstraintViolation() => {
                    Logger.info(s"User: ${sessionUser.userEmail}. Create User: Failed to create due to unique constraints violation - ${user.email}")
                    OkNoCache(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
                  }
                  case _ => {
                    Logger.info(s"User: ${sessionUser.userEmail}. Update User: Internal Server Error")
                    InternalServerError("Failed to update user due to server error")
                  }
                }
              }
          } otherwise {
            InvalidUserRequest("Update User",sessionUser)
          }
        })
  }

  def deleteUser(externalisedUserId: String) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId =>
      val actions = for {
        user <- userService.findOneById(internalUserId)
        updateResult <- 
            userService.deleteUser(internalUserId) map { result => 
              result match {
                  case SuccessUpdate(rowsUpdated) => {
                      Logger.info(s"User: ${sessionUser.userEmail}. Delete User: ${user.email}")
                      OkNoCache(Json.obj("status" -> "OK")) 
                  }
                  case ForeignKeyConstraintViolation() => {
                      Logger.info(s"User: ${sessionUser.userEmail}. Delete User: Failed to delete due to foreign key constraints violation - ${user.email}")
                      OkNoCache(Json.obj("status" -> "FK_CONSTRAINTS_VIOLATION"))
                  }
                  case _ => {
                      Logger.info(s"User: ${sessionUser.userEmail}. Delete User: Internal Server Error")
                      InternalServerError("Failed to delete user due to server error")
                  }
              }
            }
      } yield updateResult
      actions
    } otherwise {
      InvalidUserRequest("Delete User",sessionUser)
    }
  }
  
  def searchUser(searchString: String) = ValidUserAsyncAction(parse.empty) { token => sessionUser => implicit request =>
    userService.searchUsers(searchString) map { foundUsers => 
       OkNoCache(Json.toJson(foundUsers))
    }
  }

  def enableUser(externalisedUserId: String, status: Boolean) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      val actions = for {
        user <- userService.findOneById(internalUserId)
        updateResult <- 
          userService.enableUser(internalUserId,status) map { result =>      
             Logger.info(s"User: ${sessionUser.userEmail}. Enable/Disable User: ${user.email}")
             OkNoCache
          }
      } yield updateResult
      actions
    } otherwise {
       InvalidUserRequest("Enable/Disable User",sessionUser)
    }
  }
  
  def unlockUser(externalisedUserId: String) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      val actions = for {
        user <- userService.findOneById(internalUserId)
        updateResult <- 
           userService.unlockUser(internalUserId) map { result =>
             Logger.info(s"User: ${sessionUser.userEmail}. Unlock User: ${user.email}")
             OkNoCache 
           }        
      } yield updateResult
      actions
    } otherwise {
      InvalidUserRequest("Unlock User",sessionUser)
    }
  }
  
  def changeUserPassword(externalisedUserId: String, newPassword: String) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      if (isPasswordStrongEnough(newPassword)) {
         PasswordCrypt.encrypt(newPassword) ifSome { encryptedPassword => 
             val actions = for {
               user <- userService.findOneById(internalUserId)
               updateResult <- 
                  userService.changeUserPassword(internalUserId,encryptedPassword) map { result =>  
                     Logger.info(s"User: ${sessionUser.userEmail}. Change User Password: ${user.email}")
                     OkNoCache(Json.obj("status" -> "OK")) 
                  }
               
             } yield updateResult
             actions
         } otherwise {
             InvalidPasswordResult("Change User Password",sessionUser)
         }
      } else {
         PasswordNotStringEnoughResult("Change User Password",sessionUser)
      }
    } otherwise {
       InvalidUserRequest("Change User Password",sessionUser)
    }
  }
  
  def getUsers(page: Int) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    val pageSize = configuration.getInt(PAGE_SIZE).getOrElse(50)
    val actions = for {
      foundUsers <- userService.getUsers(page,pageSize)
      result <- {
        userService.getTotalUserCount() map { total =>
            val numberOfPages: Int = (total / pageSize) + 1
            val data = Json.obj("users" -> Json.toJson(foundUsers), "total" -> total, "numberOfPages" -> numberOfPages, "pageSize" -> pageSize)
            OkNoCache(data)      
        }        
      }
    } yield result
    actions    
  }
  
  implicit val appGroupMembershipToJson: Writes[ApplicationRoleMembership] = ApplicationRoleMembership.createJsonWrite(id => indirectReferenceMapper.convertInternalIdToExternalised(id))
  
  def getUser(externalisedUserId: String) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
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
      InvalidUserRequest("Get User",sessionUser)
    }
  }

  def getMyProfile() = ValidUserAsyncAction(parse.empty) { token => sessionUser => implicit request =>
      val actions = for {
        user <- userService.findOneById(sessionUser.userId)
        result <- {
          userService.getRoles(sessionUser.userId) map { roles =>
             val toReturn = Json.obj("user" -> Json.toJson(user), "roles" -> Json.toJson(roles))
             OkNoCache(toReturn)   
          }
        }
      } yield result
      actions
  }
  
  def updateMyProfile() = ValidUserAsyncAction(parse.json) { token => sessionUser => implicit request =>
      request.body.validate[User].fold(
        errors => {
          UserBadRequest("Update my profile", sessionUser, errors)
        },
        user => {
          user.id ifSome { updatedUserId =>
              val actions = for {
                currentUser <- userService.findOneById(updatedUserId)
                updateResult <- {
                    currentUser.mergeEditableChanges(user)
                    userService.updateUser(currentUser) map { updateDbResult => updateDbResult match {
                          case SuccessUpdate(rowsUpdated) => {
                            Logger.info(s"User: ${sessionUser.userEmail}. Updated Profile.")
                            OkNoCache(Json.obj("status" -> "OK"))
                          }
                          case _ => {
                            Logger.info(s"User: ${sessionUser.userEmail}. Update Profile: Internal Server Error")
                            InternalServerError("Failed to update user profile due to server error")
                          }
                       }
                    }
                  }
              } yield updateResult
              actions
          } otherwise {
            InvalidUserRequest("Update User Profile",sessionUser)
          }
        })
  }
  
  def changeMyPassword(currentPassword: String, newPassword: String) = ValidUserAsyncAction(parse.empty) { token => sessionUser => implicit request =>
    PasswordCrypt.encrypt(currentPassword) ifSome { currentEncryptedPassword =>
      userService.authenticate(sessionUser.userEmail, currentEncryptedPassword) map { loginResult =>
          loginResult match {
            case SuccessfulLogin(user) => {
                 performChangeOwnPassword(newPassword,user)
            }
            case _ => {
               Logger.info(s"User: ${sessionUser.userEmail}. Change Own Password: Invalid current password")
               OkNoCache(Json.obj("status" -> "INVALID_CURRENT_PASSWORD"))
            }
          }
      }
    } otherwise {
        InvalidUserRequest("Change Own Password",sessionUser)
    }
  }

  private def performChangeOwnPassword(newPassword: String, user: User) = {
    if (isPasswordStrongEnough(newPassword)) {
      PasswordCrypt.encrypt(newPassword) ifSome { encryptedPassword =>
        userService.changeUserPassword(user.id.get, encryptedPassword)
        Logger.info(s"User: ${user.email}. Change Own Password.")
        OkNoCache(Json.obj("status" -> "OK"))
      } otherwise {
        Logger.info(s"User: ${user.email}. Change Own Password: Invalid Password")
        OkNoCache(Json.obj("status" -> "INVALID_PASSWORD"))
      }
    } else {
      Logger.info(s"User: ${user.email}. Change Own Password: Password not strong enough")
      val msg = configuration.getString(PASSWORD_POLICY_MESSAGE).get
      OkNoCache(Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> msg))
    }
  }
  
  implicit val userRoleMemberToJson: Writes[UserRoleMember] = UserRoleMember.createJsonWrite(id => indirectReferenceMapper.convertInternalIdToExternalised(id))
  
  def getRoleMembers(roleType: String) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    userService.getRoleMembers(roleType) map { roleMembers =>
        OkNoCache(Json.toJson(roleMembers))
    }
  }
  
  def getRoleNonMembers(roleType: String) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    userService.getRoleNonMembers(roleType) map { nonRoleMembers =>
        OkNoCache(Json.toJson(nonRoleMembers))
    }
  }
  
  def deleteRoleMember(externalisedUserId: String, roleType: String) = RestrictAsync(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    indirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      userService.deleteRoleMember(internalUserId,roleType)
      val userFuture = userService.findOneById(sessionUser.userId)
      userFuture.map { user =>
          Logger.info(s"User: ${sessionUser.userEmail}. Delete Role: $roleType from ${user.email}")
          OkNoCache
      }
    } otherwise {
       InvalidUserRequest("Delete Role",sessionUser)
    }
  }
  
  implicit val jsonToUserRoleMember: Reads[UserRoleMember] = UserRoleMember.createJsonRead(indirectReferenceMapper)
  
  def addUsersToRole = RestrictAsync(parse.json)(Array("admin")) { token => sessionUser => implicit request =>
      request.body.validate[Seq[UserRoleMember]].fold(
        errors => {
          UserBadRequest("Add Users To Role", sessionUser, errors)
        },
        newRoleMembers => {
          userService.addRoleMembers(newRoleMembers) map { dbResult => 
              dbResult match {
                  case SuccessUpdate(rowsUpdated) => Ok(Json.obj("status" -> "OK"))
                  case UniqueConstraintViolation() => {
                      Logger.info(s"User: ${sessionUser.userEmail}. Add Users To Role: Unique constraints violation")
                      OkNoCache(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
                  }
                  case _ => {
                      Logger.info(s"User: ${sessionUser.userEmail}. Add Users To Role: Internal Server Error")
                      InternalServerError("Failed to add role members due to server error")
                  }
              }
           }
        }
     )
  }
  
  private def isPasswordStrongEnough(newPassword: String): Boolean = {
      val minPasswordLength = configuration.getInt(PASSWORD_MINIMUM_PASSWORD_LENGTH).getOrElse(8);
      val lengthRule = new LengthRule(minPasswordLength, 30);
      // control allowed characters
      val charRule = new CharacterCharacteristicsRule();
      var rules = 0;
      if (configuration.getBoolean(PASSWORD_MUST_HAVE_1_DIGIT).getOrElse(true)) {
         charRule.getRules().add(new DigitCharacterRule(1));
         rules += 1;
      }
      if (configuration.getBoolean(PASSWORD_MUST_HAVE_1_NON_ALPHA).getOrElse(true)) {
         charRule.getRules().add(new NonAlphanumericCharacterRule(1));
         rules += 1;
      }
      if (configuration.getBoolean(PASSWORD_MUST_HAVE_1_UPPER_CASE).getOrElse(true)) {
         charRule.getRules().add(new UppercaseCharacterRule(1));
         rules += 1;
      }
      if (configuration.getBoolean(PASSWORD_MUST_HAVE_1_LOWER_CASE).getOrElse(true)) {
         charRule.getRules().add(new LowercaseCharacterRule(1));
         rules += 1;
      }
      charRule.setNumberOfCharacteristics(rules);

      // These rules will always apply don't allow alphabetical sequences
      val alphaSeqRule = new AlphabeticalSequenceRule();
      // don't allow numerical sequences of length 3
      val numSeqRule = new NumericalSequenceRule(3, false);
      // don't allow qwerty sequences
      val qwertySeqRule = new QwertySequenceRule();
      // don't allow 4 repeat characters
      val repeatRule = new RepeatCharacterRegexRule(4);

      // group all rules together in a List
      var ruleList = new ArrayBuffer[Rule]();
      ruleList += lengthRule;
      ruleList += charRule;
      ruleList += alphaSeqRule;
      ruleList += numSeqRule;
      ruleList += qwertySeqRule;
      ruleList += repeatRule;

      import scala.collection.JavaConversions.bufferAsJavaList
      val validator = new PasswordValidator(ruleList);
      val passwordData = new PasswordData(new Password(newPassword));

      val result = validator.validate(passwordData);
      result.isValid();
   }
}