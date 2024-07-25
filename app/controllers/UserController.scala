package controllers

import javax.inject._
import play.api.mvc._
import daos.UserDAO
import models.User
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
//Need to import Constraints to use Constraints
import play.api.data.validation.Constraints._
import org.mindrot.jbcrypt.BCrypt

@Singleton
class UserController @Inject()(cc: ControllerComponents, userDAO: UserDAO)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val passwordRegex = """^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$""".r

  private val userForm: Form[User] = Form(
    mapping(
      "id" -> optional(longNumber),
      "username" -> nonEmptyText(minLength = 3, maxLength = 20),
      "email" -> email,
      "password" -> nonEmptyText(minLength = 8).verifying(
        pattern(passwordRegex, error = "Password must contain at least one letter and one number"
        ))
    )(User.apply)(User.unapply)
  )

  def showSignUpForm: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.session.get("username") match {
      case Some(_) =>
        Redirect("/")
      case None =>
        Ok(views.html.signup(""))
    }
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

  //Need to test with E2E testing
  def showLogInForm(error: String = ""): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.session.get("username") match {
      case Some(_) =>
        Redirect("/")
      case None =>
        Ok(views.html.login("", error))
    }
  }

  //Need to return with sessions
  def logIn: Action[JsValue] = Action.async(parse.json) { implicit request =>
    (request.body \ "username").asOpt[String].zip((request.body \ "password").asOpt[String]).map {
      case (username, password) =>
        userDAO.findUserByUsername(username).flatMap {
          case Some(user) if BCrypt.checkpw(password, user.password) =>
            Future.successful(Ok(Json.obj("status" -> "success", "message" -> "Logged in")).withSession("username" -> username))
          case _ => Future.successful(Unauthorized(Json.obj("status" -> "error", "message" -> "Invalid credentials")))
        }
    }.getOrElse(Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> "Invalid login data"))))
  }
}

//When dealing with asynchronous code, a future value will eventually hold either Some(user) or None.
//The 'map' method on a 'Future' is used to transform the result of the 'Future' when it completes successfully.
//However, 'map' doesn't handle nested Futures properly which can lead to Future[Future[Result]] rather than
//Future[Result].