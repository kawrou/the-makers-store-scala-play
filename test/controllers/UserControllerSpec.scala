package controllers

import daos.UserDAO
import models.Users
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.Play.materializer
import play.api.db.evolutions.Evolutions
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.JdbcProfile
import slick.lifted
import slick.lifted.TableQuery
import play.api.db.{DBApi, Database}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Play, Application}

import scala.concurrent.ExecutionContext


class UserControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with BeforeAndAfterEach {

  // WithApplication - Play Framework utility to manage application lifecycle during testing. Fresh app instance for
  // each test and running the evolutions
  // Play Evolutions - Scripts that manage database schema changes.
  // Slick - Database query and access library.

    def fakeApp(): Application = new GuiceApplicationBuilder().build()
    lazy val database: Database = fakeApp().injector.instanceOf[DBApi].database("default")

    override def beforeEach(): Unit = {
//      Evolutions.cleanupEvolutions(database)
      Evolutions.applyEvolutions(database)
    }

    override def afterEach(): Unit = {
       Evolutions.cleanupEvolutions(database)
      // can put cleanup in here instead but kept in beforeEach so table structure is kept in db
    }

  "UserController POST /signUp" should {

    "create a new user" in {
      //Inject an instance of UserDao to our controller
      val userDAO = inject[UserDAO]
      //Uses something called stubControllerComponents() from Play
      val userController = new UserController(stubControllerComponents(), userDAO)(inject[ExecutionContext])

      val request = FakeRequest(POST, "/signUp")
        .withJsonBody(Json.obj(
          "username" -> "testuser",
          "email" -> "test@example.com",
          "password" -> "Password1")
        )
        .withCSRFToken

      val result = call(userController.signUp, request)

      status(result) mustBe CREATED
      val jsonResponse = contentAsJson(result)
      (jsonResponse \ "status").as[String] mustBe "success"
      (jsonResponse \ "message").as[String] must include("User")

      // Verify user is actually created in the database
      val maybeUser = await(userDAO.findUserByUsername("testuser"))
      maybeUser must not be empty
      maybeUser.get.email mustBe "test@example.com"
    }

    //In order to trigger the .recover block, we need to throw some error in the addUser() method or the "Created".
    //We don't have control over Created, so we only have control of the userDao file and the User model.
    //1. We could return an error when creating a User object.
    //2. We could return an error when addUser()
    //3. Check that the received JSON contains necessary fields before even instantiating the User object

    //Email should match this: .+@example\.com
    //Password: [0-9a-fA-F]{4,8}
    "return bad request for invalid data" in {
      val userDAO = inject[UserDAO]
      val userController = new UserController(stubControllerComponents(), userDAO)(inject[ExecutionContext])

      val request = FakeRequest(POST, "/signUp")
        .withJsonBody(Json.obj(
          "username" -> "",
          "email" -> "not-an-email",
          "password" -> "")
        )
        .withCSRFToken

      val result = call(userController.signUp, request)
      status(result) mustBe BAD_REQUEST
    }
  }
}
