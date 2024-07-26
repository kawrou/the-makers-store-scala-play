package controllers

import models.{Item, User}
import daos.ItemDAO
import play.api.data.Form
import play.api.data.Forms.{bigDecimal, longNumber, mapping, nonEmptyText, number, optional}
import play.api.data.validation.Constraints.pattern
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ItemsController @Inject()(cc: ControllerComponents, itemDAO: ItemDAO)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val itemUpdateForm: Form[Item] = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "price" -> bigDecimal(10, 2).transform[Double](_.toDouble, BigDecimal(_)),
      "description" -> nonEmptyText
    )(Item.apply)(Item.unapply)
  )

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
    itemUpdateForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> "Missing Item data")))
      },
      itemWithData => {
        itemDAO.findItemById(id).flatMap {
          case Some(item) =>
            val updatedItemObj = (Json.toJson(item).as[JsObject] ++ updateData).as[Item]
            itemDAO.updateItemById(id, updatedItemObj).map {
              rows => Ok(Json.obj("status" -> "success", "message" -> s"Item with id: $id updated with $itemWithData. $rows updated"))
            }.recover {
              case e: Exception => InternalServerError(Json.obj("status" -> "error", "message" -> s"Error updating item: $e"))
            }
          case None => Future.successful(NotFound(Json.obj("status" -> "error", "message" -> s"Item with id: $id not found")))
        }.recover {
          case e: Exception => InternalServerError(Json.obj("status" -> "error", "message" -> s"Can't find item: $e"))
        }
      }
    )

    //    val updateData = request.body.as[Item]
    //    itemUpdateForm.bindFromRequest()
    //      .fold(
    //        formWithErrors => {
    //          Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> "Missing Item data")))
    //        },
    //        itemWithData => {
    //          itemDAO.updateItemById(id, itemWithData).map {
    //            rows => Ok(Json.obj("status" -> "success", "message" -> s"Item with id: $id updated. $rows updated"))
    //          }.recover {
    //            case _ => InternalServerError(Json.obj("status" -> "error", "message" -> "Error updating item"))
    //          }
    //        }
    //      )

    //        val updateData = request.body.as[JsObject]
    //        itemDAO.findItemById(id).flatMap {
    //          case Some(item) =>
    //            val updatedItemObj = (Json.toJson(item).as[JsObject] ++ updateData).as[Item]
    //            itemDAO.updateItemById(id, updatedItemObj).map {
    //              rows => Ok(Json.obj("status" -> "success", "message" -> s"Item with id: $id updated"))
    //            }
    //          case None => Future.successful(NotFound(Json.obj("status" -> "error", "message" -> s"Item with id: $id not found")))
    //        }.recover {
    //          case _ => InternalServerError(Json.obj("status" -> "error", "message" -> "Error updating item"))
    //        }
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
