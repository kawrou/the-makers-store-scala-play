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

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { implicit request =>

    def findItem(id: Long) = {
      itemDAO.findItemById(id)
    }

    def updateItem(id: Long, existingItem: Item, itemWithNewData: Item) = {
      val updatedItemObj = mergeItem(existingItem, itemWithNewData)
      itemDAO.updateItemById(id, updatedItemObj).map {
        rows => Ok(Json.obj("status" -> "success", "message" -> s"Item with id: $id updated with. $rows updated"))
      }
    }

    def mergeItem(existingItem: Item, updateItemObj: Item) = {
      existingItem.copy(
        name = updateItemObj.name,
        price = updateItemObj.price,
        description = updateItemObj.description
      )
    }

    itemUpdateForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> s"Missing Item data. $formWithErrors")))
      },
      itemWithNewData => {
        findItem(id).flatMap {
          case Some(existingItem) => updateItem(id, existingItem, itemWithNewData)
          case None => Future.successful(NotFound(Json.obj("status" -> "error", "message" -> s"Item with id: $id not found")))
        }.recover {
          case e: Exception => InternalServerError(Json.obj("status" -> "error", "message" -> s"Error"))
        }
      }
    )
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
