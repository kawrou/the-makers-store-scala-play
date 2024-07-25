package controllers

import daos.ItemDAO
import models.Items

import org.scalatest.BeforeAndAfterEach
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

class ItemControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {


  "ItemController POST / addItem" should {
    "return success" when {
      "creating a new item" in {
        val itemDAO = inject[ItemDAO]
        val itemsController = new ItemsController(stubControllerComponents(), itemDAO)(inject[ExecutionContext])

        val request = FakeRequest(POST, "/addItem")
          .withJsonBody(Json.obj(
            "item-name" -> "testItem",
            "item-price" -> 10.00,
            "item-description" -> "text description")
          )
          .withCSRFToken

        val result = call(itemsController.addItem, request)

        status(result) mustBe CREATED
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "status").as[String] mustBe "success"
        (jsonResponse \ "message").as[String] must include("Item")

        val maybeItem = await(itemDAO.findItemByName("testItem"))
        maybeItem must not be empty
        maybeItem.get.price mustBe 10.00
      }
    }
  }
}
