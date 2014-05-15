package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.api.service._
import com.coinport.coinex.api.model._
import com.github.tototoshi.play2.json4s.native.Json4s

object Admin extends Controller with Json4s {

  def index = Action {
    Ok(views.html.index())
  }

  def deposit = Action {
    Ok(views.html.index())
  }

  def transfers(currency: String, uid: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val status = getParam(query, "status").map(s => TransferStatus.get(s.toInt).getOrElse(TransferStatus.Succeeded))
      val types = getParam(query, "type").map(s => TransferType.get(s.toInt).getOrElse(TransferType.Deposit))
      TransferService.getTransfers(Some(uid.toLong), Some(currency), status, None, types, Cursor(0, 100)) map {
        case result =>
          Ok(result.toJson)
      }
  }

  def notifications() = Action {
    implicit request =>
      val result = NotificationService.getNotifications()
      Ok(result.toJson)
  }

  def addNotification() = Action(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val message = ControllerHelper.getParam(data, "message", "")
      val id = System.currentTimeMillis()
      val notification = Notification(id, NotificationType.Warning, message)
      NotificationService.addNotification(notification)
      Ok(ApiResult.toJson)
  }

  def removeNotification(id: String) = Action {
    implicit request =>
      NotificationService.removeNotification(id.toLong)
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
