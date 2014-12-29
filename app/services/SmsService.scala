package services

import scala.concurrent._
import com.typesafe.config.ConfigFactory
import com.coinport.coinex.api.model._
import sms._

trait SmsService {
  val smsConfigFile = "sms.conf"
  val smsConfig = ConfigFactory.load(smsConfigFile)
  val failedResult = ApiResult(false, -1, "send sms failed.", None)
  val successResult = ApiResult(true, 0, "send sms success.", None)

  def sendSmsSingle(phoneNum: String, text: String): Future[ApiResult]
  def sendSmsGroup(phoneNums: Set[String], text: String): Future[ApiResult]
  def sendVerifySms(phoneNum: String, randCode: String): Future[ApiResult]
}

object SmsService {
  val CLOOPEN_SERVICE_IMPL = "cloopen"
  val CLOOPEN_REST_SERVICE_IMPL = "cloopen_rest"
  val TWILIO_SERVICE_IMPL = "twilio"

  def getDefaultServiceImpl = getNamedServiceImpl(TWILIO_SERVICE_IMPL)
  def getNamedServiceImpl(serviceName: String): SmsService = {
    if (TWILIO_SERVICE_IMPL.equals(serviceName))
      TwilioSmsService
    else if (CLOOPEN_SERVICE_IMPL.equals(serviceName))
      CloopenSmsService
    else if (CLOOPEN_REST_SERVICE_IMPL.equals(serviceName))
      CloopenRestSmsService
    else
      throw new IllegalArgumentException(s"sms service: $serviceName not found.")
  }
}
