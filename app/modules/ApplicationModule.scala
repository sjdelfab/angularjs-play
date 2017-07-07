package modules

import com.google.inject.name.Named
import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.crypto.{ CookieSigner, Crypter, CrypterAuthenticatorEncoder }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ EventBus, Silhouette, SilhouetteProvider }
import com.mohiva.play.silhouette.api.{Environment => SEnvironment}
import com.mohiva.play.silhouette.crypto.{ JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings }
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.{ DelegableAuthInfoDAO, InMemoryAuthInfoDAO }
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.{ Configuration, Environment }
import services.UserService
import play.api.Logger
import play.api.Play
import services.UserServiceDatabase
import controllers.IndirectReferenceMapper
import controllers.CryptoIndirectReferenceMapper
import security.AuthenticationEnv
import security.UserIdentity
import security.UserIdentityService
import models.User
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.EnumerationReader._
import net.codingwell.scalaguice.ScalaModule
import security.PasswordInfoDAO

class ApplicationModule(environment: Environment, configuration: Configuration) extends AbstractModule with ScalaModule {
  
  def configure() {
      bind(classOf[UserService]).to(classOf[UserServiceDatabase])
      bind(classOf[IndirectReferenceMapper]).to(classOf[CryptoIndirectReferenceMapper])
      bind(classOf[IdentityService[UserIdentity]]).to(classOf[UserIdentityService])
      bind[DelegableAuthInfoDAO[PasswordInfo]].to[PasswordInfoDAO]
      
      bind[Silhouette[AuthenticationEnv]].to[SilhouetteProvider[AuthenticationEnv]]
      bind[CacheLayer].to[PlayCacheLayer]
      bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
      bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
      bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
      bind[EventBus].toInstance(EventBus())      
      bind[Clock].toInstance(Clock())
  }
  
  @Provides
  def provideEnvironment(
    userService: UserIdentityService,
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus): SEnvironment[AuthenticationEnv] = {

    SEnvironment[AuthenticationEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }
  
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }

  @Provides
  def provideAuthInfoRepository(passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo]): AuthInfoRepository = {
    new DelegableAuthInfoRepository(passwordInfoDAO)
  }
  
  @Provides
  def provideAuthenticatorService(
    @Named("authenticator-crypter") crypter: Crypter,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[JWTAuthenticator] = {

    val config = configuration.underlying.as[JWTAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(config, None, encoder, idGenerator, clock)
  }
  
  @Provides
  def providePasswordHasherRegistry(passwordHasher: PasswordHasher): PasswordHasherRegistry = {
    new PasswordHasherRegistry(passwordHasher)
  }
  
  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasherRegistry: PasswordHasherRegistry
  ): CredentialsProvider = {
    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
  }
}