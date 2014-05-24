package models

import play.api.Play.current
import scala.collection.JavaConversions._
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

case class User(email: String, name: String, password: String)

object User {
  val userConfigFile = "users.conf"

  private def loadUserFromConfig(): Seq[User] = {
    val config = ConfigFactory.load(userConfigFile)
    val users = config.getConfigList("users") map {
      u: Config =>
      User(u.getString("email"), u.getString("name"), u.getString("password"))
    }
    users.toSeq
  }

  /**
   * Authenticate a User.
   */
  def authenticate(email: String, password: String): Option[User] = {
    val preDefinedUsers = loadUserFromConfig
    preDefinedUsers.foreach(println)
    preDefinedUsers.find(_.email.equals(email)) match {
      case Some(user) => if(user.password.equals(password)) Some(user) else None
      case None => None
    }
  }

}
