package controllers

import _root_.play.api.mvc.Action
import _root_.play.api.mvc.BodyParsers.parse
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
import com.coinport.coinex.api.service.BitwayService
import com.coinport.coinex.data.CryptoCurrencyAddressType
import scala.concurrent.Future
import com.coinport.coinex.api.service.AccountService

object Admin extends Controller with Json4s {
  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying("Invalid email or password", result => result match {
      case (email, password) => AdminUser.authenticate(email, password).isDefined
    })
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
      Ok(views.html.login(loginForm))
  }

  def authenticate = Action.async(parse.urlFormEncoded) { implicit request =>
      val data = request.body
      val email       = getParam(data, "email").getOrElse("")
      val password    = getParam(data, "password").getOrElse("")
      val emailUuid   = getParam(data, "emailuuid").getOrElse("")
      val emailCode   = getParam(data, "emailcode").getOrElse("")
      val verifiedEmail = getParam(data, "verifiedemail").getOrElse("")

      ControllerHelper.validateParamsAndThen(
        new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, true, emailUuid, emailCode),
        new PasswordValidator(email, password)) {
          ControllerHelper.popCachedValue(emailUuid)
          Future(ApiResult(true, ErrorCode.Ok.value, "succeed", None))
        } map { result =>
            if (result.success) {
              Ok(result.toJson).withSession("email" -> email, "ve" -> verifiedEmail)
            } else {
              Ok(result.toJson)
            }
        }
  }

  def logout = Action {
    Redirect(routes.Admin.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def index = Authenticated {
    implicit request =>
      Ok(views.html.index()(request.session))
  }

  def deposit = Authenticated {
    implicit request =>
      Ok(views.html.index()(request.session))
  }

  def requestDeposit = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val uid = ControllerHelper.getParam(data, "uid", "1000000000").toLong
      val amount = ControllerHelper.getParam(data, "amount", "0.0").toDouble
      val currency: Currency = ControllerHelper.getParam(data, "currency", "")

      AccountService.deposit(uid, currency, amount) map {
        case result => Ok(result.toJson)
      }
  }

  def getTransfers() = Action.async {
    implicit request =>
      val query = request.queryString
      val pager = ControllerHelper.parsePagingParam()
      val status = getParam(query, "status").map(s => TransferStatus.valueOf(s).getOrElse(TransferStatus.Accepted))
      val types = getParam(query, "tType").map(s => TransferType.valueOf(s).getOrElse(TransferType.Withdrawal)) match {
        case Some(t) => Seq(t)
        case None => Nil
      }
      val currency = getParam(query, "currency").map(s => Currency.valueOf(s).getOrElse(Currency.Btc))
      val uid = getParam(query, "uid").map(_.toLong)

      TransferService.getTransfers(uid, currency, status, None, types, Cursor(pager.skip, pager.limit), true) map {
        case result =>
          Ok(result.toJson)
      }
  }

  def confirmTransfer(id: String) = Action {
    implicit request =>
      TransferService.AdminConfirmTransfer(id.toLong, true)
      Ok(ApiResult.toJson())
  }

  def rejectTransfer(id: String) = Action {
    implicit request =>
      TransferService.AdminConfirmTransfer(id.toLong, false)
      Ok(ApiResult.toJson())
  }

  def getActiveActors() = Action.async {
    implicit request =>
      MonitorService.getActorsPath() map {
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
        updated = System.currentTimeMillis(),
        lang = Language.valueOf(ControllerHelper.getParam(data, "language", "Chinese")).getOrElse(Language.Chinese))

      NotificationService.updateNotification(n)
      Ok(ApiResult.toJson)
  }

  def wallets(currency: String, walletsType: String) = Action.async {
    implicit request =>
      BitwayService.getWallets(currency, CryptoCurrencyAddressType.valueOf(walletsType).get).map(result =>
        Ok(result.toJson))
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
