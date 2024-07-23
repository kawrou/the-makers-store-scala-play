package controllers

import daos.UserDAO
import models.Users
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Play.materializer
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.JdbcProfile
import slick.lifted
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext

class UserControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

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
