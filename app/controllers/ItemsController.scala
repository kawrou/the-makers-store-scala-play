package controllers

import models.Item
import daos.ItemDAO
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ItemsController @Inject()(cc: ControllerComponents, itemDAO: ItemDAO)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.items())
  }

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val json = request.body.as[JsObject]
    val name = (json \ "item-name").as[String]
    val price = (json \ "item-price").as[Double]
    val description = (json \ "item-description").as[String]

    val item = Item(None, name, price, description)
    itemDAO.addItem(item).map { id =>
      Created(Json.obj("status" -> "success", "message" -> s"Item $id: $name created"))
    }
  }

  //I need to find the original item with the same Id to check if it exists.
  //If it does exist, I want to merge the update object with the DB object and create a new Item object
  //Then update the DB item by ID using the new Item object

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val updateData = request.body.as[JsObject]

    itemDAO.findItemById(id).flatMap {
      case Some(item) =>
        val updatedItemObj = (Json.toJson(item).as[JsObject] ++ updateData).as[Item]
        itemDAO.updateItemById(id, updatedItemObj).map {
          rows => Ok(Json.obj("status" -> "success", "message" -> s"Item with id: $id updated"))
        }
      case None => Future.successful(NotFound(Json.obj("status" -> "error", "message" -> s"Item with id: $id not found")))
    }.recover {
      case _ => InternalServerError(Json.obj("status" -> "error", "message" -> "Error updating item"))
    }
  }

  def destroy(id: Long): Action[AnyContent] = Action.async { implicit request =>
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
