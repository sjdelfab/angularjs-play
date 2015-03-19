package controllers

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer
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
import models.User
import models.User.UserToJson
import models.UserRoleMember
import play.api.Logger
import play.api.Play
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toApplicativeOps
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsError
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
import play.api.libs.json.__
import play.api.mvc.Controller
import play.api.mvc.Result
import scalaext.OptionExt.extendOption
import services.ForeignKeyConstraintViolation
import services.SuccessInsert
import services.SuccessUpdate
import services.UniqueConstraintViolation
import services.UserService
import services.UserSession
import utils.PasswordCrypt
import security.InternalUser
import services.SuccessfulLogin

@Singleton
class Users @Inject() (userSession: UserSession, userService: UserService) extends Controller with Security {
  
  def getUserSession(): UserSession = {
    userSession
  }
  
  def getUserService(): UserService = {
    userService
  }
  
  /** Retrieves a logged in user if the authentication token is valid.
    *
    * If the token is invalid, [[HasToken]] does not invoke this function.
    *
    * @return The user in JSON format.
    */
  def currentLoggedInUser() =  ValidUserAction(parse.empty) { token => sessionUser => implicit request =>
     Ok(Json.toJson(userService.findOneById(sessionUser.userId)))
  }

  def createUser() = Restrict(parse.json)(Array("admin")) { token => sessionUser => implicit request =>
    request.body.validate[User].fold(
        errors => {
          Logger.info("User: " + sessionUser.userEmail + ". Creating User: Invalid JSON request: " + JsError.toFlatJson(errors))
          BadRequest("Invalid request")
        },
        user => {
           user.password ifSome { enteredPassword =>
              if (isPasswordStrongEnough(enteredPassword)) {
                PasswordCrypt.encrypt(enteredPassword) ifSome { encryptedPassword =>
                  createNewUser(sessionUser,user,encryptedPassword)
                } otherwise {
                  Logger.info("User: " + sessionUser.userEmail + ". Create User: Invalid Password")
                  Ok(Json.obj("status" -> "INVALID_PASSWORD"))
                }
              } else {
                Logger.info("User: " + sessionUser.userEmail + ". Create User: Password not strong enough")
                val msg = Play.current.configuration.getString(PASSWORD_POLICY_MESSAGE).get
                Ok(Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> msg))
              }
           } otherwise {
             Logger.info("User: " + sessionUser.userEmail + ". Create User: Invalid Request")
             BadRequest("Invalid request")
           }
        })
  }

  private def createNewUser(sessionUser: InternalUser, user: User, encryptedPassword: String):Result = {
    userService.createUser(user,encryptedPassword) match {
      case SuccessInsert(newUserId) => {
        Logger.info("User: " + sessionUser.userEmail + ". Create User: " + user.email)
        Ok(Json.obj("id" -> IndirectReferenceMapper.convertInternalIdToExternalised(newUserId)))
      }
      case UniqueConstraintViolation() => {
        Logger.info("User: " + sessionUser.userEmail + ". Create User: Failed to create due to unique constraints violation - " + user.email)
        Ok(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
      }
      case _ => {
        Logger.info("User: " + sessionUser.userEmail + ". Create User: Internal Server Error")
        InternalServerError("Failed to update user due to server error")
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
        User.currentUser(IndirectReferenceMapper.getExternalisedId(id), email, name, enabled)
      }
    })

  def updateUser() = Restrict(parse.json)(Array("admin")) { token => sessionUser => implicit request =>
      request.body.validate[User].fold(
        errors => {
          Logger.info("User: " + sessionUser.userEmail + ". Update User: Invalid JSON request: " + JsError.toFlatJson(errors))
          BadRequest("Invalid request")
        },
        user => {
          user.id ifSome { updatedUserId =>
              userService.updateUser(user) match {
                case SuccessUpdate(rowsUpdated) => {
                  Logger.info("User: " + sessionUser.userEmail + ". Update User: " + user.email)
                  Ok(Json.obj("status" -> "OK"))
                }
                case UniqueConstraintViolation() => {
                  Logger.info("User: " + sessionUser.userEmail + ". Create User: Failed to create due to unique constraints violation - " + user.email)
                  Ok(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
                }
                case _ => {
                  Logger.info("User: " + sessionUser.userEmail + ". Update User: Internal Server Error")
                  InternalServerError("Failed to update user due to server error")
                }
              }
          } otherwise {
            Logger.info("User: " + sessionUser.userEmail + ". Update User: Invalid request")
            BadRequest("Invalid request")
          }
        })
  }

  def deleteUser(externalisedUserId: String) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    IndirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      val user = userService.findOneById(internalUserId)
      userService.deleteUser(internalUserId) match {
        case SuccessUpdate(rowsUpdated) => {
            Logger.info("User: " + sessionUser.userEmail + ". Delete User: " + user.email)
            Ok(Json.obj("status" -> "OK")) 
        }
        case ForeignKeyConstraintViolation() => {
            Logger.info("User: " + sessionUser.userEmail + ". Delete User: Failed to delete due to foreign key constraints violation - " + user.email)
            Ok(Json.obj("status" -> "FK_CONSTRAINTS_VIOLATION"))
        }
        case _ => {
            Logger.info("User: " + sessionUser.userEmail + ". Delete User: Internal Server Error")
            InternalServerError("Failed to delete user due to server error")
        }
      }
    } otherwise {
       Logger.info("User: " + sessionUser.userEmail + ". Delete User: Invalid Request")
       BadRequest("Invalid request")
    }
  }
  
  def searchUser(searchString: String) = ValidUserAction(parse.empty) { token => sessionUser => implicit request =>
    val foundUsers = userService.searchUsers(searchString)
    Ok(Json.toJson(foundUsers))
  }

  def enableUser(externalisedUserId: String, status: Boolean) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    IndirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      userService.enableUser(internalUserId,status)
      val user = userService.findOneById(internalUserId)
      Logger.info("User: " + sessionUser.userEmail + ". Enable/Disable User: " + user.email)
      Ok
    } otherwise {
       Logger.info("User: " + sessionUser.userEmail + ". Enable/Disable User: Invalid Request")
       BadRequest("Invalid request")
    }
  }
  
  def unlockUser(externalisedUserId: String) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    IndirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      userService.unlockUser(internalUserId)
      val user = userService.findOneById(internalUserId)
      Logger.info("User: " + sessionUser.userEmail + ". Unlock User: " + user.email)
      Ok
    } otherwise {
       Logger.info("User: " + sessionUser.userEmail + ". Unlock User: Invalid Request")
       BadRequest("Invalid request")
    }
  }
  
  def changeUserPassword(externalisedUserId: String, newPassword: String) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    IndirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      if (isPasswordStrongEnough(newPassword)) {
         PasswordCrypt.encrypt(newPassword) ifSome { encryptedPassword => 
             userService.changeUserPassword(internalUserId,encryptedPassword)
             val user = userService.findOneById(internalUserId)
             Logger.info("User: " + sessionUser.userEmail + ". Change User Password: " + user.email)
             Ok(Json.obj("status" -> "OK"))
         } otherwise {
             Logger.info("User: " + sessionUser.userEmail + ". Change User Password: Invalid Password")
             Ok(Json.obj("status" -> "INVALID_PASSWORD"))
         }
      } else {
         Logger.info("User: " + sessionUser.userEmail + ". Change User Password: Password not strong enough")
         val msg = Play.current.configuration.getString(PASSWORD_POLICY_MESSAGE).get
         Ok(Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH","message" -> msg))
      }
    } otherwise {
       Logger.info("User: " + sessionUser.userEmail + ". Change User Password: Invalid Request")
       BadRequest("Invalid request")
    }
  }
  
  def getUsers(page: Int) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    val pageSize = Play.current.configuration.getInt(PAGE_SIZE).getOrElse(50)
    val (foundUsers,total) = userService.getUsers(page,pageSize)
    val numberOfPages: Int = (total / pageSize) + 1
    val data = Json.obj("users" -> Json.toJson(foundUsers), "total" -> total, "numberOfPages" -> numberOfPages, "pageSize" -> pageSize)
    Ok(data)
  }
  
  def getUser(externalisedUserId: String) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    IndirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      val user = userService.findOneById(internalUserId)
      val roles = userService.getRoles(internalUserId)
      val toReturn = Json.obj("user" -> Json.toJson(user), "roles" -> Json.toJson(roles))
      Ok(toReturn)
    } otherwise {
       Logger.info("User: " + sessionUser.userEmail + ". Get User: Invalid Request")
       BadRequest("Invalid request")
    }
  }

  def getMyProfile() = ValidUserAction(parse.empty) { token => sessionUser => implicit request =>
      val user = userService.findOneById(sessionUser.userId)
      val roles = userService.getRoles(sessionUser.userId)
      val toReturn = Json.obj("user" -> Json.toJson(user), "roles" -> Json.toJson(roles))
      Ok(toReturn)
  }
  
  def updateMyProfile() = ValidUserAction(parse.json) { token => sessionUser => implicit request =>
      request.body.validate[User].fold(
        errors => {
          Logger.info("User: " + sessionUser.userEmail + ". Update User: Invalid JSON request: " + JsError.toFlatJson(errors))
          BadRequest("Invalid request")
        },
        user => {
          user.id ifSome { updatedUserId =>
              var currentUser = userService.findOneById(updatedUserId)
              currentUser.mergeEditableChanges(user)
              userService.updateUser(currentUser) match {
                case SuccessUpdate(rowsUpdated) => {
                  Logger.info("User: " + sessionUser.userEmail + ". Updated Profile.")
                  Ok(Json.obj("status" -> "OK"))
                }
                case _ => {
                  Logger.info("User: " + sessionUser.userEmail + ". Update Profile: Internal Server Error")
                  InternalServerError("Failed to update user profile due to server error")
                }
              }
          } otherwise {
            Logger.info("User: " + sessionUser.userEmail + ". Update User Profile: Invalid request")
            BadRequest("Invalid request")
          }
        })
  }
  
  def changeMyPassword(currentPassword: String, newPassword: String) = ValidUserAction(parse.empty) { token => sessionUser => implicit request =>
    PasswordCrypt.encrypt(currentPassword) ifSome { currentEncryptedPassword =>
      userService.authenticate(sessionUser.userEmail, currentEncryptedPassword) match {
        case SuccessfulLogin(user) => {
             performChangeOwnPassword(newPassword,user)
        }
        case _ => {
           Logger.info("User: " + sessionUser.userEmail + ". Change Own Password: Invalid current password")
           Ok(Json.obj("status" -> "INVALID_CURRENT_PASSWORD"))
        }
      }       
    } otherwise {
       Logger.info("User: " + sessionUser.userEmail + ". Change Own Password: Invalid Request")
       BadRequest("Invalid request")
    }
  }

  private def performChangeOwnPassword(newPassword: String, user: User) = {
    if (isPasswordStrongEnough(newPassword)) {
      PasswordCrypt.encrypt(newPassword) ifSome { encryptedPassword =>
        userService.changeUserPassword(user.id.get, encryptedPassword)
        Logger.info("User: " + user.email + ". Change Own Password.")
        Ok(Json.obj("status" -> "OK"))
      } otherwise {
        Logger.info("User: " + user.email + ". Change Own Password: Invalid Password")
        Ok(Json.obj("status" -> "INVALID_PASSWORD"))
      }
    } else {
      Logger.info("User: " + user.email + ". Change Own Password: Password not strong enough")
      val msg = Play.current.configuration.getString(PASSWORD_POLICY_MESSAGE).get
      Ok(Json.obj("status" -> "PASSWORD_NOT_STRONG_ENOUGH", "message" -> msg))
    }
  }
  
  def getRoleMembers(roleType: String) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    val roleMembers = userService.getRoleMembers(roleType)
    Ok(Json.toJson(roleMembers))
  }
  
  def getRoleNonMembers(roleType: String) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    val roleMembers = userService.getRoleNonMembers(roleType)
    Ok(Json.toJson(roleMembers))
  }
  
  def deleteRoleMember(externalisedUserId: String, roleType: String) = Restrict(parse.empty)(Array("admin")) { token => sessionUser => implicit request =>
    IndirectReferenceMapper.getExternalisedId(externalisedUserId) ifSome { internalUserId=>
      userService.deleteRoleMember(internalUserId,roleType)
      val user = userService.findOneById(internalUserId)
      Logger.info("User: " + sessionUser.userEmail + ". Delete Role: " + roleType + " from " + user.email)
      Ok
    } otherwise {
       Logger.info("User: " + sessionUser.userEmail + ". Delete Role: Invalid Request")
       BadRequest("Invalid request")
    }
  }
  
  def addUsersToRole = Restrict(parse.json)(Array("admin")) { token => sessionUser => implicit request =>
      request.body.validate[Seq[UserRoleMember]].fold(
        errors => {
          Logger.info("User: " + sessionUser.userEmail + ". Add Users To Role: Invalid JSON request: " + JsError.toFlatJson(errors))
          BadRequest("Invalid request")
        },
        newRoleMembers => {
          userService.addRoleMembers(newRoleMembers) match {
              case SuccessUpdate(rowsUpdated) => Ok(Json.obj("status" -> "OK"))
              case UniqueConstraintViolation() => {
                  Logger.info("User: " + sessionUser.userEmail + ". Add Users To Role: Unique constraints violation")
                  Ok(Json.obj("status" -> "UNIQUE_CONSTRAINTS_VIOLATION"))
              }
              case _ => {
                  Logger.info("User: " + sessionUser.userEmail + ". Add Users To Role: Internal Server Error")
                  InternalServerError("Failed to add role members due to server error")
              }
          }
        }
     )
  }
  
  private def isPasswordStrongEnough(newPassword: String): Boolean = {
      val minPasswordLength = Play.current.configuration.getInt(PASSWORD_MINIMUM_PASSWORD_LENGTH).getOrElse(8);
      val lengthRule = new LengthRule(minPasswordLength, 30);
      // control allowed characters
      val charRule = new CharacterCharacteristicsRule();
      var rules = 0;
      if (Play.current.configuration.getBoolean(PASSWORD_MUST_HAVE_1_DIGIT).getOrElse(true)) {
         charRule.getRules().add(new DigitCharacterRule(1));
         rules += 1;
      }
      if (Play.current.configuration.getBoolean(PASSWORD_MUST_HAVE_1_NON_ALPHA).getOrElse(true)) {
         charRule.getRules().add(new NonAlphanumericCharacterRule(1));
         rules += 1;
      }
      if (Play.current.configuration.getBoolean(PASSWORD_MUST_HAVE_1_UPPER_CASE).getOrElse(true)) {
         charRule.getRules().add(new UppercaseCharacterRule(1));
         rules += 1;
      }
      if (Play.current.configuration.getBoolean(PASSWORD_MUST_HAVE_1_LOWER_CASE).getOrElse(true)) {
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