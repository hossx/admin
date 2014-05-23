package models

import play.api.Play.current

case class User(email: String, name: String, password: String)

object User {
  val preDefinedUser = Seq[User](
    User("admin@coinport.com", "admin", "123456"),
    User("user@coinport.com", "user", "123456"),
    User("developer@coinport.com", "developer", "123456")
  )

  /**
   * Authenticate a User.
   */
  def authenticate(email: String, password: String): Option[User] = {
    preDefinedUser.find(_.email.equals(email)) match {
      case Some(user) => if(user.password.equals(password)) Some(user) else None
      case None => None
    }
  }

}
