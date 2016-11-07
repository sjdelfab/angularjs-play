package controllers

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
import models.User
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.JsError
import play.api.libs.json.JsPath
import play.api.libs.json.Writes
import play.api.mvc.Controller
import play.api.mvc.Result
import security.UserIdentity
import play.api.Configuration

trait BaseController { self: Controller { 
                               def getConfiguration(): Configuration
                               def getIndirectReferenceMapper(): IndirectReferenceMapper
                             } =>
  
  protected def UserBadRequest(operation: String, identity: UserIdentity, errors: Seq[(JsPath, Seq[ValidationError])]): Future[Result] = {    
      Future{
        Logger.info(s"User: ${identity.user.email}. $operation: Invalid JSON request: " + JsError.toJson(errors))
        BadRequest("Invalid request").withHeaders(PRAGMA -> "no-cache", CACHE_CONTROL -> "no-cache, no-store, must-revalidate", "Expires" -> "-1")
      }
  }
  
  protected def InvalidUserRequest(operation: String, identity: UserIdentity): Future[Result] = {    
      Future{
        Logger.info(s"User: ${identity.user.email}. $operation: Invalid request.")
        BadRequest("Invalid request").withHeaders(PRAGMA -> "no-cache", CACHE_CONTROL -> "no-cache, no-store, must-revalidate", "Expires" -> "-1")
      }
  }
  
  protected def OkNoCache = {
    val status = new Status(OK)
    status.withHeaders(PRAGMA -> "no-cache", CACHE_CONTROL -> "no-cache, no-store, must-revalidate", "Expires" -> "-1")
    status
  }
  
  implicit val userToJsonWrites: Writes[User] = User.createJsonWrite(getConfiguration.getInt(controllers.MAX_FAILED_LOGIN_ATTEMPTS).getOrElse(3), 
                                                                     user => getIndirectReferenceMapper().convertInternalIdToExternalised(user.id.get))
                                                                     
  def isPasswordStrongEnough(newPassword: String): Boolean = {
      val minPasswordLength = getConfiguration.getInt(PASSWORD_MINIMUM_PASSWORD_LENGTH).getOrElse(8);
      val lengthRule = new LengthRule(minPasswordLength, 30);
      // control allowed characters
      val charRule = new CharacterCharacteristicsRule();
      var rules = 0;
      if (getConfiguration.getBoolean(PASSWORD_MUST_HAVE_1_DIGIT).getOrElse(true)) {
         charRule.getRules().add(new DigitCharacterRule(1));
         rules += 1;
      }
      if (getConfiguration.getBoolean(PASSWORD_MUST_HAVE_1_NON_ALPHA).getOrElse(true)) {
         charRule.getRules().add(new NonAlphanumericCharacterRule(1));
         rules += 1;
      }
      if (getConfiguration.getBoolean(PASSWORD_MUST_HAVE_1_UPPER_CASE).getOrElse(true)) {
         charRule.getRules().add(new UppercaseCharacterRule(1));
         rules += 1;
      }
      if (getConfiguration.getBoolean(PASSWORD_MUST_HAVE_1_LOWER_CASE).getOrElse(true)) {
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