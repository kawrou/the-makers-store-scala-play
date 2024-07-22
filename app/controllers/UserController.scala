package controllers

import javax.inject._
import play.api.mvc._
import daos.UserDAO
import models.User
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import org.mindrot.jbcrypt.BCrypt

@Singleton
class UserController @Inject()(cc: ControllerComponents, userDAO: UserDAO)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  val userForm: Form[User] = Form(
    mapping(
      "id" -> optional(longNumber),
      "username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def showSignUpForm = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.signup(""))
  }

  def signUp = Action.async(parse.json) { implicit request =>
    // Retrieve the data from the JSON
    // Get the username, email, and password
    val json = request.body.as[JsObject]
    val username = (json \ "username").as[String]
    val email = (json \ "email").as[String]
    val password = (json \ "password").as[String]

    if (username.isEmpty || email.isEmpty || password.isEmpty) {
      Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> "Invalid Input Data")))
    } else {
      // Use that data to create a new user object
      val user = User(None, username, email, password)

      // Doing something with the DAO, I suspect its related to slick and how it communicates with the DB
      // Call the addUser method and then map the result? using the 'Created' method from the Play framework
      // IDE says Created does "Generates a ‘201 CREATED’ result."
      // .recover seems similar to the 'catch' block found in other languages
      // uses "case _" to match anything "throwable" from the "future" and sends it to the InternalServerError
      // "InternalServerError" seems to be a method from the Play framework
      // IDE says "Generates a ‘500 INTERNAL_SERVER_ERROR’ result."
      userDAO.addUser(user).map { id =>
        Created(Json.obj("status" -> "success", "message" -> s"User $id created"))
      }.recover {
        case _ => InternalServerError(Json.obj("status" -> "error", "message" -> "User could not be created"))
      }
    }
  }

  def logIn = Action.async(parse.json) { implicit request =>
    (request.body \ "username").asOpt[String].zip((request.body \ "password").asOpt[String]).map {
      case (username, password) =>
        userDAO.findUserByUsername(username).map {
          case Some(user) if BCrypt.checkpw(password, user.password) =>
            Ok(Json.obj("status" -> "success", "message" -> "Logged in"))
          case _ => Unauthorized(Json.obj("status" -> "error", "message" -> "Invalid credentials"))
        }
    }.getOrElse(Future.successful(BadRequest("Invalid login data")))
  }
}
