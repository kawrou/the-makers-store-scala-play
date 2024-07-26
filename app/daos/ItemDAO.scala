package daos

import models.{Item, Items}
import org.mindrot.jbcrypt.BCrypt
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ItemDAO @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val items = Items.table

  def addItem(item: Item): Future[Long] = {
    db.run(items returning items.map(_.id) += item)
  }

  def findItemByName(itemName: String): Future[Option[Item]] = {
    db.run(items.filter(_.name === itemName).result.headOption)
  }

  def findItemById(itemId: Long): Future[Option[Item]] = {
    db.run(items.filter(_.id === itemId).result.headOption)
  }

  def updateItemById(itemId: Long, item: Item): Future[Int] = {
    db.run(items.filter(_.id === itemId).update(item))
  }

  def deleteItem(itemId: Long): Future[Int] = {
    db.run(items.filter(_.id === itemId).delete)
  }
}
