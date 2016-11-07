package security

import javax.inject.Inject
import javax.inject.Singleton
import models.User
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import services.UserService

@Singleton
class UserIdentityService @Inject()(userService: UserService) extends IdentityService[UserIdentity] {
  
  def retrieve(loginInfo: LoginInfo): Future[Option[UserIdentity]] = {
    for {
       user <- userService.findByEmail(loginInfo.providerKey)       
       identity <-  {
         user.fold[Future[Option[UserIdentity]]](Future.successful[Option[UserIdentity]](None)) { u =>
           userService.getRoles(u.id.get) map { rs => 
             val roles = rs.map { r => new SecurityRole(r.roleType)}
             Some(UserIdentity(u,Some(roles)))
           }
         }         
       }
    } yield identity
  }
  
}