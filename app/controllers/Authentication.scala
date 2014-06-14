package controllers

import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play

trait AuthenticateHelper {
  val ajaxRequestHeaderKey = "ajaxRequestKey"
  val ajaxRequestHeadervalue = "value"
  val cookieNameTimestamp = "COOKIE_TIMESTAMP"

  val sysConfig = Play.current.configuration
  val timeoutMinutes: Int = sysConfig.getInt("session.timout.minutes").getOrElse(60)
  val timeoutMillis: Long = timeoutMinutes * 60 * 1000

  def responseOnRequestHeader[A](request: Request[A], msg: String): Future[Result] = {
    val ajaxRequestHeader = request.headers.get(ajaxRequestHeaderKey).getOrElse("")
    if (ajaxRequestHeadervalue.equals(ajaxRequestHeader)) {
      Future(Unauthorized)
    } else {
      Future (
        Redirect(routes.Admin.login).withNewSession.flashing(
          "success" -> msg
        ).withCookies(Cookie(cookieNameTimestamp, System.currentTimeMillis.toString))
      )
    }
  }
}

object Authenticated extends ActionBuilder[Request] with AuthenticateHelper {

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    // check login and session timeout here:
    request.session.get("email").map { email =>
      val currTs = System.currentTimeMillis
      request.cookies.get(cookieNameTimestamp).map {
        tsCookie =>
        println(s"timestamp cookie: $tsCookie, currtime: $currTs, timeoutMillis: $timeoutMillis")
        val ts = tsCookie.value.toLong
        if (currTs - ts > timeoutMillis) {
          responseOnRequestHeader(request, "session timeout, please relogin")
        } else {
          block(request).map(_.withCookies(
            Cookie(cookieNameTimestamp, currTs.toString)))
        }
      } getOrElse {
        block(request).map(_.withCookies(
          Cookie(cookieNameTimestamp, currTs.toString)))
      }
    } getOrElse {
      responseOnRequestHeader(request, "please login first")
    }
  }
}
