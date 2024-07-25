package models

import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}

// Item case class
case class Item(id: Option[Long], name: String, price: Double, description: String)

// Companion object for Item
object Item {
  implicit val itemFormat: OFormat[Item] = Json.format[Item]
}

// Items table definition
class Items(tag: Tag) extends Table[Item](tag, "items") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name: Rep[String] = column[String]("name")
  def price: Rep[Double] = column[Double]("price")
  def description: Rep[String] = column[String]("description")

  def * : ProvenShape[Item] = (id.?, name, price, description) <> ((Item.apply _).tupled, Item.unapply)
}

// Companion object for Items table
object Items {
  val table = TableQuery[Items]
}
