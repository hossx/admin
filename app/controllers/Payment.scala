package controllers


import play.api._
import play.api.mvc._
import play.api.libs.json._
import akka.pattern.ask
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.bitway.data._
import ControllerHelper._
import java.util.Date
import com.coinport.coinex.api.model.ApiResult

/**
 * Created by chenxi on 8/29/14.
 */

object Payment  extends Controller with PaymentAccess {
  def getPaymentWithdrawal = Action.async {
    implicit request =>
      val cur = request.queryString.get("cur").map(_(0))
    val limit = request.queryString.get("limit").map(_(0)).getOrElse("10").toInt
    val skip = request.queryString.get("skip").map(_(0)).getOrElse("0").toInt
    val currency = cur.map(c => Currency.valueOf(c).getOrElse(Currency.Cny))

    routers.accountActor ? QueryTransfer(
      merchantId = Some(request.session.get("id").map(_.toLong).get),
      currency = currency,
      types = Seq(TransferType.Withdrawal),
      cur = Cursor(limit = limit, skip = skip)) map {
      case rv: QueryTransferResult =>
        val jsArr = new JsArray(rv.transfers.map { t =>
          val date = new Date(t.created.get)

          Json.obj("date" -> date.toString, "status" -> t.status.getValue, "amount" -> t.amount, "address" -> t.address.get)
        })
        Ok(Json.obj("count" -> rv.count, "items" -> jsArr))
    }
  }

  def paymentWithdrawalSucceed(id: String) = Action {
    implicit request =>
      val transfer = getTransfer(id.toLong, request.body.asFormUrlEncoded.get)
      routers.accountActor ? AdminConfirmTransferSuccess(transfer, None)
      Ok(Json.obj())
  }


  def paymentWithdrawalFailed(id: String) = Action {
    implicit request =>
      val transfer = getTransfer(id.toLong, request.body.asFormUrlEncoded.get)
      routers.accountActor ? AdminConfirmTransferFailure(transfer, ErrorCode.InvalidAmount)
      Ok(Json.obj())
  }

  def paymentWithdrawalProcessed(id: String) = Action {
    implicit request =>
      val transfer = getTransfer(id.toLong, request.body.asFormUrlEncoded.get)
      routers.accountActor ? AdminConfirmTransferProcessed(transfer)
      Ok(Json.obj())
  }

  private def getTransfer(id: Long, data: Map[String, Seq[String]]): AccountTransfer = {
    val cur = getParam(data, "currency").get
    val currency = Some(Currency.valueOf(cur).getOrElse(Currency.Btc)).get
    val merchantId = getParam(data, "merchantId").get.toLong

    AccountTransfer(
      id = id,
      merchantId = merchantId,
      `type` = TransferType.Withdrawal,
      currency = currency,
      amount = 0L
    )
  }

  def getParam(queryString: Map[String, Seq[String]], param: String): Option[String] = {
    queryString.get(param).map(_(0))
  }

}
