package security

import javax.inject.Inject
import javax.inject.Singleton
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import services.UserService
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import org.mindrot.jbcrypt.BCrypt

@Singleton
class PasswordInfoDAO @Inject()(userService: UserService) extends DelegableAuthInfoDAO[PasswordInfo] {
  
  def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    update(loginInfo, authInfo)

  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    userService.findByEmail(loginInfo.providerKey).map {
      case Some(user) if user.enabled && user.password.isDefined => Some(PasswordInfo(BCryptPasswordHasher.ID, user.password.get, salt = Some(BCrypt.gensalt(10))))
      case _ => None
    }

  def remove(loginInfo: LoginInfo): Future[Unit] = {
    Future.successful({})
  }

  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    Future.successful({throw new Exception("PasswordInfoDAO - save : not supported")})
  }

  def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
     Future.successful({throw new Exception("PasswordInfoDAO - update : not supported")})
  }

 
}