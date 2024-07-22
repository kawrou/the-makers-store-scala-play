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
      "username" -> nonEmptyText(minLength = 3, maxLength = 20),
      "email" -> email,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def showSignUpForm: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.signup(""))
  }

  def signUp: Action[JsValue] = Action.async(parse.json) { implicit request =>
    // Retrieve the data from the JSON
    // Get the username, email, and password
    val json = request.body.as[JsObject]
    val username = (json \ "username").as[String]
    val email = (json \ "email").as[String]
    val password = (json \ "password").as[String]

    userForm.bindFromRequest()
      .fold(
        formWithErrors => {
          // binding failure, you retrieve the form containing errors:
          //    You can't use "Future.failed()" in this block because it returns a Future[Throwable]
          //    which is not the same type expected by the .recover{} below.
          //    This happens because if/else blocks must return the same data type.
          Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> "Missing Sign-up data")))
        },
        userData => {
          /* binding success, you get the actual value. */
          val user = User(None, username, email, password)
          userDAO.addUser(user).map { id =>
            Created(Json.obj("status" -> "success", "message" -> s"User $id created"))
          }.recover {
            case e: IllegalArgumentException => BadRequest(Json.obj("status" -> "error", "message" -> s"${e.getMessage}"))
            case _ => InternalServerError(Json.obj("status" -> "error", "message" -> "User could not be created"))
          }
        }
      )
  }

  def logIn: Action[JsValue] = Action.async(parse.json) { implicit request =>
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
