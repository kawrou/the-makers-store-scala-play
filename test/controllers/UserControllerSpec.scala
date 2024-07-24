package controllers

import daos.UserDAO
import models.Users
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Play.materializer
import play.api.db.evolutions.Evolutions
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.JdbcProfile
import slick.lifted
import slick.lifted.TableQuery
import play.api.db.{DBApi, Database}

import scala.concurrent.ExecutionContext


class UserControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

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

  //Might need to update the 1.sql Evolution to add user data for testing
  //Or we can use beforeAll and afterAll so that data from the previous test can be used
  //Or we just create a user within the test and then test logIn
  "User Controller POST / logIn" should {
    "return success" when {
      "user credentials are correct" in {
        val userDAO = inject[UserDAO]
        val userController = new UserController(stubControllerComponents(), userDAO)(inject[ExecutionContext])

        val signUpRequest = FakeRequest(POST, "/signUp")
          .withJsonBody(Json.obj(
            "username" -> "testuser",
            "email" -> "test@example.com",
            "password" -> "Password1")
          )
          .withCSRFToken

        val logInRequest = FakeRequest(POST, "/logIn")
          .withJsonBody(Json.obj(
            "username" -> "testuser",
            "password" -> "Password1")
          )
          .withCSRFToken

        val signUpResult = call(userController.signUp, signUpRequest)

        status(signUpResult) mustBe CREATED

        val result = call(userController.logIn, logInRequest)

        status(result) mustBe OK
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "status").as[String] mustBe "success"
        (jsonResponse \ "message").as[String] must include("Logged in")
      }
    }
    "return error" when {
      "user credentials are wrong" in {
        val userDAO = inject[UserDAO]
        val userController = new UserController(stubControllerComponents(), userDAO)(inject[ExecutionContext])

        val signUpRequest = FakeRequest(POST, "/signUp")
          .withJsonBody(Json.obj(
            "username" -> "testuser",
            "email" -> "test@example.com",
            "password" -> "Password1")
          )
          .withCSRFToken

        val logInRequest = FakeRequest(POST, "/logIn")
          .withJsonBody(Json.obj(
            "username" -> "testuser",
            "password" -> "Password2")
          )
          .withCSRFToken

        val signUpResult = call(userController.signUp, signUpRequest)

        status(signUpResult) mustBe CREATED

        val result = call(userController.logIn, logInRequest)

        status(result) mustBe UNAUTHORIZED
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "status").as[String] mustBe "error"
        (jsonResponse \ "message").as[String] must include("Invalid credentials")
      }
    }
    "return bad request" when {
      "user credentials contain invalid types" in {
        val userDAO = inject[UserDAO]
        val userController = new UserController(stubControllerComponents(), userDAO)(inject[ExecutionContext])

        val signUpRequest = FakeRequest(POST, "/signUp")
          .withJsonBody(Json.obj(
            "username" -> "testuser",
            "email" -> "test@example.com",
            "password" -> "Password1")
          )
          .withCSRFToken

        val logInRequest = FakeRequest(POST, "/logIn")
          .withJsonBody(Json.obj(
            "username" -> "testuser2",
            "password" -> 1)
          )
          .withCSRFToken

        val signUpResult = call(userController.signUp, signUpRequest)

        status(signUpResult) mustBe CREATED

        val result = call(userController.logIn, logInRequest)

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
