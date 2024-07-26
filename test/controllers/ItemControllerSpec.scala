package controllers

import daos.ItemDAO
import models.Items
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.Play.materializer
import play.api.db.evolutions.Evolutions
import play.api.db.{DBApi, Database}
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

  "ItemController POST / create" should {
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

        val result = call(itemsController.create, request)

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

  "ItemsController DElETE / destroy" should {
    "return success" when {
      "deleting an item" in {
        val itemDAO = inject[ItemDAO]
        val itemsController = new ItemsController(stubControllerComponents(), itemDAO)(inject[ExecutionContext])

        val request = FakeRequest(DELETE, "/items/1").withCSRFToken

        val result = call(itemsController.destroy(1), request)

        status(result) mustBe OK
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "status").as[String] mustBe "success"
        (jsonResponse \ "message").as[String] must include("Item with id: 1 deleted")
      }
    }
  }

  "ItemsController PATCH / updateItem" should {
    "return success" when {
      "updating an item" in {
        val itemDAO = inject[ItemDAO]
        val itemsController = new ItemsController(stubControllerComponents(), itemDAO)(inject[ExecutionContext])

        val request = FakeRequest(PATCH, "/items/1")
          .withJsonBody(Json.obj(
          "name" -> "Makers T-shirt",
          "price" -> 15.00,
          "description" -> "A lovely T-shirt from Makers")
        ).withCSRFToken

        val result = call(itemsController.update(1), request)

        status(result) mustBe OK
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "status").as[String] mustBe "success"
        (jsonResponse \ "message").as[String] must include("Item with id: 1 updated")

        val maybeItem = await(itemDAO.findItemByName("Makers T-shirt"))
        maybeItem must not be empty
        maybeItem.get.price mustBe 15.00
      }
    }
  }
}
