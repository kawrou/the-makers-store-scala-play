package daos

import models.{User, Users}
import org.mindrot.jbcrypt.BCrypt
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDAO @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  //Gets all the users from the DB
  val users = Users.table

  //Method returns a Future which I guess is like Promise
  //It uses the password property of the user to hash pw with Bcrypt
  //Then creates a new user instance shallow copy using Scala's .copy method
  //Replaces the password property value with the new hashed password.
  //Uses slick's run() method to add the new user to the Table
  //It adds to the user table by first retrieving all the users and then mapping with +=

  def addUser(user: User): Future[Long] = {
    val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
    val userWithHashedPassword = user.copy(password = hashedPassword)
    db.run(users returning users.map(_.id) += userWithHashedPassword)
  }

  def findUserByUsername(username: String): Future[Option[User]] = {
    db.run(users.filter(_.username === username).result.headOption)
  }

  private class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def email = column[String]("email")
    def password = column[String]("password")

    def * = (id.?, username, email, password) <> ((User.apply _).tupled, User.unapply)
  }
}
