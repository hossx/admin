/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package controllers

import play.api.mvc._
import play.api.Logger
import play.api.libs.functional.syntax._
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.UUID
import java.util.Random

import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.api.model._
import com.coinport.coinex.data.ErrorCode
import com.coinport.coinex.api.service.UserService
import services._
import models._
import ControllerHelper._

object VerifyController extends Controller with Json4s {
  val logger = Logger(this.getClass)

  val smsService = SmsService.getDefaultServiceImpl
  val smsServiceInChina = SmsService.getNamedServiceImpl(SmsService.CLOOPEN_REST_SERVICE_IMPL)
  val cacheService = CacheService.getDefaultServiceImpl

  val allowedMinIntervalSeconds :Int = 20
  val rand = new Random()
  val randMax = 999999
  val randMin = 100000
  val emails = List("c@coinport.com", "yangli@coinport.com", "xiaolu@coinport.com", "coinport@126.com")

  private def generateVerifyCode: (String, String) = {
    val uuid = UUID.randomUUID().toString
    val verifyNum = rand.nextInt(randMax - randMin) + randMin
    val verifyCode = verifyNum.toString
    cacheService.putWithTimeout(uuid, verifyCode, 30 * 60)
    (uuid, verifyCode)
  }

  def sendVerifySms = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val phoneNum = getParam(data, "phoneNumber").getOrElse("")
    logger.info(s"phoneNum: $phoneNum")
    val (uuid, verifyCode) = generateVerifyCode
    validateParamsAndThen(
      new PhoneNumberValidator(phoneNum)
    ) {
      sendSms(phoneNum, verifyCode, uuid)
    } map {
      result =>
      Ok(result.toJson)
    }
  }

  private def checkSmsFrequency(phoneNum: String): Boolean = {
    val lastTsStr = cacheService.get(phoneNum)
    val currTs = System.currentTimeMillis
    if (lastTsStr == null) {
      cacheService.putWithTimeout(phoneNum, currTs.toString, 120)
      true
    } else {
      val lastTs = lastTsStr.toLong
      logger.debug(s"lastTsStr: $lastTsStr, currTs: $currTs")
      if (currTs - lastTs < allowedMinIntervalSeconds * 1000)
        false
      else {
        cacheService.putWithTimeout(phoneNum, currTs.toString, 120)
        true
      }
    }
  }

  // TODO check phoneNum format.
  private def sendSms(phoneNum: String, verifyCode: String, uuid: String): Future[ApiResult] =
    if (! checkSmsFrequency(phoneNum)) {
      val err = ErrorCode.SendSmsFrequencyTooHigh
      Future(ApiResult(false, err.value, err.toString))
    } else {
      //logger.info(s"send sms verify code: phoneNum=$phoneNum, verifyCode=$verifyCode")
      val sendRes = if (phoneNum.startsWith("+86") || phoneNum.startsWith("0086")) {
        val shortPhoneNum = if (phoneNum.startsWith("+86")) phoneNum.substring(3).trim
        else phoneNum.substring(4).trim
        smsServiceInChina.sendVerifySms(shortPhoneNum, verifyCode)
      } else if(!phoneNum.startsWith("+") && !phoneNum.startsWith("00") &&
        phoneNum.trim.length == 11) {
        smsServiceInChina.sendVerifySms(phoneNum, verifyCode)
      } else {
        smsService.sendVerifySms(phoneNum, verifyCode)
      }

      sendRes map {
        result =>
        if (result.success)
          ApiResult(true, 0, "", Some(uuid))
        else
          result
      }
    }

  def sendVerificationEmail = Action.async { implicit request =>
      val data = request.queryString
      val userEmail = getParam(data, "email").getOrElse("")
      if (emails.contains(userEmail)) {
        val (uuid, verifyCode) = generateVerifyCode
        UserService.sendVerificationCodeEmail(userEmail, verifyCode.toString) map {
          rv => Ok(ApiResult(true, 0, "", Some(uuid)).toJson)
        }
      } else {
        Future(Ok(ApiResult(false, 0, "bad email address", None).toJson))
      }
  }

  def sendVerifySms2 = Authenticated.async {
    implicit request =>
    val userId = request.session.get("uid").getOrElse("")
    val (uuid, verifyCode) = generateVerifyCode
    val uid = userId.toLong
    UserService.getProfile(uid) flatMap {
      result =>
      if (result.success) {
        val user = result.data.get.asInstanceOf[com.coinport.coinex.api.model.User]
        val phoneNum = user.mobile.getOrElse("")
        if (phoneNum == null || phoneNum.trim.length == 0) {
          val err = ErrorCode.MobileNotVerified
          Future(Ok(ApiResult(false, err.value, err.toString).toJson))
        } else {
          sendSms(phoneNum, verifyCode, uuid) map {
            result =>
            Ok(result.toJson)
          }
        }
      } else
        Future(Ok(result.toJson))
    }
  }

}
