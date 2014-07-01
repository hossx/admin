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
import ControllerHelper._
import services.{UserService => AdminUserService}

object Users extends Controller with Json4s {

  def search = Authenticated {
    implicit request =>
    val data = request.queryString
    val userName = getParam(data, "username").getOrElse("")
    val idFromStr = getParam(data, "idFrom").getOrElse("")
    val idToStr = getParam(data, "idTo").getOrElse("")

    val idFrom = strToLong(idFromStr).getOrElse(0L)
    val idTo = strToLong(idToStr).getOrElse(Long.MaxValue)

    val result = AdminUserService.searchUser(userName, idFrom, idTo)
    Ok(result.toJson)
  }

  def suspend(uid: Long) = Authenticated.async {
    implicit request =>
    AdminUserService.suspendUser(uid) map {
      result =>
      Ok(result.toJson)
    }
  }

  def resume(uid: Long) = Authenticated.async {
    implicit request =>
    AdminUserService.resumeUser(uid) map {
      result =>
      Ok(result.toJson)
    }
  }

}