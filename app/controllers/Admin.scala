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
import com.typesafe.config.ConfigFactory
import com.mongodb.casbah.{ MongoConnection, MongoURI, WriteConcern }
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports._
import org.bson.types.ObjectId

object Admin extends Controller with Json4s {
  private val configPath = if (System.getProperty("gooc.config") != null) System.getProperty("gooc.config") else "gooc.conf"
  private val config = ConfigFactory.load(configPath)

  private val host = config.getString("akka.mongo.host")
  private val port = config.getInt("akka.mongo.port")
  private val mongoUri = MongoURI(s"mongodb://${host}:${port}/gooc")
  private val ethMongoUri = MongoURI(s"mongodb://${host}:${port}/eth")
  private val txCollection = MongoConnection(mongoUri)(mongoUri.database.get)("dTxs")
  private val ethTxCollection = MongoConnection(ethMongoUri)(ethMongoUri.database.get)("txs")
  private val edmUri = MongoURI(s"mongodb://${host}:${port}/edm")
  private val edmCollection = MongoConnection(edmUri)(edmUri.database.get)("edms")

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

  private case class GoocTx(_id: Long, a: Double, c: String, cps: String, rp: String, ty: String, tt: String, sp: String, ra: String, sa: String, t: Long, cptxid: Long, cpuid: Long)
  private case class EthTx(_id: String, a: Double, c: String, cps: String, ty: String, ra: String, sa: String, t: Long, cptxid: Long, cpuid: Long)
  private case class EdmItem(_id: String, t: String, tn: String, e: String, s: String, ts: Long)

  def getGoocTxs() = Action { implicit request =>
      val query = request.queryString
      val pager = ControllerHelper.parsePagingParam()
      val status = getParam(query, "status")
      val gid = getParam(query, "gid")
      val uid = getParam(query, "uid")
      val t = getParam(query, "type")
      var q = MongoDBObject()
      if (status.isDefined)
        q += ("cps" -> status.get)
      if (gid.isDefined)
        q += ("_id" -> new java.lang.Long(gid.get.toLong))
      if (uid.isDefined)
        q += ("c" -> uid.get)
      if (t.isDefined)
        q += ("ty" -> t.get)
      val count = txCollection.count(q)
      val txs = txCollection.find(q).sort(MongoDBObject("_id" -> -1)).skip(pager.skip).limit(pager.limit).map(toGoocTx(_)).toSeq
      Ok(ApiResult(data = Some(ApiPagingWrapper(pager.skip, pager.limit, txs, count.toInt))).toJson)
  }

  def getEthTxs() = Action { implicit request =>
      val query = request.queryString
      val pager = ControllerHelper.parsePagingParam()
      val status = getParam(query, "status")
      val gid = getParam(query, "gid")
      val uid = getParam(query, "uid")
      val t = getParam(query, "type")
      var q = MongoDBObject()
      if (status.isDefined)
        q += ("cps" -> status.get)
      if (gid.isDefined)
        q += ("_id" -> gid.get)
      if (uid.isDefined)
        q += ("c" -> uid.get)
      if (t.isDefined)
        q += ("ty" -> t.get)
      val count = ethTxCollection.count(q)
      val txs = ethTxCollection.find(q).sort(MongoDBObject("t" -> -1)).skip(pager.skip).limit(pager.limit).map(toEthTx(_)).toSeq
      Ok(ApiResult(data = Some(ApiPagingWrapper(pager.skip, pager.limit, txs, count.toInt))).toJson)
  }

  private def toEthTx(obj: DBObject) = {
    EthTx(
      obj.get("_id").asInstanceOf[String],
      obj.get("a").asInstanceOf[Double],
      obj.get("c").asInstanceOf[String],
      obj.get("cps").asInstanceOf[String],
      obj.get("ty").asInstanceOf[String],
      obj.get("outputAddr").asInstanceOf[String],
      obj.get("inputAddr").asInstanceOf[String],
      obj.get("t").asInstanceOf[Long],
      obj.get("cptxid").asInstanceOf[Long],
      obj.get("cpuid").asInstanceOf[Long])
  }

  private def toGoocTx(obj: DBObject) = {
    GoocTx(
      obj.get("_id").asInstanceOf[Long],
      obj.get("a").asInstanceOf[Double],
      obj.get("c").asInstanceOf[String],
      obj.get("cps").asInstanceOf[String],
      obj.get("rp").asInstanceOf[String],
      obj.get("ty").asInstanceOf[String],
      obj.get("tt").asInstanceOf[String],
      obj.get("sp").asInstanceOf[String],
      obj.get("ra").asInstanceOf[String],
      obj.get("sa").asInstanceOf[String],
      obj.get("t").asInstanceOf[Long],
      obj.get("cptxid").asInstanceOf[Long],
      obj.get("cpuid").asInstanceOf[Long])
  }

  private def toEdmItem(obj: DBObject) = {
    EdmItem(
      obj.get("_id").asInstanceOf[ObjectId].toString(),
      obj.get("t").asInstanceOf[String],
      obj.get("tn").asInstanceOf[String],
      obj.get("e").asInstanceOf[String],
      obj.get("s").asInstanceOf[String],
      obj.get("ts").asInstanceOf[Long])
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

  def confirmGoocTx() = Authenticated.async(parse.urlFormEncoded) { implicit request =>
      val data = request.body
      val goocId = new java.lang.Long(ControllerHelper.getParam(data, "_id", "0").toLong)
      if (goocId == 0) {
        Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, "gooc id can't be null").toJson))
      } else {
        val tx = txCollection.findOne(MongoDBObject("_id" -> goocId))
        if (!tx.isDefined) {
          Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, "can't find gooc tx: " + goocId).toJson))
        } else if (tx.get.get("cps").asInstanceOf[String] != "BAD_FORM" && tx.get.get("cps").asInstanceOf[String] != "UNDER_LIMIT") {
          Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, s"gooc tx ${goocId} can't be confirmed").toJson))
        } else {
          val uid = ControllerHelper.getParam(data, "inputUid", "1000000000").toLong
          val amount = ControllerHelper.getParam(data, "a", "0.0").toDouble
          val currency: Currency = Currency.Gooc

          txCollection.update(MongoDBObject("_id" -> goocId), $set("cps" -> "PROCESSING"), false, false, WriteConcern.Safe)
          AccountService.deposit(uid, currency, amount) map { case result =>
            if (result.success) {
              val cptxid = result.data.get.asInstanceOf[RequestTransferSucceeded].transfer.id
              txCollection.update(MongoDBObject("_id" -> goocId),
                $set("cps" -> "PROCESSED", "cptxid" -> cptxid, "cpuid" -> uid), false, false, WriteConcern.Safe)
            } else {
              txCollection.update(MongoDBObject("_id" -> goocId), $set("cps" -> "FAILED"), false, false, WriteConcern.Safe)
            }
            Ok(result.toJson)
          }
        }
      }
  }

  def confirmEthTx() = Authenticated.async(parse.urlFormEncoded) { implicit request =>
      val data = request.body
      val ethId = ControllerHelper.getParam(data, "_id", "0")
      if (ethId == "0") {
        Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, "eth id can't be null").toJson))
      } else {
        val tx = ethTxCollection.findOne(MongoDBObject("_id" -> ethId))
        if (!tx.isDefined) {
          Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, "can't find eth tx: " + ethId).toJson))
        } else if (tx.get.get("cps").asInstanceOf[String] != "BAD_FORM" && tx.get.get("cps").asInstanceOf[String] != "UNDER_LIMIT") {
          Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, s"eth tx ${ethId} can't be confirmed").toJson))
        } else {
          val uid = ControllerHelper.getParam(data, "inputUid", "1000000000").toLong
          val amount = ControllerHelper.getParam(data, "a", "0.0").toDouble
          val currency: Currency = Currency.Eth

          ethTxCollection.update(MongoDBObject("_id" -> ethId), $set("cps" -> "PROCESSING"), false, false, WriteConcern.Safe)
          AccountService.deposit(uid, currency, amount) map { case result =>
            if (result.success) {
              val cptxid = result.data.get.asInstanceOf[RequestTransferSucceeded].transfer.id
              ethTxCollection.update(MongoDBObject("_id" -> ethId),
                $set("cps" -> "PROCESSED", "cptxid" -> cptxid, "cpuid" -> uid), false, false, WriteConcern.Safe)
            } else {
              ethTxCollection.update(MongoDBObject("_id" -> ethId), $set("cps" -> "FAILED"), false, false, WriteConcern.Safe)
            }
            Ok(result.toJson)
          }
        }
      }
  }

  def rejectEthTx(gid: String) = Authenticated.async(parse.urlFormEncoded) { implicit request =>
    val ethId = gid
    val tx = ethTxCollection.findOne(MongoDBObject("_id" -> ethId))
    if (!tx.isDefined) {
      Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, "can't find eth tx: " + ethId).toJson))
    } else if (tx.get.get("cps").asInstanceOf[String] != "BAD_FORM" && tx.get.get("cps").asInstanceOf[String] != "UNDER_LIMIT") {
      Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, s"eth tx ${ethId} can't be rejected").toJson))
    } else {
      ethTxCollection.update(MongoDBObject("_id" -> ethId), $set("cps" -> "FAILED"), false, false, WriteConcern.Safe)
      Future(Ok(ApiResult(true, ErrorCode.Ok.value, "eth tx ${ethId} is rejected").toJson))
    }
  }

  def rejectGoocTx(gid: String) = Authenticated.async(parse.urlFormEncoded) { implicit request =>
    val goocId = new java.lang.Long(gid.toLong)
    val tx = txCollection.findOne(MongoDBObject("_id" -> goocId))
    if (!tx.isDefined) {
      Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, "can't find gooc tx: " + goocId).toJson))
    } else if (tx.get.get("cps").asInstanceOf[String] != "BAD_FORM" && tx.get.get("cps").asInstanceOf[String] != "UNDER_LIMIT") {
      Future(Ok(ApiResult(false, ErrorCode.ParamEmpty.value, s"gooc tx ${goocId} can't be rejected").toJson))
    } else {
      txCollection.update(MongoDBObject("_id" -> goocId), $set("cps" -> "FAILED"), false, false, WriteConcern.Safe)
      Future(Ok(ApiResult(true, ErrorCode.Ok.value, "gooc tx ${goocId} is rejected").toJson))
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

  def sendEdm() = Authenticated.async(parse.urlFormEncoded) { implicit request =>
      val data = request.body
      val title = getParam(data, "title")
      val tplName = getParam(data, "tplName")
      val emails = getParam(data, "emails")
      val isResend = getParam(data, "isResend").getOrElse("false") == "true"
      if ((!isResend && (!title.isDefined || !tplName.isDefined || !emails.isDefined)) ||
        (isResend && !emails.isDefined)) {
        Future(BadRequest)
      } else {
        if (isResend) {
          for (id <- emails.get.split(";")) {
            edmCollection.update(MongoDBObject("_id" -> new ObjectId(id)), $set("s" -> "PENDING"), false, false, WriteConcern.Safe)
          }
        } else {
          for (email <- emails.get.split(";")) {
            edmCollection.insert(MongoDBObject("t" -> title.get, "tn" -> tplName.get, "e" -> email, "s" -> "PENDING", "ts" -> System.currentTimeMillis))
          }
        }
        Future(Ok("ok"))
      }
  }

  def getEdm() = Authenticated.async(parse.urlFormEncoded) { implicit request =>
      val query = request.body
      val pager = ControllerHelper.parsePagingParam()
      val id = getParam(query, "id").getOrElse("")
      val title = getParam(query, "title").getOrElse("")
      val tplName = getParam(query, "tplName").getOrElse("")
      val email = getParam(query, "email").getOrElse("")
      val status = getParam(query, "status").getOrElse("")
      var q = MongoDBObject()
      if (id != "")
        q += ("_id" -> new ObjectId(id))
      if (title != "")
        q += ("t" -> title)
      if (tplName != "")
        q += ("tn" -> tplName)
      if (email != "")
        q += ("e" -> email)
      if (status != "")
        q += ("s" -> status)
      val count = edmCollection.count(q)
      val items = edmCollection.find(q).sort(MongoDBObject("_id" -> -1)).skip(pager.skip).limit(pager.limit).map(toEdmItem(_)).toSeq
      Future(Ok(ApiResult(data = Some(ApiPagingWrapper(pager.skip, pager.limit, items, count.toInt))).toJson))
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
