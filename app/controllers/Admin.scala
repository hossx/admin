package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.api.service._
import com.coinport.coinex.api.model._
import com.github.tototoshi.play2.json4s.native.Json4s
import models.{User => AdminUser}

object Admin extends Controller with Json4s {
  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => AdminUser.authenticate(email, password).isDefined
    })
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Admin.index).withSession("email" -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Admin.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def index = Authenticated {
    implicit request =>
    Ok(views.html.index()(session))
  }

  def deposit = Authenticated {
    implicit request =>
    Ok(views.html.index()(session))
  }

  def transfers(currency: String, uid: String) = Authenticated.async {
    implicit request =>
      val query = request.queryString
      val status = getParam(query, "status").map(s => TransferStatus.get(s.toInt).getOrElse(TransferStatus.Succeeded))
      val types = getParam(query, "type").map(s => TransferType.get(s.toInt).getOrElse(TransferType.Deposit))
      TransferService.getTransfers(Some(uid.toLong), Some(currency), status, None, types, Cursor(0, 100)) map {
        case result =>
          Ok(result.toJson)
      }
  }

  def getNotifications() = Authenticated.async {
    implicit request =>
      val skip = 0
      val limit = 100
      NotificationService.adminGetNotifications(None, None, None, Cursor(skip, limit)) map {
        case rv =>
        Ok(rv.toJson)
      }
  }

  def setNotification() = Authenticated(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val n = Notification(
        id = ControllerHelper.getParam(data, "id", "0").toLong,
        nType = NotificationType.valueOf(ControllerHelper.getParam(data, "ntype", "Info")).getOrElse(NotificationType.Info),
        title = ControllerHelper.getParam(data, "title", ""),
        content = ControllerHelper.getParam(data, "content", ""),
        removed = ControllerHelper.getParam(data, "removed", "false").toBoolean,
        created = ControllerHelper.getParam(data, "created", System.currentTimeMillis().toString).toLong,
        updated = System.currentTimeMillis())

      NotificationService.updateNotification(n)
      Ok(ApiResult.toJson)
  }

  private def getParam(queryString: Map[String, Seq[String]], param: String): Option[String] = {
    queryString.get(param).map(_(0))
  }

  private def getParam(queryString: Map[String, Seq[String]], param: String, default: String): String = {
    queryString.get(param) match {
      case Some(seq) =>
        if (seq.isEmpty) default else seq(0)
      case None =>
        default
    }
  }
}
