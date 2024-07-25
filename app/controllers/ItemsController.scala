package controllers

import models.Item
import daos.ItemDAO
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ItemsController @Inject()(cc: ControllerComponents, itemDAO: ItemDAO)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.items())
  }

  def addItem: Action[JsValue] = Action.async(parse.json) { implicit request =>

    val json = request.body.as[JsObject]
    val name = (json \ "item-name").as[String]
    val price = (json \ "item-price").as[Double]
    val description = (json \ "item-description").as[String]

    val item = Item(None, name, price, description)
    itemDAO.addItem(item).map { id =>
      Created(Json.obj("status" -> "success", "message" -> s"Item $id: $name created"))
    }
  }

  def deleteItem(id: Long): Action[AnyContent] = Action.async { implicit request =>
    //    val json = request.body.as[JsObject]
    //    val id = (json \ "item-id").as[Long]

    itemDAO.deleteItem(id).map {
      case i if i == 1 => Ok(Json.obj("status" -> "success", "message" -> s"Item with id: $id deleted"))
      case 0 => NotFound(Json.obj("status" -> "error", "message" -> s"Item with id: $id not found"))
    }.recover {
      case _: Exception => InternalServerError(Json.obj("status" -> "error", "message" -> "Error occurred. Item couldn't be deleted"))
    }
  }
}
