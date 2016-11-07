package controllers

import scala.reflect.runtime.universe

import org.specs2.mock.Mockito
import org.specs2.specification.Scope

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.test.FakeEnvironment

import net.codingwell.scalaguice.ScalaModule
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule.fromGuiceModule
import play.api.inject.guice.GuiceableModule.fromPlayBinding
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import security.AuthenticationEnv
import security.UserIdentity
import services.UserService
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import scala.concurrent.Future
import play.api.mvc.Result
import com.mohiva.play.silhouette.test._

trait TestScope extends Scope with Mockito with org.specs2.matcher.MustThrownExpectations {

  class FakeModule extends AbstractModule with ScalaModule {
    def configure() = {
      bind[Environment[AuthenticationEnv]].toInstance(env)
    }
  }

  def identity: UserIdentity

  val userService = mock[UserService]

  def overrideConfig: Map[String, Any] = Map.empty 
  
  implicit val env: Environment[AuthenticationEnv] = new FakeEnvironment[AuthenticationEnv](Seq(identity.loginInfo -> identity))

  lazy val application = new GuiceApplicationBuilder().configure(overrideConfig).overrides(new FakeModule()).overrides(bind[UserService].toInstance(userService)).build

  def executeUserJsonOperation(app: play.api.Application, jsonRequest: JsValue)(op: Users => FakeRequest[JsValue] => Future[Result]): Future[Result] = {
    val usersController = app.injector.instanceOf[Users]
    val request = FakeRequest().withBody(jsonRequest).withAuthenticator[AuthenticationEnv](identity.loginInfo)
    op(usersController)(request)
  }

  def executeUserOperation(app: play.api.Application)(op: Users => FakeRequest[Unit] => Future[Result]): Future[Result] = {
    val usersController = app.injector.instanceOf[Users]
    val request = FakeRequest().withBody((): Unit).withAuthenticator[AuthenticationEnv](identity.loginInfo)
    op(usersController)(request)
  }
  
  def executeApplicationOperation(app: play.api.Application)(op: Application => FakeRequest[Unit] => Future[Result]): Future[Result] = {
    val appController = app.injector.instanceOf[Application]
    val request = FakeRequest().withBody((): Unit).withAuthenticator[AuthenticationEnv](identity.loginInfo)
    op(appController)(request)
  }
}