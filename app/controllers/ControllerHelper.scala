package controllers

import scala.concurrent.Future
import com.coinport.coinex.api.model._
import play.api.mvc.Request

case class Pager(skip: Int = 0, limit: Int = 10, page: Int)

object ControllerHelper {
  type Validator = (_) => (Boolean, Future[ApiResult])
  val parmaErrorResult = ApiResult(false, -1, "参数错误", None)

  def getParam(queryString: Map[String, Seq[String]], param: String): Option[String] = {
    queryString.get(param).map(_(0))
  }

  def getParam(queryString: Map[String, Seq[String]], param: String, default: String): String = {
    queryString.get(param) match {
      case Some(seq) =>
        if (seq.isEmpty) default else seq(0)
      case None =>
        default
    }
  }

  // def validateParamsAndThen(validators: Seq[Validator])(f: => Future[ApiResult]) {
  //   if (validators.isEmpty) {
  //     f
  //   } else {
  //     val validator = validators.head
  //     val (isValid, apiResult) = validator
  //     if (isValid) {
  //       validateParamsAndThen(validators.tail, f)
  //     } else {
  //       Future { apiResult }
  //     }
  //   }
  // }

  def stringParamsNotEmpty(params: Seq[String])(f: => ApiResult): (Boolean, ApiResult) = {
    if (params.isEmpty) (true, null)
    else {
      val param = params.head
      if (param == null || param.trim.length == 0) {
        val apiResult = f
        (false, apiResult)
      }
      else
        stringParamsNotEmpty(params.tail)(f)
    }
  }

  def optionStringParamsNotEmpty(params: Seq[Option[String]])(f: => ApiResult): (Boolean, ApiResult) = {
    if (params.isEmpty) (true, null)
    else {
      val param = params.head.getOrElse("")
      if (param == null || param.trim.length == 0) {
        val apiResult = f
        (false, apiResult)
      }
      else
        optionStringParamsNotEmpty(params.tail)(f)
    }
  }

  def parsePagingParam()(implicit request: Request[_]): Pager = {
    val query = request.queryString
    val limit = getParam(query, "limit", "10").toInt
    val page = getParam(query, "page", "1").toInt
    val skip = (page - 1) * limit
    Pager(skip = skip, limit = limit, page = page)
  }

}
