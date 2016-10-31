package services

import javax.inject.Singleton
import play.api.cache._
import security.InternalUser
import models.ApplicationRoleMembership
import javax.inject.Inject

trait UserSession {

  def register(token: String, user: InternalUser)

  def lookup(token: String): Option[InternalUser]
  
  def deregister(token: String)
 
}

@Singleton
class PlayCacheUserSession @Inject() (cache: CacheApi) extends UserSession {
  
  override def register(token: String, user: InternalUser) {
     cache.set(token, user)
  }
  
  override def deregister(token: String) {
	 cache.remove(token)
  }
  
  override def lookup(token: String): Option[InternalUser] = {
     cache.get[InternalUser](token)
  }
}

@Singleton
class DevelopmentUserSession extends UserSession {
  
  override def register(token: String, user: InternalUser) { }
  
  override def deregister(token: String) { }
  
  override def lookup(token: String): Option[InternalUser] = {
     val allRoles: Seq[ApplicationRoleMembership] = List(ApplicationRoleMembership(1l,"admin"),ApplicationRoleMembership(1l,"resource_manager"))
     val user = new InternalUser("simon@email.com",1l,Some(allRoles))
     Some(user)
  }
}