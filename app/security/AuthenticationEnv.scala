package security

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User

trait AuthenticationEnv extends Env {
  type I = UserIdentity
  type A = JWTAuthenticator
}